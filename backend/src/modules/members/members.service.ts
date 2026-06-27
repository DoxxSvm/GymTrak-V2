import {
  BadRequestException,
  ConflictException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { AuditAction, AuditEntityType, GymRole, Prisma } from '@prisma/client';
import {
  classifyAttendancePunctuality,
  formatCheckInRelativeLine,
  monthShortLabel,
  punctualityDisplayLabel,
} from '../../common/utils/attendance-punctuality';
import { normalizeEmailForStorage } from '../../common/utils/normalize-email';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { WhatsAppAutomationService } from '../messaging/whatsapp-automation.service';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import type { AddMemberSubscriptionDto } from './dto/add-subscription.dto';
import type { AddDietEntryDto } from './dto/add-diet-entry.dto';
import type { AddWorkoutDto } from './dto/add-workout.dto';
import type { CreateMemberDto } from './dto/create-member.dto';
import type { ReceivePaymentDto } from './dto/receive-payment.dto';
import type { UpdateMemberDto } from './dto/update-member.dto';
import { computeMemberLifecycleStatus } from './member-lifecycle';
import { MemberListFilter, memberListFilterWhere } from './member-list-filter';
import { AuditService } from '../audit/audit.service';
import { NOTIFICATION_EVENTS } from '../notifications/domain-events';

type MemberSubscriptionWithPlanNames = Prisma.MemberSubscriptionGetPayload<{
  include: {
    plan: { select: { name: true } };
    gymPlan: { select: { name: true } };
  };
}>;

@Injectable()
export class MembersService {
  private readonly logger = new Logger(MembersService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly subscriptions: SubscriptionsService,
    private readonly whatsapp: WhatsAppAutomationService,
    private readonly events: EventEmitter2,
    private readonly audit: AuditService,
  ) {}

  /** Shared include for member detail + profile (latest subscription row). */
  private memberDetailInclude(): Prisma.GymUserInclude {
    return {
      user: {
        select: {
          id: true,
          phone: true,
          email: true,
          fullName: true,
          heightCm: true,
          weightKg: true,
          createdAt: true,
          avatarUrl: true,
        },
      },
      memberSubscriptions: {
        orderBy: { startsAt: 'desc' },
        take: 1,
        include: {
          plan: { select: { name: true } },
          gymPlan: { select: { name: true } },
        },
      },
    };
  }

  private buildSubscriptionSummary(
    memberSubscriptions: MemberSubscriptionWithPlanNames[],
    now: Date,
  ): {
    stats: {
      active_subscription: number;
      pending_payment: number;
      overdue: number;
    };
    current_subscription: {
      plan_name: string;
      start_date: Date;
      expiry_date: Date;
      remaining_days: number;
      amount_paid: number;
      amount_pending: number;
    } | null;
  } {
    const current = memberSubscriptions[0];
    const pending =
      current == null ? 0 : Math.max(0, current.priceCents - current.paidCents);
    const overdue =
      current != null && current.endsAt < now && pending > 0 ? 1 : 0;
    const remainingDays =
      current == null
        ? 0
        : Math.max(
            0,
            Math.ceil((current.endsAt.getTime() - now.getTime()) / 86400000),
          );

    return {
      stats: {
        active_subscription: current ? 1 : 0,
        pending_payment: pending > 0 ? 1 : 0,
        overdue,
      },
      current_subscription: current
        ? {
            plan_name: current.gymPlan?.name ?? current.plan?.name ?? '',
            start_date: current.startsAt,
            expiry_date: current.endsAt,
            remaining_days: remainingDays,
            amount_paid: Math.round(current.paidCents / 100),
            amount_pending: Math.round(pending / 100),
          }
        : null,
    };
  }

  async list(
    actorUserId: string,
    gymId: string | undefined,
    q: string | undefined,
    filter: MemberListFilter | undefined,
    limit: number,
    offset: number,
  ) {
    const resolvedGymId = await this.resolveGymIdForActor(actorUserId, gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, resolvedGymId);
    const now = new Date();
    const where: Prisma.GymUserWhereInput = {
      gymId: resolvedGymId,
      role: GymRole.MEMBER,
      ...memberListFilterWhere(filter, now),
      ...(q?.trim()
        ? {
            user: {
              OR: [
                {
                  fullName: {
                    contains: q.trim(),
                    mode: 'insensitive',
                  },
                },
                { phone: { contains: q.trim() } },
                {
                  email: {
                    contains: q.trim(),
                    mode: 'insensitive',
                  },
                },
              ],
            },
          }
        : {}),
    };

    const [total, rows] = await Promise.all([
      this.prisma.gymUser.count({ where }),
      this.prisma.gymUser.findMany({
        where,
        orderBy: [{ user: { fullName: 'asc' } }, { id: 'asc' }],
        take: limit,
        skip: offset,
        select: {
          id: true,
          isLead: true,
          isActive: true,
          membershipEndsAt: true,
          joinedAt: true,
          user: {
            select: {
              id: true,
              fullName: true,
              phone: true,
              email: true,
              avatarUrl: true,
            },
          },
        },
      }),
    ]);

    const [active, inactive, expired,totalMembers] = await Promise.all([
      this.prisma.gymUser.count({
        where: {
          gymId: resolvedGymId,
          role: GymRole.MEMBER,
          isActive: true,
          OR: [{ membershipEndsAt: null }, { membershipEndsAt: { gte: now } }],
        },
      }),
      this.prisma.gymUser.count({
        where: { gymId: resolvedGymId, role: GymRole.MEMBER, isActive: false },
      }),
      this.prisma.gymUser.count({
        where: {
          gymId: resolvedGymId,
          role: GymRole.MEMBER,
          isActive: true,
          membershipEndsAt: { not: null, lt: now },
        },
      }),
      this.prisma.gymUser.count({
        where: { gymId: resolvedGymId, role: GymRole.MEMBER },
      }),
    ]);

    const page = Math.floor(offset / Math.max(limit, 1)) + 1;
    const totalPages = Math.max(1, Math.ceil(total / Math.max(limit, 1)));
    const mapped = rows.map((r) => ({
      id: r.id,
      name: r.user.fullName ?? '',
      phone: r.user.phone,
      status: computeMemberLifecycleStatus(r, now),
      plan_name: '',
      expiry_date: r.membershipEndsAt,
      profile_image: r.user.avatarUrl ?? '',
    }));

    return {
      members: mapped,
      pagination: {
        page,
        total_pages: totalPages,
        total_records: total,
      },
      stats: {
        active,
        inactive,
        expired,
        total_members: totalMembers,
      },
      total,
      limit,
      offset,
      items: rows.map((r) => ({
        gymUserId: r.id,
        userId: r.user.id,
        name: r.user.fullName,
        phone: r.user.phone,
        email: r.user.email,
        avatarUrl: r.user.avatarUrl,
        lifecycleStatus: computeMemberLifecycleStatus(r, now),
        membershipEndsAt: r.membershipEndsAt,
        isLead: r.isLead,
        isActive: r.isActive,
        joinedAt: r.joinedAt,
      })),
    };
  }

  async create(actorUserId: string, dto: CreateMemberDto) {
    const gymId = await this.resolveGymIdForActor(actorUserId, dto.gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const fullName = this.resolveFullName(dto);
    const phone = dto.phone.trim();
    const now = new Date();

    const gymRow = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { ownerId: true },
    });
    if (!gymRow) {
      throw new NotFoundException('Gym not found');
    }

    const existingUserByPhone = await this.prisma.user.findUnique({
      where: { phone },
      select: { id: true },
    });
    if (existingUserByPhone) {
      if (gymRow.ownerId === existingUserByPhone.id) {
        throw new ConflictException(
          'This phone is already the gym owner account. Use a different phone number for the member.',
        );
      }
      const existingLink = await this.prisma.gymUser.findFirst({
        where: { gymId, userId: existingUserByPhone.id },
        select: { id: true, role: true },
      });
      if (existingLink) {
        if (existingLink.role === GymRole.MEMBER) {
          throw new ConflictException(
            'A member with this phone already exists at this gym',
          );
        }
        const roleLabel =
          existingLink.role === GymRole.TRAINER
            ? 'a trainer'
            : existingLink.role === GymRole.STAFF
              ? 'staff'
              : existingLink.role === GymRole.OWNER
                ? 'an owner'
                : 'this gym';
        throw new ConflictException(
          `This phone is already linked to this gym as ${roleLabel}. Use a different phone or update that profile instead of adding a new member.`,
        );
      }
    }

    const emailNorm = normalizeEmailForStorage(dto.email);
    if (emailNorm) {
      const emailTaken = await this.prisma.user.findFirst({
        where: {
          email: emailNorm,
          NOT: { phone },
        },
        select: { id: true },
      });
      if (emailTaken) {
        throw new ConflictException(
          'This email is already registered to another account',
        );
      }
    }

    const membershipEndsAt = this.parseNullableDateField(
      'membershipEndsAt',
      dto.membershipEndsAt,
    );
    const dateOfBirth = this.parseNullableDateField(
      'dateOfBirth',
      dto.dateOfBirth ?? dto.dob,
    );
    const joinedAt = this.parseOptionalDateField(
      'date_of_joining',
      dto.date_of_joining,
    );

    let createdGymUserId = '';
    let newSubId: string | undefined;
    try {
      await this.prisma.$transaction(async (tx) => {
        const user = await tx.user.upsert({
          where: { phone },
          create: {
            phone,
            fullName,
            ...(emailNorm ? { email: emailNorm } : {}),
            ...(dto.avatarUrl?.trim()
              ? { avatarUrl: dto.avatarUrl.trim() }
              : {}),
          },
          update: {
            fullName,
            ...(emailNorm ? { email: emailNorm } : {}),
            ...(dto.avatarUrl !== undefined
              ? { avatarUrl: dto.avatarUrl?.trim() || null }
              : {}),
          },
        });

        const userUpdate: Prisma.UserUpdateInput = {};
        if (dto.heightCm != null) {
          userUpdate.heightCm = new Prisma.Decimal(dto.heightCm);
        }
        if (dto.weightKg != null) {
          userUpdate.weightKg = new Prisma.Decimal(dto.weightKg);
        }
        if (Object.keys(userUpdate).length > 0) {
          await tx.user.update({
            where: { id: user.id },
            data: userUpdate,
          });
        }

        const gu = await tx.gymUser.create({
          data: {
            userId: user.id,
            gymId,
            role: GymRole.MEMBER,
            isLead: dto.isLead ?? false,
            isActive: true,
            membershipEndsAt,
            notes: this.composeNotes(dto),
            emergencyContactName: dto.emergencyContactName?.trim() ?? null,
            emergencyContactPhone: dto.emergencyContactPhone?.trim() ?? null,
            dateOfBirth,
            gender: dto.gender?.trim() ?? null,
            ...(joinedAt !== undefined ? { joinedAt } : {}),
          },
        });
        createdGymUserId = gu.id;

        if (dto.initialSubscription) {
          newSubId = await this.subscriptions.createInitialSubscriptionTx(
            tx,
            gu.id,
            dto.initialSubscription,
            now,
          );
        }

        await this.subscriptions.syncMembershipEndsAt(tx, gu.id);
      });
    } catch (e: unknown) {
      if (
        e instanceof Prisma.PrismaClientKnownRequestError &&
        e.code === 'P2002'
      ) {
        const target = e.meta?.target;
        const fields = Array.isArray(target)
          ? target.map(String)
          : target != null
            ? [String(target)]
            : [];
        const targetStr = fields.join(' ');
        const compositeUserGym =
          (fields.includes('userId') && fields.includes('gymId')) ||
          (targetStr.includes('userId') && targetStr.includes('gymId'));
        if (compositeUserGym) {
          throw new ConflictException(
            'This user is already linked to this gym (for example as trainer or staff). The same phone cannot be added again as a member.',
          );
        }
        if (fields.some((f) => f === 'email' || f.includes('email'))) {
          throw new ConflictException(
            'This email is already registered to another account',
          );
        }
        if (fields.some((f) => f === 'phone' || f.includes('phone'))) {
          throw new ConflictException(
            'A member with this phone already exists at this gym',
          );
        }
        if (
          fields.some((f) => f === 'username' || String(f).includes('username'))
        ) {
          throw new ConflictException(
            'This account has a login username that conflicts with another user.',
          );
        }
        throw new ConflictException(
          `A unique field conflicted with an existing record (${fields.join(', ') || 'unknown'}).`,
        );
      }
      throw e;
    }

    this.events.emit(NOTIFICATION_EVENTS.MEMBER_ADDED, {
      gymId,
      gymUserId: createdGymUserId,
      memberName: fullName,
      actorUserId: actorUserId,
    });
    if (newSubId) {
      this.events.emit(NOTIFICATION_EVENTS.PLAN_ASSIGNED, {
        gymId,
        gymUserId: createdGymUserId,
        memberSubscriptionId: newSubId,
        actorUserId: actorUserId,
      });
    }

    const created = await this.prisma.gymUser.findUnique({
      where: { id: createdGymUserId },
      select: { userId: true },
    });
    if (created) {
      await this.audit.log({
        gymId,
        actorUserId: actorUserId,
        action: AuditAction.MEMBER_ADDED,
        entityType: AuditEntityType.GYM_USER,
        entityId: createdGymUserId,
        metadata: { memberUserId: created.userId },
      });
      if (newSubId) {
        await this.audit.log({
          gymId,
          actorUserId: actorUserId,
          action: AuditAction.PLAN_ASSIGNED,
          entityType: AuditEntityType.MEMBER_SUBSCRIPTION,
          entityId: newSubId,
          metadata: { gymUserId: createdGymUserId },
        });
      }
      void this.whatsapp.enqueueWelcome(gymId, created.userId).catch((err) => {
        this.logger.warn(
          `Welcome WhatsApp enqueue failed: ${(err as Error).message}`,
        );
      });
    }

    return this.getDetail(actorUserId, gymId, createdGymUserId);
  }

  async getDetail(actorUserId: string, gymId: string, gymUserId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const row = await this.prisma.gymUser.findFirst({
      where: {
        id: gymUserId,
        gymId,
        role: GymRole.MEMBER,
      },
      include: this.memberDetailInclude(),
    });
    if (!row) {
      throw new NotFoundException('Member not found');
    }

    const digits = row.user.phone.replace(/\D/g, '');
    const whatsappUrl = digits.length > 0 ? `https://wa.me/${digits}` : null;
    const subscription = this.buildSubscriptionSummary(
      row.memberSubscriptions as MemberSubscriptionWithPlanNames[],
      now,
    );

    const [lifetimeCheckIns, lastAttendance] = await Promise.all([
      this.prisma.attendanceRecord.count({
        where: { gymId, memberUserId: row.userId },
      }),
      this.prisma.attendanceRecord.findFirst({
        where: { gymId, memberUserId: row.userId },
        orderBy: { checkedInAt: 'desc' },
        select: { attendedOn: true, checkedInAt: true },
      }),
    ]);

    return {
      gymUserId: row.id,
      gymId: row.gymId,
      lifecycleStatus: computeMemberLifecycleStatus(row, now),
      isLead: row.isLead,
      isActive: row.isActive,
      membershipEndsAt: row.membershipEndsAt,
      joinedAt: row.joinedAt,
      notes: row.notes,
      emergencyContactName: row.emergencyContactName,
      emergencyContactPhone: row.emergencyContactPhone,
      dateOfBirth: row.dateOfBirth,
      gender: row.gender,
      /** Same shape as entries in `GET /members` → `members[]` (list card). */
      summary: {
        id: row.id,
        name: row.user.fullName ?? '',
        phone: row.user.phone,
        status: computeMemberLifecycleStatus(row, now),
        plan_name: subscription.current_subscription?.plan_name ?? '',
        expiry_date: row.membershipEndsAt,
        profile_image: row.user.avatarUrl ?? '',
      },
      subscription: {
        stats: subscription.stats,
        current_subscription: subscription.current_subscription,
      },
      user: {
        id: row.user.id,
        fullName: row.user.fullName,
        phone: row.user.phone,
        email: row.user.email,
        heightCm: row.user.heightCm,
        weightKg: row.user.weightKg,
        createdAt: row.user.createdAt,
        avatarUrl: row.user.avatarUrl,
      },
      contact: {
        phone: row.user.phone,
        telUri: digits.length > 0 ? `tel:${digits}` : null,
        whatsappUrl,
      },
      tabs: {
        subscriptions: `members/${row.id}/subscriptions?gymId=${encodeURIComponent(gymId)}`,
        attendance: `members/${row.id}/attendance/summary?gymId=${encodeURIComponent(gymId)}`,
        attendance_history: `members/${row.id}/attendance/history?gymId=${encodeURIComponent(gymId)}`,
        payments: `members/${row.id}/payments?gymId=${encodeURIComponent(gymId)}`,
      },
      attendance: {
        lifetime_check_ins: lifetimeCheckIns,
        last_check_in_at: lastAttendance?.checkedInAt.toISOString() ?? null,
        last_attended_on: lastAttendance
          ? lastAttendance.attendedOn.toISOString().slice(0, 10)
          : null,
        links: {
          summary: `members/${row.id}/attendance/summary?gymId=${encodeURIComponent(gymId)}`,
          history: `members/${row.id}/attendance/history?gymId=${encodeURIComponent(gymId)}`,
        },
      },
    };
  }

  /**
   * Legacy flat profile card (same permission as list/detail: owner or `members:manage`).
   * Prefer `GET /members/:id` for full detail + `summary` + `subscription`.
   */
  async getProfile(actorUserId: string, gymId: string, gymUserId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const row = await this.prisma.gymUser.findFirst({
      where: { id: gymUserId, gymId, role: GymRole.MEMBER },
      include: this.memberDetailInclude(),
    });
    if (!row) {
      throw new NotFoundException('Member not found');
    }

    const subscription = this.buildSubscriptionSummary(
      row.memberSubscriptions as MemberSubscriptionWithPlanNames[],
      now,
    );

    return {
      id: row.id,
      name: row.user.fullName ?? '',
      phone: row.user.phone,
      gender: row.gender,
      dob: row.dateOfBirth,
      join_date: row.joinedAt,
      status: computeMemberLifecycleStatus(row, now),
      profile_image: row.user.avatarUrl ?? '',
      stats: subscription.stats,
      current_subscription: subscription.current_subscription,
    };
  }

  async update(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: UpdateMemberDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.gymUser.findFirst({
      where: { id: gymUserId, gymId, role: GymRole.MEMBER },
      select: { id: true, userId: true },
    });
    if (!row) {
      throw new NotFoundException('Member not found');
    }

    if (dto.email !== undefined) {
      const emailNorm = normalizeEmailForStorage(dto.email);
      if (emailNorm) {
        const emailTaken = await this.prisma.user.findFirst({
          where: { email: emailNorm, NOT: { id: row.userId } },
          select: { id: true },
        });
        if (emailTaken) {
          throw new ConflictException(
            'This email is already registered to another account',
          );
        }
      }
    }

    await this.prisma.$transaction(async (tx) => {
      const userData: Prisma.UserUpdateInput = {};
      if (dto.fullName != null) {
        userData.fullName = dto.fullName.trim();
      }
      if (dto.email !== undefined) {
        userData.email = normalizeEmailForStorage(dto.email);
      }
      if (dto.heightCm != null) {
        userData.heightCm = new Prisma.Decimal(dto.heightCm);
      }
      if (dto.weightKg != null) {
        userData.weightKg = new Prisma.Decimal(dto.weightKg);
      }
      if (Object.keys(userData).length > 0) {
        await tx.user.update({
          where: { id: row.userId },
          data: userData,
        });
      }

      const guData: Prisma.GymUserUpdateInput = {};
      if (dto.isLead !== undefined) {
        guData.isLead = dto.isLead;
      }
      if (dto.isActive !== undefined) {
        guData.isActive = dto.isActive;
      }
      if (dto.notes !== undefined) {
        guData.notes = dto.notes?.trim() || null;
      }
      if (dto.emergencyContactName !== undefined) {
        guData.emergencyContactName = dto.emergencyContactName?.trim() || null;
      }
      if (dto.emergencyContactPhone !== undefined) {
        guData.emergencyContactPhone =
          dto.emergencyContactPhone?.trim() || null;
      }
      if (dto.dateOfBirth !== undefined) {
        guData.dateOfBirth = dto.dateOfBirth ? new Date(dto.dateOfBirth) : null;
      }
      if (dto.gender !== undefined) {
        guData.gender = dto.gender?.trim() || null;
      }
      if (dto.membershipEndsAt !== undefined) {
        guData.membershipEndsAt = dto.membershipEndsAt
          ? new Date(dto.membershipEndsAt)
          : null;
      }
      if (Object.keys(guData).length > 0) {
        await tx.gymUser.update({
          where: { id: row.id },
          data: guData,
        });
      }
    });

    return this.getDetail(actorUserId, gymId, gymUserId);
  }

  async softDelete(actorUserId: string, gymId: string, gymUserId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.gymUser.findFirst({
      where: { id: gymUserId, gymId, role: GymRole.MEMBER },
      select: { id: true },
    });
    if (!row) {
      throw new NotFoundException('Member not found');
    }
    await this.prisma.gymUser.update({
      where: { id: row.id },
      data: { isActive: false },
    });
    return { ok: true as const };
  }

  async listSubscriptions(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
  ) {
    const result = await this.subscriptions.listMemberSubscriptions(
      actorUserId,
      gymId,
      gymUserId,
      limit,
      offset,
    );
    const active = result.items.filter((x) => x.status === 'ACTIVE');
    const completed = result.items.filter((x) => x.status !== 'ACTIVE');
    return { ...result, active, completed };
  }

  async addSubscription(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: AddMemberSubscriptionDto,
  ) {
    return this.subscriptions.addMemberSubscription(
      actorUserId,
      gymId,
      gymUserId,
      dto,
    );
  }

  /**
   * Legacy: `month`+`year` → same payload as `getAttendanceSummary`; otherwise same as `getAttendanceHistory`.
   */
  async listAttendance(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
    month?: string,
    year?: string,
  ) {
    if (month != null && year != null) {
      return this.getAttendanceSummary(actorUserId, gymId, gymUserId, {
        month,
        year,
      });
    }
    return this.getAttendanceHistory(
      actorUserId,
      gymId,
      gymUserId,
      limit,
      offset,
    );
  }

  /**
   * Month dashboard: calendar, month + lifetime stats, punctuality-labelled recent logs, per-month overview.
   * Default month/year = current calendar month in the gym's `timezone`.
   */
  async getAttendanceSummary(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    opts?: {
      month?: string;
      year?: string;
      monthsOverviewLimit?: number;
      recentLimit?: number;
    },
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const memberUserId = await this.getMemberUserId(gymId, gymUserId);
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { timezone: true },
    });
    const timeZone = gym?.timezone?.trim() || 'UTC';
    const now = new Date();

    let y: number;
    let m: number;
    if (opts?.month != null && opts?.year != null) {
      m = parseInt(opts.month, 10);
      y = parseInt(opts.year, 10);
      if (m < 1 || m > 12 || y < 1970 || y > 2100) {
        throw new BadRequestException('Invalid month or year');
      }
    } else {
      const cur = MembersService.currentYearMonthInZone(now, timeZone);
      y = cur.y;
      m = cur.m;
    }

    const start = new Date(Date.UTC(y, m - 1, 1));
    const endExclusive = new Date(Date.UTC(y, m, 1));
    const daysInMonth = Math.round(
      (endExclusive.getTime() - start.getTime()) / 86_400_000,
    );

    const overviewLimit = opts?.monthsOverviewLimit ?? 24;
    const recentLimit = opts?.recentLimit ?? 20;

    const [records, lifetime, monthsOverview, recentRaw] = await Promise.all([
      this.prisma.attendanceRecord.findMany({
        where: {
          gymId,
          memberUserId,
          attendedOn: { gte: start, lt: endExclusive },
        },
        select: {
          id: true,
          attendedOn: true,
          checkedInAt: true,
          checkedOutAt: true,
        },
        orderBy: { attendedOn: 'asc' },
      }),
      this.prisma.attendanceRecord.count({ where: { gymId, memberUserId } }),
      this.loadAttendanceMonthsOverview(gymId, memberUserId, overviewLimit),
      this.prisma.attendanceRecord.findMany({
        where: { gymId, memberUserId },
        orderBy: { checkedInAt: 'desc' },
        take: recentLimit,
        select: {
          id: true,
          attendedOn: true,
          checkedInAt: true,
          checkedOutAt: true,
        },
      }),
    ]);

    const presentSet = new Set(
      records.map((r) => r.attendedOn.toISOString().slice(0, 10)),
    );
    const calendar: { date: string; status: string }[] = [];
    for (let d = 1; d <= daysInMonth; d++) {
      const dt = new Date(Date.UTC(y, m - 1, d));
      const key = dt.toISOString().slice(0, 10);
      calendar.push({
        date: key,
        status: presentSet.has(key) ? 'present' : 'absent',
      });
    }

    const recent_logs = recentRaw.map((r) =>
      this.toAttendanceLogEntry(r, timeZone, now),
    );

    return {
      filter: { year: y, month: m, month_label: monthShortLabel(m) },
      gym_timezone: timeZone,
      stats: {
        days_present_month: presentSet.size,
        days_in_month: daysInMonth,
        lifetime_check_ins: lifetime,
        present_days: presentSet.size,
        total_days: daysInMonth,
      },
      calendar,
      recent_logs,
      months_overview: monthsOverview,
    };
  }

  /** Paginated history with optional `from` / `to` (YYYY-MM-DD, UTC calendar day on `attendedOn`). */
  async getAttendanceHistory(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
    from?: string,
    to?: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const memberUserId = await this.getMemberUserId(gymId, gymUserId);
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { timezone: true },
    });
    const timeZone = gym?.timezone?.trim() || 'UTC';
    const now = new Date();

    const f = from?.trim();
    const t = to?.trim();
    if (f && t && f > t) {
      throw new BadRequestException(
        'from must be on or before to (YYYY-MM-DD)',
      );
    }

    const attendedOn: Prisma.DateTimeFilter = {};
    if (f) {
      attendedOn.gte = new Date(`${f}T00:00:00.000Z`);
    }
    if (t) {
      attendedOn.lte = new Date(`${t}T23:59:59.999Z`);
    }

    const where: Prisma.AttendanceRecordWhereInput = {
      gymId,
      memberUserId,
      ...(Object.keys(attendedOn).length > 0 ? { attendedOn } : {}),
    };

    const [total, rows] = await Promise.all([
      this.prisma.attendanceRecord.count({ where }),
      this.prisma.attendanceRecord.findMany({
        where,
        orderBy: [{ attendedOn: 'desc' }, { checkedInAt: 'desc' }],
        take: limit,
        skip: offset,
        select: {
          id: true,
          attendedOn: true,
          checkedInAt: true,
          checkedOutAt: true,
        },
      }),
    ]);

    return {
      filters: { from: from?.trim() ?? null, to: to?.trim() ?? null },
      gym_timezone: timeZone,
      total,
      limit,
      offset,
      items: rows.map((r) => this.toAttendanceLogEntry(r, timeZone, now)),
    };
  }

  private static currentYearMonthInZone(
    now: Date,
    timeZone: string,
  ): { y: number; m: number } {
    const parts = new Intl.DateTimeFormat('en', {
      timeZone: timeZone || 'UTC',
      year: 'numeric',
      month: 'numeric',
    }).formatToParts(now);
    const year = parseInt(
      parts.find((p) => p.type === 'year')?.value ?? '1970',
      10,
    );
    const month = parseInt(
      parts.find((p) => p.type === 'month')?.value ?? '1',
      10,
    );
    return { y: year, m: month };
  }

  private toAttendanceLogEntry(
    r: {
      id: string;
      attendedOn: Date;
      checkedInAt: Date;
      checkedOutAt: Date | null;
    },
    timeZone: string,
    now: Date,
  ) {
    const punctuality = classifyAttendancePunctuality(r.checkedInAt, timeZone);
    return {
      id: r.id,
      headline: 'Check-in Success',
      punctuality,
      punctuality_label: punctualityDisplayLabel(punctuality),
      attended_on: r.attendedOn.toISOString().slice(0, 10),
      checked_in_at: r.checkedInAt.toISOString(),
      checked_out_at: r.checkedOutAt?.toISOString() ?? null,
      display_relative: formatCheckInRelativeLine(r.checkedInAt, now, timeZone),
    };
  }

  private async loadAttendanceMonthsOverview(
    gymId: string,
    memberUserId: string,
    limit: number,
  ): Promise<
    Array<{
      year: number;
      month: number;
      month_label: string;
      days_present: number;
      total_check_ins: number;
      days_in_month: number;
    }>
  > {
    const rows = await this.prisma.$queryRaw<
      Array<{
        y: number;
        m: number;
        check_ins: bigint;
        days_present: bigint;
      }>
    >(
      Prisma.sql`
      SELECT
        EXTRACT(YEAR FROM "attendedOn")::int AS y,
        EXTRACT(MONTH FROM "attendedOn")::int AS m,
        COUNT(*)::bigint AS check_ins,
        COUNT(DISTINCT "attendedOn")::bigint AS days_present
      FROM "AttendanceRecord"
      WHERE "gymId" = ${gymId} AND "memberUserId" = ${memberUserId}
      GROUP BY 1, 2
      ORDER BY 1 DESC, 2 DESC
      LIMIT ${limit}
    `,
    );

    return rows.map((r) => {
      const dim = MembersService.daysInMonthUtc(r.y, r.m);
      return {
        year: r.y,
        month: r.m,
        month_label: monthShortLabel(r.m),
        days_present: Number(r.days_present),
        total_check_ins: Number(r.check_ins),
        days_in_month: dim,
      };
    });
  }

  private static daysInMonthUtc(year: number, month1to12: number): number {
    return new Date(Date.UTC(year, month1to12, 0)).getUTCDate();
  }

  async listPayments(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const memberUserId = await this.getMemberUserId(gymId, gymUserId);
    const where = {
      gymId,
      memberUserId,
    };
    const [total, rows] = await Promise.all([
      this.prisma.payment.count({ where }),
      this.prisma.payment.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        take: limit,
        skip: offset,
        select: {
          id: true,
          amountCents: true,
          currency: true,
          status: true,
          method: true,
          reference: true,
          description: true,
          memberSubscriptionId: true,
          invoiceId: true,
          completedAt: true,
          createdAt: true,
        },
      }),
    ]);
    return {
      total,
      limit,
      offset,
      items: rows,
      payments: rows.map((r) => ({
        amount: Math.round(r.amountCents / 100),
        mode: (r.method ?? 'CASH').toLowerCase(),
        date: r.completedAt ?? r.createdAt,
        status: r.status === 'COMPLETED' ? 'success' : r.status.toLowerCase(),
      })),
    };
  }

  async receivePayment(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: ReceivePaymentDto,
  ) {
    return this.subscriptions.receiveMemberPayment(
      actorUserId,
      gymId,
      gymUserId,
      dto,
    );
  }

  async getPaymentSummary(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    const memberUserId = await this.getMemberUserId(gymId, gymUserId);
    const [paidAgg, pendingAgg, last] = await Promise.all([
      this.prisma.payment.aggregate({
        where: {
          gymId,
          memberUserId,
          status: 'COMPLETED',
        },
        _sum: { amountCents: true },
      }),
      this.prisma.payment.aggregate({
        where: {
          gymId,
          memberUserId,
          status: 'PENDING',
        },
        _sum: { amountCents: true },
      }),
      this.prisma.payment.findFirst({
        where: { gymId, memberUserId, status: 'COMPLETED' },
        orderBy: { completedAt: 'desc' },
        select: { amountCents: true, completedAt: true, createdAt: true },
      }),
    ]);
    return {
      total_paid: Math.round((paidAgg._sum.amountCents ?? 0) / 100),
      outstanding: Math.round((pendingAgg._sum.amountCents ?? 0) / 100),
      last_payment: last
        ? {
            amount: Math.round(last.amountCents / 100),
            date: last.completedAt ?? last.createdAt,
          }
        : null,
    };
  }

  async listWorkouts(actorUserId: string, gymId: string, gymUserId: string) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.ensureMemberByGymUser(gymId, gymUserId);
    const rows = await this.prisma.$queryRaw<
      Array<{
        id: string;
        title: string;
        description: string | null;
        trainer_name: string | null;
        created_at: Date;
      }>
    >(Prisma.sql`
      SELECT id, title, description, trainer_name, created_at
      FROM member_workouts
      WHERE gym_id = ${gymId} AND gym_user_id = ${gymUserId}
      ORDER BY created_at DESC
      LIMIT 200
    `);
    return rows;
  }

  async addWorkout(actorUserId: string, dto: AddWorkoutDto) {
    const member = await this.requireMemberForOwner(actorUserId, dto.member_id);
    const workoutId = randomUUID();
    const rows = await this.prisma.$queryRaw<Array<{ id: string }>>(Prisma.sql`
      INSERT INTO member_workouts (id, gym_id, gym_user_id, title, description, trainer_name)
      VALUES (${workoutId}, ${member.gymId}, ${dto.member_id}, ${dto.title.trim()}, ${dto.description?.trim() ?? null}, ${dto.trainer_name?.trim() ?? null})
      RETURNING id
    `);
    return { success: true as const, workout_id: rows[0]?.id };
  }

  async listDiet(actorUserId: string, gymId: string, gymUserId: string) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.ensureMemberByGymUser(gymId, gymUserId);
    const rows = await this.prisma.$queryRaw<
      Array<{
        id: string;
        meal_type: string;
        items_json: Prisma.JsonValue;
        created_at: Date;
      }>
    >(Prisma.sql`
      SELECT id, meal_type, items_json, created_at
      FROM member_diets
      WHERE gym_id = ${gymId} AND gym_user_id = ${gymUserId}
      ORDER BY created_at DESC
      LIMIT 200
    `);
    return rows.map((r) => ({
      id: r.id,
      meal_type: r.meal_type,
      items: r.items_json,
      created_at: r.created_at,
    }));
  }

  async addDietEntry(actorUserId: string, dto: AddDietEntryDto) {
    const member = await this.requireMemberForOwner(actorUserId, dto.member_id);
    const dietId = randomUUID();
    const rows = await this.prisma.$queryRaw<Array<{ id: string }>>(Prisma.sql`
      INSERT INTO member_diets (id, gym_id, gym_user_id, meal_type, items_json)
      VALUES (${dietId}, ${member.gymId}, ${dto.member_id}, ${dto.meal_type.trim()}, ${JSON.stringify(dto.items)}::jsonb)
      RETURNING id
    `);
    return { success: true as const, diet_id: rows[0]?.id };
  }

  private async getMemberUserId(
    gymId: string,
    gymUserId: string,
  ): Promise<string> {
    const m = await this.prisma.gymUser.findFirst({
      where: { id: gymUserId, gymId, role: GymRole.MEMBER },
      select: { userId: true },
    });
    if (!m) {
      throw new NotFoundException('Member not found');
    }
    return m.userId;
  }

  private async ensureMemberByGymUser(
    gymId: string,
    gymUserId: string,
  ): Promise<void> {
    await this.getMemberUserId(gymId, gymUserId);
  }

  private async requireMemberForOwner(actorUserId: string, gymUserId: string) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: gymUserId },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);
    return member;
  }

  private async resolveGymIdForActor(
    actorUserId: string,
    gymId?: string,
  ): Promise<string> {
    if (gymId?.trim()) {
      return gymId;
    }
    const owned = await this.prisma.gym.findMany({
      where: { ownerId: actorUserId },
      select: { id: true },
      orderBy: { name: 'asc' },
      take: 2,
    });
    if (owned.length === 1) {
      return owned[0].id;
    }
    throw new BadRequestException('gymId is required');
  }

  private resolveFullName(dto: CreateMemberDto): string {
    if (dto.fullName?.trim()) {
      return dto.fullName.trim();
    }
    const first = dto.first_name?.trim() ?? '';
    const last = dto.last_name?.trim() ?? '';
    const merged = `${first} ${last}`.trim();
    if (!merged) {
      throw new BadRequestException('fullName or first_name is required');
    }
    return merged;
  }

  private composeNotes(dto: CreateMemberDto): string | null {
    const blocks: string[] = [];
    if (dto.notes?.trim()) {
      blocks.push(dto.notes.trim());
    }
    if (dto.address?.trim()) {
      blocks.push(`Address: ${dto.address.trim()}`);
    }
    if (dto.aadhaar_number?.trim()) {
      blocks.push(`Aadhaar: ${dto.aadhaar_number.trim()}`);
    }
    return blocks.length > 0 ? blocks.join('\n') : null;
  }

  /** For optional DB date columns that allow null when absent. */
  private parseNullableDateField(
    fieldLabel: string,
    value: string | undefined,
  ): Date | null {
    if (value == null || String(value).trim() === '') {
      return null;
    }
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) {
      throw new BadRequestException(`Invalid ${fieldLabel} date`);
    }
    return d;
  }

  /** For optional fields where undefined means “omit / use default”. */
  private parseOptionalDateField(
    fieldLabel: string,
    value: string | undefined,
  ): Date | undefined {
    if (value == null || String(value).trim() === '') {
      return undefined;
    }
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) {
      throw new BadRequestException(`Invalid ${fieldLabel} date`);
    }
    return d;
  }
}
