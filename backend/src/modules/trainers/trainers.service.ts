import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  AuditAction,
  AuditEntityType,
  GlobalRole,
  GymFeatureKey,
  GymRole,
  MemberSubscriptionStatus,
  PaymentStatus,
  PlanType,
  Prisma,
  SalaryPeriod,
} from '@prisma/client';
import * as bcrypt from 'bcrypt';
import { randomBytes } from 'crypto';
import { normalizeEmailForStorage } from '../../common/utils/normalize-email';
import { GymAccessService } from '../../common/services/gym-access.service';
import {
  paymentMethodFromApi,
  paymentMethodToApi,
} from '../../common/utils/payment-method.util';
import { startOfUtcDay } from '../../common/utils/utc-date';
import { monthUtcRange } from '../../common/utils/month-utc-range';
import { AuditService } from '../audit/audit.service';
import { PrismaService } from '../prisma/prisma.service';
import { GymFeaturesService } from '../gym-features/gym-features.service';
import {
  type EffectivePermissionMatrix,
  PermissionEngineService,
} from '../rbac/permission-engine.service';
import {
  ALL_TRAINER_PERMISSION_CODES,
  TRAINER_PERMISSION_CODES,
} from './trainers.constants';
import type { CreateTrainerDto } from './dto/create-trainer.dto';
import type { RecordTrainerSalaryPaymentDto } from './dto/record-trainer-salary-payment.dto';
import type { TrainerShiftDto } from './dto/trainer-shift.dto';
import type { UpdateTrainerDto } from './dto/update-trainer.dto';
import type { TrainerPermissionsDto } from './dto/trainer-permissions.dto';
import type { PayTrainerSalaryMobileDto } from './dto/pay-trainer-salary-mobile.dto';

@Injectable()
export class TrainersService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly features: GymFeaturesService,
    private readonly permissionEngine: PermissionEngineService,
    private readonly audit: AuditService,
  ) {}

  /** For staff login: RBAC toggles for the current user at this gym (trainer or staff). */
  async getSelfPermissions(actorUserId: string, gymId: string) {
    await this.assertTrainersFeature(gymId);
    const gu = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId: actorUserId,
        role: { in: [GymRole.TRAINER, GymRole.STAFF] },
        isActive: true,
      },
      select: { id: true, role: true },
    });
    if (!gu) {
      throw new ForbiddenException(
        'You are not an active trainer or staff at this gym',
      );
    }
    const permissions = await this.permissionFlags(gu.id);
    return { gymUserId: gu.id, role: gu.role, permissions };
  }

  async list(
    actorUserId: string,
    gymId: string,
    q: string | undefined,
    includeInactive: boolean | undefined,
    limit: number,
    offset: number,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const qq = q?.trim();
    const where: Prisma.GymUserWhereInput = {
      gymId,
      role: GymRole.TRAINER,
      ...(includeInactive ? {} : { isActive: true }),
      ...(qq
        ? {
            user: {
              OR: [
                { fullName: { contains: qq, mode: 'insensitive' } },
                { phone: { contains: qq } },
                { username: { contains: qq, mode: 'insensitive' } },
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
          isActive: true,
          dateOfBirth: true,
          gender: true,
          joinedAt: true,
          user: {
            select: {
              id: true,
              fullName: true,
              phone: true,
              email: true,
              username: true,
              avatarUrl: true,
            },
          },
          trainerProfile: {
            select: {
              salaryCents: true,
              salaryPeriod: true,
              contractStartsAt: true,
              contractEndsAt: true,
              experience: true,
              address: true,
            },
          },
          trainerExpertise: {
            select: {
              tag: { select: { name: true } },
            },
          },
        },
      }),
    ]);

    return {
      total,
      limit,
      offset,
      items: rows.map((r) => ({
        gymUserId: r.id,
        userId: r.user.id,
        fullName: r.user.fullName,
        phone: r.user.phone,
        email: r.user.email,
        username: r.user.username,
        avatarUrl: r.user.avatarUrl,
        isActive: r.isActive,
        dateOfBirth: r.dateOfBirth,
        gender: r.gender,
        joinedAt: r.joinedAt,
        expertise: r.trainerExpertise.map((e) => e.tag.name),
        salary: r.trainerProfile
          ? {
              salaryCents: r.trainerProfile.salaryCents,
              salaryPeriod: r.trainerProfile.salaryPeriod,
              contractStartsAt: r.trainerProfile.contractStartsAt,
              contractEndsAt: r.trainerProfile.contractEndsAt,
              experience: r.trainerProfile.experience,
              address: r.trainerProfile.address,
            }
          : null,
      })),
    };
  }

  async getBasic(actorUserId: string, gymId: string, gymUserId: string) {
    await this.assertTrainersFeature(gymId);
    await this.assertTrainerDetailAccess(actorUserId, gymId, gymUserId);
    const row = await this.loadTrainerOrThrow(gymId, gymUserId);
    const perms = await this.permissionFlags(row.id);
    const base = this.serializeTrainerDetail(row, perms);
    const mobile = await this.buildTrainerDetailMobilePanels(gymId, row);
    return { ...base, ...mobile };
  }

  /** Current user’s trainer tab at this gym (no staff manage-gym permission required). */
  async getBasicSelf(actorUserId: string, gymId: string) {
    await this.assertTrainersFeature(gymId);
    const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
      actorUserId,
      gymId,
    );
    const row = await this.loadTrainerOrThrow(gymId, gymUserId);
    const perms = await this.permissionFlags(row.id);
    return this.serializeTrainerDetail(row, perms);
  }

  /**
   * Trainer updates own profile at a gym. Does not change phone, email, permissions, or plan assignments.
   */
  async updateSelf(
    actorUserId: string,
    gymId: string,
    dto: {
      fullName?: string;
      avatarUrl?: string | null;
      dateOfBirth?: string | null;
      gender?: string | null;
      experience?: string | null;
      address?: string | null;
      salaryCents?: number | null;
      salaryPeriod?: SalaryPeriod | null;
      expertise?: string[];
      shifts?: TrainerShiftDto[];
    },
  ) {
    await this.assertTrainersFeature(gymId);
    const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
      actorUserId,
      gymId,
    );
    if (dto.shifts?.length) {
      await this.assertSubFeature(gymId, GymFeatureKey.trainer_shifts);
    }
    await this.loadTrainerOrThrow(gymId, gymUserId);

    await this.prisma.$transaction(async (tx) => {
      const gu = await tx.gymUser.findFirstOrThrow({
        where: { id: gymUserId, gymId, role: GymRole.TRAINER },
        select: { id: true, userId: true },
      });

      await tx.trainerProfile.upsert({
        where: { gymUserId: gu.id },
        create: { gymUserId: gu.id },
        update: {},
      });

      const userUpdate: Prisma.UserUpdateInput = {};
      if (dto.fullName != null) {
        userUpdate.fullName = dto.fullName.trim();
      }
      if (dto.avatarUrl !== undefined) {
        userUpdate.avatarUrl = dto.avatarUrl?.trim() || null;
      }
      if (Object.keys(userUpdate).length > 0) {
        await tx.user.update({
          where: { id: gu.userId },
          data: userUpdate,
        });
      }

      const gymUserUpdate: Prisma.GymUserUpdateInput = {};
      if (dto.dateOfBirth !== undefined) {
        gymUserUpdate.dateOfBirth = dto.dateOfBirth
          ? this.parseDateValue(dto.dateOfBirth, 'dateOfBirth')
          : null;
      }
      if (dto.gender !== undefined) {
        gymUserUpdate.gender = dto.gender?.trim() || null;
      }
      if (Object.keys(gymUserUpdate).length > 0) {
        await tx.gymUser.update({
          where: { id: gu.id },
          data: gymUserUpdate,
        });
      }

      const profileUpdate: Prisma.TrainerProfileUpdateInput = {};
      if (dto.salaryCents !== undefined) {
        profileUpdate.salaryCents = dto.salaryCents;
      }
      if (dto.salaryPeriod !== undefined) {
        profileUpdate.salaryPeriod = dto.salaryPeriod;
      }
      if (dto.experience !== undefined) {
        profileUpdate.experience = dto.experience?.trim() || null;
      }
      if (dto.address !== undefined) {
        profileUpdate.address = dto.address?.trim() || null;
      }
      if (Object.keys(profileUpdate).length > 0) {
        await tx.trainerProfile.update({
          where: { gymUserId: gu.id },
          data: profileUpdate,
        });
      }

      if (dto.expertise) {
        await this.applyExpertise(tx, gymId, gu.id, dto.expertise);
      }
      if (dto.shifts) {
        await this.replaceShifts(tx, gu.id, dto.shifts);
      }
    });

    return this.getBasicSelf(actorUserId, gymId);
  }

  /**
   * Gym catalog plans (PT/batch) assigned to this trainer + live enrollment counts.
   */
  async getPlansTab(actorUserId: string, gymId: string, gymUserId: string) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const plans = await this.prisma.gymPlan.findMany({
      where: { gymId, trainerGymUserId: gymUserId, isActive: true },
      select: { id: true, name: true, priceCents: true },
      orderBy: { name: 'asc' },
    });
    if (plans.length === 0) {
      return {
        plans: [] as {
          plan_name: string;
          members_enrolled: number;
          price: number;
        }[],
      };
    }
    const planIds = plans.map((p) => p.id);
    const counts = await this.prisma.memberSubscription.groupBy({
      by: ['gymPlanId'],
      where: {
        gymPlanId: { in: planIds },
        status: {
          in: [
            MemberSubscriptionStatus.ACTIVE,
            MemberSubscriptionStatus.FROZEN,
            MemberSubscriptionStatus.SCHEDULED,
          ],
        },
      },
      _count: { _all: true },
    });
    const countByPlan = new Map(
      counts
        .filter(
          (c): c is typeof c & { gymPlanId: string } => c.gymPlanId != null,
        )
        .map((c) => [c.gymPlanId, c._count._all]),
    );
    return {
      plans: plans.map((p) => ({
        plan_name: p.name,
        members_enrolled: countByPlan.get(p.id) ?? 0,
        price: Math.round(p.priceCents / 100),
      })),
    };
  }

  async getTrainerClients(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const rows = await this.prisma.memberSubscription.findMany({
      where: {
        gymUser: { gymId },
        gymPlan: { trainerGymUserId: gymUserId },
      },
      orderBy: { startsAt: 'desc' },
      take: 500,
      select: {
        gymUserId: true,
        status: true,
        priceCents: true,
        gymPlan: { select: { name: true } },
        gymUser: {
          select: {
            user: { select: { fullName: true, phone: true } },
          },
        },
      },
    });
    const seen = new Set<string>();
    const clients: {
      member_id: string;
      member_name: string;
      phone: string;
      plan_name: string;
      plan_price: number;
      status: string;
    }[] = [];
    for (const r of rows) {
      if (seen.has(r.gymUserId)) {
        continue;
      }
      seen.add(r.gymUserId);
      clients.push({
        member_id: r.gymUserId,
        member_name: r.gymUser.user.fullName ?? '',
        phone: r.gymUser.user.phone ?? '',
        plan_name: r.gymPlan?.name ?? '',
        plan_price: Math.round(r.priceCents / 100),
        status: r.status.toLowerCase(),
      });
    }
    return { clients };
  }

  async getTrainerRevenue(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const planRows = await this.prisma.gymPlan.findMany({
      where: { gymId, trainerGymUserId: gymUserId },
      select: { id: true },
    });
    const planIds = planRows.map((p) => p.id);
    if (planIds.length === 0) {
      return {
        total_revenue: 0,
        trend: [1, 2, 3, 4].map((week) => ({ week, revenue: 0 })),
        recent_payments: [] as {
          member_name: string;
          amount: number;
          date: string;
          mode: string;
        }[],
      };
    }
    const payBase = {
      gymId,
      status: PaymentStatus.COMPLETED,
      memberSubscription: { gymPlanId: { in: planIds } },
    } satisfies Prisma.PaymentWhereInput;

    const [totalAgg, recent] = await Promise.all([
      this.prisma.payment.aggregate({
        where: payBase,
        _sum: { amountCents: true },
      }),
      this.prisma.payment.findMany({
        where: payBase,
        orderBy: [{ completedAt: 'desc' }, { createdAt: 'desc' }],
        take: 15,
        select: {
          amountCents: true,
          method: true,
          completedAt: true,
          createdAt: true,
          memberUser: { select: { fullName: true } },
        },
      }),
    ]);

    const today = startOfUtcDay(new Date());
    const trend: { week: number; revenue: number }[] = [];
    for (let w = 0; w < 4; w++) {
      const rangeStart = new Date(today.getTime() - (4 - w) * 7 * 86_400_000);
      const rangeEnd = new Date(today.getTime() - (3 - w) * 7 * 86_400_000);
      const agg = await this.prisma.payment.aggregate({
        where: {
          ...payBase,
          OR: [
            {
              completedAt: {
                gte: rangeStart,
                lt: rangeEnd,
              },
            },
            {
              completedAt: null,
              createdAt: { gte: rangeStart, lt: rangeEnd },
            },
          ],
        },
        _sum: { amountCents: true },
      });
      trend.push({
        week: w + 1,
        revenue: Math.round((agg._sum.amountCents ?? 0) / 100),
      });
    }

    return {
      total_revenue: Math.round((totalAgg._sum.amountCents ?? 0) / 100),
      trend,
      recent_payments: recent.map((p) => ({
        member_name: p.memberUser?.fullName ?? '',
        amount: Math.round(p.amountCents / 100),
        date: (p.completedAt ?? p.createdAt).toISOString().slice(0, 10),
        mode: paymentMethodToApi(p.method ?? undefined),
      })),
    };
  }

  async getTrainerSalaryMobile(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.assertSubFeature(gymId, GymFeatureKey.trainer_payroll);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const trainer = await this.loadTrainerOrThrow(gymId, gymUserId);
    const monthlySalary = Math.round(
      (trainer.trainerProfile?.salaryCents ?? 0) / 100,
    );
    const now = new Date();
    const { start, endExclusive } = monthUtcRange(
      now.getUTCFullYear(),
      now.getUTCMonth() + 1,
    );
    const payments = await this.prisma.trainerSalaryPayment.findMany({
      where: {
        gymId,
        gymUserId,
        OR: [
          { paidAt: { gte: start, lt: endExclusive } },
          {
            paidAt: null,
            createdAt: { gte: start, lt: endExclusive },
          },
        ],
      },
      orderBy: [{ paidAt: 'desc' }, { createdAt: 'desc' }],
    });
    const paidCents = payments.reduce((s, p) => s + p.amountCents, 0);
    const paid_amount = Math.round(paidCents / 100);
    const pending_amount = Math.max(0, monthlySalary - paid_amount);
    const history = await this.prisma.trainerSalaryPayment.findMany({
      where: { gymId, gymUserId },
      orderBy: [{ paidAt: 'desc' }, { createdAt: 'desc' }],
      take: 50,
    });
    return {
      monthly_salary: monthlySalary,
      paid_amount,
      pending_amount,
      payment_history: history.map((h) => ({
        amount: Math.round(h.amountCents / 100),
        date: (h.paidAt ?? h.createdAt).toISOString().slice(0, 10),
        mode: paymentMethodToApi(h.method ?? undefined),
      })),
    };
  }

  async payTrainerSalaryMobile(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: PayTrainerSalaryMobileDto,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.assertSubFeature(gymId, GymFeatureKey.trainer_payroll);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const method = paymentMethodFromApi(dto.payment_mode);
    const amountCents = dto.amount * 100;
    const anchorDay = dto.date?.trim()
      ? (() => {
          const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dto.date!.trim());
          if (!m) {
            throw new BadRequestException('date must be YYYY-MM-DD');
          }
          return new Date(Date.UTC(+m[1], +m[2] - 1, +m[3], 0, 0, 0, 0));
        })()
      : startOfUtcDay(new Date());
    const y = anchorDay.getUTCFullYear();
    const mo = anchorDay.getUTCMonth() + 1;
    const { start: periodStart, endExclusive } = monthUtcRange(y, mo);
    const periodEnd = new Date(endExclusive.getTime() - 86_400_000);
    const paidAt = new Date();

    const row = await this.prisma.$transaction(async (tx) =>
      tx.trainerSalaryPayment.create({
        data: {
          gymId,
          gymUserId,
          amountCents,
          currency: 'USD',
          method,
          periodStart,
          periodEnd,
          paidAt,
          description: null,
        },
      }),
    );

    await this.audit.log({
      gymId,
      actorUserId,
      action: AuditAction.TRAINER_SALARY_PAID,
      entityType: AuditEntityType.GYM_USER,
      entityId: gymUserId,
      metadata: {
        salaryPaymentId: row.id,
        amountCents,
        method,
      },
    });

    const profile = await this.prisma.trainerProfile.findUnique({
      where: { gymUserId },
    });
    const monthlySalary = Math.round((profile?.salaryCents ?? 0) / 100);
    const paidInMonth = await this.prisma.trainerSalaryPayment.aggregate({
      where: {
        gymId,
        gymUserId,
        OR: [
          { paidAt: { gte: periodStart, lt: endExclusive } },
          {
            paidAt: null,
            createdAt: { gte: periodStart, lt: endExclusive },
          },
        ],
      },
      _sum: { amountCents: true },
    });
    const paidTotal = Math.round((paidInMonth._sum.amountCents ?? 0) / 100);

    return {
      success: true as const,
      payment_id: row.id,
      monthly_salary: monthlySalary,
      paid_amount_this_month: paidTotal,
      pending_amount: Math.max(0, monthlySalary - paidTotal),
    };
  }

  async getAttendanceTab(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.assertSubFeature(gymId, GymFeatureKey.trainer_attendance);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.loadTrainerOrThrow(gymId, gymUserId);
    const trainerUserId = row.userId;
    const where = { gymId, trainerUserId };
    const [total, items] = await Promise.all([
      this.prisma.trainerAttendanceRecord.count({ where }),
      this.prisma.trainerAttendanceRecord.findMany({
        where,
        orderBy: { attendedOn: 'desc' },
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
    return { total, limit, offset, items };
  }

  async getSalaryPaymentsTab(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.assertSubFeature(gymId, GymFeatureKey.trainer_payroll);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const where = { gymId, gymUserId };
    const [total, items] = await Promise.all([
      this.prisma.trainerSalaryPayment.count({ where }),
      this.prisma.trainerSalaryPayment.findMany({
        where,
        orderBy: { periodStart: 'desc' },
        take: limit,
        skip: offset,
      }),
    ]);
    return { total, limit, offset, items };
  }

  async create(actorUserId: string, dto: CreateTrainerDto) {
    await this.assertTrainersFeature(dto.gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, dto.gymId);
    if (dto.shifts?.length) {
      await this.assertSubFeature(dto.gymId, GymFeatureKey.trainer_shifts);
    }
    this.assertCredentialInput(dto.username, dto.password);

    const phone = dto.phone.trim();
    let credentialPlain: { username: string; password: string } | undefined;

    await this.prisma.$transaction(async (tx) => {
      const emailNorm = normalizeEmailForStorage(dto.email);

      const existingMembership = await tx.gymUser.findFirst({
        where: {
          gymId: dto.gymId,
          user: { phone },
        },
        select: { id: true },
      });
      if (existingMembership) {
        throw new ConflictException(
          'A user with this phone is already linked to this gym',
        );
      }

      const existingUser = await tx.user.findUnique({ where: { phone } });
      const user =
        existingUser ??
        (await tx.user.create({
          data: {
            phone,
            fullName: dto.fullName.trim(),
            ...(emailNorm ? { email: emailNorm } : {}),
            avatarUrl: dto.avatarUrl?.trim() || undefined,
          },
        }));
      if (existingUser) {
        const userUpdate: Prisma.UserUpdateInput = {
          fullName: dto.fullName.trim(),
        };
        if (dto.email != null && dto.email.length > 0) {
          userUpdate.email = emailNorm ?? undefined;
        }
        if (dto.avatarUrl !== undefined) {
          userUpdate.avatarUrl = dto.avatarUrl?.trim() || null;
        }
        await tx.user.update({
          where: { id: user.id },
          data: userUpdate,
        });
      }

      const gu = await tx.gymUser.create({
        data: {
          userId: user.id,
          gymId: dto.gymId,
          role: GymRole.TRAINER,
          isActive: true,
          dateOfBirth: dto.dateOfBirth
            ? this.parseDateValue(dto.dateOfBirth, 'dateOfBirth')
            : undefined,
          gender: dto.gender?.trim() || undefined,
        },
      });

      await tx.trainerProfile.create({
        data: {
          gymUserId: gu.id,
          salaryCents: dto.salaryCents ?? null,
          salaryPeriod: dto.salaryPeriod ?? null,
          contractStartsAt: dto.contractStartsAt
            ? new Date(dto.contractStartsAt)
            : null,
          contractEndsAt: dto.contractEndsAt
            ? new Date(dto.contractEndsAt)
            : null,
          experience: dto.experience?.trim() ?? null,
          address: dto.address?.trim() ?? null,
          notes: dto.notes?.trim() ?? null,
        },
      });

      await this.applyExpertise(tx, dto.gymId, gu.id, dto.expertise ?? []);
      if (dto.shifts?.length) {
        await this.replaceShifts(tx, gu.id, dto.shifts);
      }
      if (dto.planIds?.length) {
        await this.replacePlanAssignments(tx, dto.gymId, gu.id, dto.planIds);
      }
      await this.syncTrainerPermissions(tx, gu.id, dto.permissions);

      if (dto.username && dto.password) {
        const username = await this.assertUsernameAvailable(
          tx,
          dto.username,
          user.id,
        );
        const password = dto.password;
        const passwordHash = await bcrypt.hash(password, 10);
        await tx.user.update({
          where: { id: user.id },
          data: { username, passwordHash },
        });
        credentialPlain = { username, password };
      } else if (dto.generateLoginCredentials) {
        const username = await this.pickUsername(tx, dto.fullName);
        const password = randomBytes(12).toString('base64url').slice(0, 14);
        const passwordHash = await bcrypt.hash(password, 10);
        await tx.user.update({
          where: { id: user.id },
          data: { username, passwordHash },
        });
        credentialPlain = { username, password };
      }
    });

    const created = await this.prisma.gymUser.findFirst({
      where: {
        gymId: dto.gymId,
        role: GymRole.TRAINER,
        user: { phone },
      },
      select: { id: true },
    });
    if (!created) {
      throw new BadRequestException('Trainer create failed');
    }
    const detail = await this.getBasic(actorUserId, dto.gymId, created.id);
    return credentialPlain
      ? { ...detail, loginCredentials: credentialPlain }
      : detail;
  }

  async update(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: UpdateTrainerDto,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    if (dto.shifts?.length) {
      await this.assertSubFeature(gymId, GymFeatureKey.trainer_shifts);
    }
    await this.loadTrainerOrThrow(gymId, gymUserId);

    await this.prisma.$transaction(async (tx) => {
      const gu = await tx.gymUser.findFirstOrThrow({
        where: { id: gymUserId, gymId, role: GymRole.TRAINER },
        select: { id: true, userId: true },
      });

      const userUpdate: Prisma.UserUpdateInput = {};
      if (dto.fullName != null) {
        userUpdate.fullName = dto.fullName.trim();
      }
      if (dto.email !== undefined) {
        userUpdate.email = normalizeEmailForStorage(dto.email);
      }
      if (dto.avatarUrl !== undefined) {
        userUpdate.avatarUrl = dto.avatarUrl?.trim() || null;
      }
      if (Object.keys(userUpdate).length > 0) {
        await tx.user.update({
          where: { id: gu.userId },
          data: userUpdate,
        });
      }

      const gymUserUpdate: Prisma.GymUserUpdateInput = {};
      if (dto.isActive !== undefined) {
        gymUserUpdate.isActive = dto.isActive;
      }
      if (dto.dateOfBirth !== undefined) {
        gymUserUpdate.dateOfBirth = dto.dateOfBirth
          ? this.parseDateValue(dto.dateOfBirth, 'dateOfBirth')
          : null;
      }
      if (dto.gender !== undefined) {
        gymUserUpdate.gender = dto.gender?.trim() || null;
      }
      if (Object.keys(gymUserUpdate).length > 0) {
        await tx.gymUser.update({
          where: { id: gu.id },
          data: gymUserUpdate,
        });
      }

      const profileUpdate: Prisma.TrainerProfileUpdateInput = {};
      if (dto.salaryCents !== undefined) {
        profileUpdate.salaryCents = dto.salaryCents;
      }
      if (dto.salaryPeriod !== undefined) {
        profileUpdate.salaryPeriod = dto.salaryPeriod;
      }
      if (dto.contractStartsAt !== undefined) {
        profileUpdate.contractStartsAt = dto.contractStartsAt
          ? new Date(dto.contractStartsAt)
          : null;
      }
      if (dto.contractEndsAt !== undefined) {
        profileUpdate.contractEndsAt = dto.contractEndsAt
          ? new Date(dto.contractEndsAt)
          : null;
      }
      if (dto.experience !== undefined) {
        profileUpdate.experience = dto.experience?.trim() || null;
      }
      if (dto.address !== undefined) {
        profileUpdate.address = dto.address?.trim() || null;
      }
      if (dto.notes !== undefined) {
        profileUpdate.notes = dto.notes?.trim() ?? null;
      }
      if (Object.keys(profileUpdate).length > 0) {
        await tx.trainerProfile.update({
          where: { gymUserId: gu.id },
          data: profileUpdate,
        });
      }

      if (dto.expertise) {
        await this.applyExpertise(tx, gymId, gu.id, dto.expertise);
      }
      if (dto.shifts) {
        await this.replaceShifts(tx, gu.id, dto.shifts);
      }
      if (dto.planIds) {
        await this.replacePlanAssignments(tx, gymId, gu.id, dto.planIds);
      }
      if (dto.permissions) {
        await this.syncTrainerPermissions(tx, gu.id, dto.permissions);
      }
    });

    return this.getBasic(actorUserId, gymId, gymUserId);
  }

  async softDelete(actorUserId: string, gymId: string, gymUserId: string) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    await this.prisma.gymUser.update({
      where: { id: gymUserId },
      data: { isActive: false },
    });
    return { success: true as const };
  }

  async changePassword(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    password: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    const row = await this.loadTrainerOrThrow(gymId, gymUserId);
    const passwordHash = await bcrypt.hash(password, 10);
    await this.prisma.user.update({
      where: { id: row.userId },
      data: { passwordHash },
    });
    return { success: true as const };
  }

  async updatePermissionsCompat(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    flags: {
      add_members?: boolean;
      add_clients?: boolean;
      view_dashboard?: boolean;
      show_dashboard?: boolean;
      view_payments?: boolean;
      show_payments?: boolean;
      show_payment_in_details?: boolean;
      view_member_details?: boolean;
      add_trainer?: boolean;
    },
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    await this.prisma.$transaction(async (tx) => {
      await this.syncTrainerPermissions(tx, gymUserId, {
        members: !!(
          flags.add_members ||
          flags.add_clients ||
          flags.view_member_details
        ),
        dashboard: !!(flags.view_dashboard || flags.show_dashboard),
        payments: !!(
          flags.view_payments ||
          flags.show_payments ||
          flags.show_payment_in_details
        ),
        admin: !!flags.add_trainer,
      });
    });
    return this.getBasic(actorUserId, gymId, gymUserId);
  }

  async updatePermissions(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    flags: TrainerPermissionsDto,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    await this.prisma.$transaction(async (tx) => {
      await this.syncTrainerPermissions(tx, gymUserId, flags);
    });
    const effective = await this.permissionFlags(gymUserId);
    return {
      gymUserId,
      gymId,
      permissions: {
        dashboard: effective.dashboard,
        payments: effective.payments,
        member: effective.members,
        admin: effective.admin,
      },
    };
  }

  async getAssignedMembers(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const rows = await this.prisma.memberSubscription.findMany({
      where: {
        gymUser: { gymId },
        gymPlan: { trainerGymUserId: gymUserId },
      },
      orderBy: { startsAt: 'desc' },
      take: 200,
      select: {
        status: true,
        priceCents: true,
        gymPlan: { select: { name: true } },
        gymUser: {
          select: {
            user: { select: { fullName: true } },
          },
        },
      },
    });
    return rows.map((r) => ({
      member_name: r.gymUser.user.fullName ?? '',
      plan_name: r.gymPlan?.name ?? '',
      plan_price: Math.round(r.priceCents / 100),
      status: r.status.toLowerCase(),
    }));
  }

  async generateLoginCredentials(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.loadTrainerOrThrow(gymId, gymUserId);
    const password = randomBytes(12).toString('base64url').slice(0, 14);
    const passwordHash = await bcrypt.hash(password, 10);
    const name = row.user.fullName?.trim() ?? row.user.phone ?? 'trainer';
    const username = await this.pickUsername(this.prisma, name);
    await this.prisma.user.update({
      where: { id: row.userId },
      data: { username, passwordHash },
    });
    return {
      gymUserId: row.id,
      userId: row.userId,
      username,
      password,
      message:
        'Store this password securely; it cannot be retrieved again. Username can be used at POST /api/v1/auth/staff/login',
    };
  }

  async punchAttendance(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.assertSubFeature(gymId, GymFeatureKey.trainer_attendance);
    const row = await this.loadTrainerOrThrow(gymId, gymUserId);
    await this.assertTrainerAttendanceAccess(actorUserId, gymId, gymUserId);
    const attendedOn = startOfUtcDay(new Date());
    const existing = await this.prisma.trainerAttendanceRecord.findUnique({
      where: {
        gymId_trainerUserId_attendedOn: {
          gymId,
          trainerUserId: row.userId,
          attendedOn,
        },
      },
      select: { id: true, attendedOn: true, checkedInAt: true, checkedOutAt: true },
    });

    if (existing?.checkedOutAt) {
      return {
        ok: true as const,
        duplicate: true as const,
        attendedOn: existing.attendedOn,
        gymId,
        trainerId: gymUserId,
        checkedInAt: existing.checkedInAt.toISOString(),
        checkedOutAt: existing.checkedOutAt.toISOString(),
        message: 'Trainer already clocked out today',
      };
    }

    if (existing && !existing.checkedOutAt) {
      const now = new Date();
      const updated = await this.prisma.trainerAttendanceRecord.update({
        where: { id: existing.id },
        data: { checkedOutAt: now },
        select: { attendedOn: true, checkedInAt: true, checkedOutAt: true },
      });
      return {
        ok: true as const,
        duplicate: false as const,
        action: 'clock_out' as const,
        attendedOn: updated.attendedOn,
        gymId,
        trainerId: gymUserId,
        checkedInAt: updated.checkedInAt.toISOString(),
        checkedOutAt: updated.checkedOutAt?.toISOString() ?? null,
      };
    }

    const rec = await this.prisma.trainerAttendanceRecord.create({
      data: {
        gymId,
        trainerUserId: row.userId,
        attendedOn,
      },
      select: { attendedOn: true, checkedInAt: true },
    });
    return {
      ok: true as const,
      duplicate: false as const,
      action: 'clock_in' as const,
      attendedOn: rec.attendedOn,
      gymId,
      trainerId: gymUserId,
      checkedInAt: rec.checkedInAt.toISOString(),
    };
  }

  private async assertTrainerAttendanceAccess(
    actorUserId: string,
    gymId: string,
    targetTrainerGymUserId: string,
  ): Promise<void> {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { ownerId: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    if (gym.ownerId === actorUserId) {
      return;
    }
    const actorUser = await this.prisma.user.findUnique({
      where: { id: actorUserId },
      select: { globalRole: true },
    });
    if (actorUser?.globalRole === GlobalRole.SUPER_ADMIN) {
      return;
    }

    const actor = await this.prisma.gymUser.findFirst({
      where: { gymId, userId: actorUserId, isActive: true },
      select: { id: true, role: true },
    });
    if (!actor) {
      throw new ForbiddenException('No access to this gym');
    }
    if (actor.role === GymRole.TRAINER) {
      if (actor.id !== targetTrainerGymUserId) {
        throw new ForbiddenException(
          'Trainers can punch attendance only for themselves',
        );
      }
      return;
    }
    if (actor.role === GymRole.STAFF || actor.role === GymRole.OWNER) {
      return;
    }

    throw new ForbiddenException('Insufficient permissions for this action');
  }

  async checkInAttendance(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
  ) {
    return this.punchAttendance(actorUserId, gymId, gymUserId);
  }

  async recordSalaryPayment(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: RecordTrainerSalaryPaymentDto,
  ) {
    await this.assertTrainersFeature(gymId);
    await this.assertSubFeature(gymId, GymFeatureKey.trainer_payroll);
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.loadTrainerOrThrow(gymId, gymUserId);
    const periodStart = new Date(dto.periodStart);
    const periodEnd = new Date(dto.periodEnd);
    if (Number.isNaN(+periodStart) || Number.isNaN(+periodEnd)) {
      throw new BadRequestException('Invalid period dates');
    }
    const method = dto.payment_mode
      ? paymentMethodFromApi(dto.payment_mode)
      : undefined;
    return this.prisma.trainerSalaryPayment.create({
      data: {
        gymId,
        gymUserId,
        amountCents: dto.amountCents,
        currency: dto.currency?.trim() || 'USD',
        method,
        periodStart,
        periodEnd,
        paidAt: dto.paidAt ? new Date(dto.paidAt) : null,
        description: dto.description?.trim() ?? null,
      },
    });
  }

  private async assertTrainersFeature(gymId: string) {
    if (!(await this.features.isEnabled(gymId, GymFeatureKey.trainers))) {
      throw new ForbiddenException(
        'Trainer management is disabled for this gym',
      );
    }
  }

  private async assertTrainerDetailAccess(
    actorUserId: string,
    gymId: string,
    targetTrainerGymUserId: string,
  ): Promise<void> {
    const actor = await this.prisma.gymUser.findFirst({
      where: { gymId, userId: actorUserId, isActive: true },
      select: { id: true, role: true },
    });
    if (actor?.role === GymRole.TRAINER) {
      if (actor.id !== targetTrainerGymUserId) {
        throw new ForbiddenException(
          'Trainers can view only their own profile',
        );
      }
      return;
    }
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
  }

  private async assertSubFeature(gymId: string, key: GymFeatureKey) {
    if (!(await this.features.isEnabled(gymId, key))) {
      throw new ForbiddenException(
        'This trainer feature is turned off for this gym',
      );
    }
  }

  private async loadTrainerOrThrow(gymId: string, gymUserId: string) {
    const row = await this.prisma.gymUser.findFirst({
      where: { id: gymUserId, gymId, role: GymRole.TRAINER },
      include: {
        user: {
          select: {
            id: true,
            phone: true,
            email: true,
            fullName: true,
            username: true,
            passwordHash: true,
            avatarUrl: true,
            createdAt: true,
          },
        },
        trainerProfile: true,
        trainerExpertise: { include: { tag: true } },
        trainerShifts: {
          orderBy: [{ dayOfWeek: 'asc' }, { startTime: 'asc' }],
        },
      },
    });
    if (!row) {
      throw new NotFoundException('Trainer not found');
    }
    return row;
  }

  private async permissionFlags(gymUserId: string) {
    const rows = await this.prisma.gymUserPermission.findMany({
      where: {
        gymUserId,
        permission: { code: { in: ALL_TRAINER_PERMISSION_CODES } },
      },
      select: { permission: { select: { code: true } } },
    });
    const codes = new Set(rows.map((r) => r.permission.code));
    return this.normalizePermissionFlags({
      dashboard: codes.has(TRAINER_PERMISSION_CODES.dashboard),
      payments: codes.has(TRAINER_PERMISSION_CODES.payments),
      members: codes.has(TRAINER_PERMISSION_CODES.members),
      admin: codes.has(TRAINER_PERMISSION_CODES.admin),
    });
  }

  private serializeTrainerDetail(
    row: {
      id: string;
      isActive: boolean;
      dateOfBirth: Date | null;
      gender: string | null;
      joinedAt: Date;
      user: {
        id: string;
        phone: string;
        email: string | null;
        fullName: string | null;
        username: string | null;
        passwordHash: string | null;
        avatarUrl: string | null;
        createdAt: Date;
      };
      trainerProfile: {
        salaryCents: number | null;
        salaryPeriod: SalaryPeriod | null;
        contractStartsAt: Date | null;
        contractEndsAt: Date | null;
        experience: string | null;
        address: string | null;
        notes: string | null;
      } | null;
      trainerExpertise: { tag: { name: string } }[];
      trainerShifts: {
        id: string;
        dayOfWeek: number;
        startTime: string;
        endTime: string;
      }[];
    },
    permissions: TrainerPermissionsDto,
  ) {
    return {
      tab: 'basic' as const,
      gymUserId: row.id,
      userId: row.user.id,
      isActive: row.isActive,
      joinedAt: row.joinedAt,
      user: {
        phone: row.user.phone,
        email: row.user.email,
        fullName: row.user.fullName,
        username: row.user.username,
        avatarUrl: row.user.avatarUrl,
        staffLoginEnabled: !!(row.user.username && row.user.passwordHash),
        createdAt: row.user.createdAt,
      },
      profile: row.trainerProfile
        ? {
            dateOfBirth: row.dateOfBirth,
            gender: row.gender,
            salaryCents: row.trainerProfile.salaryCents,
            salaryPeriod: row.trainerProfile.salaryPeriod,
            contractStartsAt: row.trainerProfile.contractStartsAt,
            contractEndsAt: row.trainerProfile.contractEndsAt,
            experience: row.trainerProfile.experience,
            address: row.trainerProfile.address,
            notes: row.trainerProfile.notes,
          }
        : {
            dateOfBirth: row.dateOfBirth,
            gender: row.gender,
            salaryCents: null,
            salaryPeriod: null,
            contractStartsAt: null,
            contractEndsAt: null,
            experience: null,
            address: null,
            notes: null,
          },
      expertise: row.trainerExpertise.map((e) => e.tag.name),
      shifts: row.trainerShifts.map((s) => ({
        id: s.id,
        dayOfWeek: s.dayOfWeek,
        startTime: s.startTime,
        endTime: s.endTime,
      })),
      permissions,
      effective: this.permissionEngine.expandEffectivePermissions(
        this.toEffectivePermissionMatrix(permissions),
      ),
    };
  }

  private toEffectivePermissionMatrix(
    permissions: TrainerPermissionsDto,
  ): EffectivePermissionMatrix {
    return {
      dashboard: !!permissions.dashboard,
      payments: !!permissions.payments,
      members: !!permissions.members,
      admin: !!permissions.admin,
      leaveCreate: false,
      leaveRead: false,
      leaveUpdate: false,
      leaveDelete: false,
      leaveApprove: false,
      leaveReject: false,
      productCreate: false,
      productRead: false,
      productUpdate: false,
      productDelete: false,
    };
  }

  /**
   * Extra panels for trainer detail (mobile / dashboard): plans, attendance, member payments, salary.
   * Gated by gym features where applicable; safe empty payloads when disabled.
   */
  private async buildTrainerDetailMobilePanels(
    gymId: string,
    row: {
      id: string;
      userId: string;
      trainerProfile: { salaryCents: number | null } | null;
      trainerShifts: {
        dayOfWeek: number;
        startTime: string;
        endTime: string;
      }[];
    },
  ) {
    const [attendanceEnabled, payrollEnabled] = await Promise.all([
      this.features.isEnabled(gymId, GymFeatureKey.trainer_attendance),
      this.features.isEnabled(gymId, GymFeatureKey.trainer_payroll),
    ]);

    const [plans, payments, attendance, salary] = await Promise.all([
      this.buildTrainerPlansPanel(gymId, row.id),
      this.buildTrainerPaymentsPanel(gymId, row.id),
      attendanceEnabled
        ? this.buildTrainerAttendancePanel(gymId, row.userId, row.trainerShifts)
        : Promise.resolve(this.emptyAttendancePanel()),
      payrollEnabled
        ? this.buildTrainerSalaryPanel(
            gymId,
            row.id,
            row.trainerProfile?.salaryCents ?? null,
          )
        : Promise.resolve(this.emptySalaryPanel()),
    ]);

    return { plans, attendance, payments, salary };
  }

  private emptyAttendancePanel() {
    const now = new Date();
    const y = now.getUTCFullYear();
    const m = now.getUTCMonth() + 1;
    const daysInMonth = new Date(Date.UTC(y, m, 0)).getUTCDate();
    return {
      summary: {
        yearMonth: `${y}-${String(m).padStart(2, '0')}`,
        daysPresentThisMonth: 0,
        daysInMonth,
        lifetimeCheckIns: 0,
      },
      recentLogs: [] as {
        title: string;
        checkedInAt: string;
        status: 'ON_TIME' | 'EARLY' | 'LATE' | 'REGULAR';
      }[],
    };
  }

  private emptySalaryPanel() {
    return {
      monthlySalary: 0,
      paidAmount: 0,
      pendingAmount: 0,
      paymentHistory: [] as {
        title: string;
        date: string;
        amount: number;
        method: string;
      }[],
    };
  }

  private planTypeDisplay(type: PlanType): string {
    switch (type) {
      case PlanType.BATCH_PLAN:
        return 'Batch Plan';
      case PlanType.PT_PLAN:
        return 'PT Plan';
      case PlanType.GYM_MEMBERSHIP:
        return 'Membership';
      case PlanType.FREE_TRIAL:
        return 'Free Trial';
      default:
        return type;
    }
  }

  private formatPlanDurationLabel(durationDays: number): string {
    if (durationDays >= 365 && durationDays % 365 === 0) {
      const y = durationDays / 365;
      return `${y} Year${y === 1 ? '' : 's'}`;
    }
    if (durationDays >= 30 && durationDays % 30 === 0) {
      const mo = durationDays / 30;
      return `${mo} Month${mo === 1 ? '' : 's'}`;
    }
    return `${durationDays} Day${durationDays === 1 ? '' : 's'}`;
  }

  private formatPlanBillingCycle(durationDays: number): string {
    if (durationDays >= 365) {
      return 'per year';
    }
    if (durationDays >= 28 && durationDays <= 31) {
      return 'per month';
    }
    if (durationDays >= 7 && durationDays <= 7) {
      return 'per week';
    }
    return 'per period';
  }

  private async buildTrainerPlansPanel(gymId: string, gymUserId: string) {
    const planRows = await this.prisma.gymPlan.findMany({
      where: { gymId, trainerGymUserId: gymUserId, isActive: true },
      select: {
        id: true,
        type: true,
        name: true,
        durationDays: true,
        priceCents: true,
        currency: true,
      },
      orderBy: { name: 'asc' },
    });
    if (planRows.length === 0) {
      return {
        totalActivePlans: 0,
        totalSubscribers: 0,
        items: [] as {
          type: string;
          name: string;
          durationLabel: string;
          activeClients: number;
          price: number;
          currency: string;
          billingCycle: string;
        }[],
      };
    }
    const planIds = planRows.map((p) => p.id);
    const [counts, distinctMembers] = await Promise.all([
      this.prisma.memberSubscription.groupBy({
        by: ['gymPlanId'],
        where: {
          gymPlanId: { in: planIds },
          status: {
            in: [
              MemberSubscriptionStatus.ACTIVE,
              MemberSubscriptionStatus.FROZEN,
              MemberSubscriptionStatus.SCHEDULED,
            ],
          },
        },
        _count: { _all: true },
      }),
      this.prisma.memberSubscription.groupBy({
        by: ['gymUserId'],
        where: {
          gymPlanId: { in: planIds },
          status: {
            in: [
              MemberSubscriptionStatus.ACTIVE,
              MemberSubscriptionStatus.FROZEN,
              MemberSubscriptionStatus.SCHEDULED,
            ],
          },
        },
        _count: { _all: true },
      }),
    ]);
    const countByPlan = new Map(
      counts
        .filter(
          (c): c is typeof c & { gymPlanId: string } => c.gymPlanId != null,
        )
        .map((c) => [c.gymPlanId, c._count._all]),
    );
    return {
      totalActivePlans: planRows.length,
      totalSubscribers: distinctMembers.length,
      items: planRows.map((p) => ({
        type: this.planTypeDisplay(p.type),
        name: p.name,
        durationLabel: this.formatPlanDurationLabel(p.durationDays),
        activeClients: countByPlan.get(p.id) ?? 0,
        price: Math.round(p.priceCents / 100),
        currency: p.currency,
        billingCycle: this.formatPlanBillingCycle(p.durationDays),
      })),
    };
  }

  private trainerPaymentWhere(
    gymId: string,
    planIds: string[],
  ): Prisma.PaymentWhereInput {
    return {
      gymId,
      status: PaymentStatus.COMPLETED,
      memberSubscription: { gymPlanId: { in: planIds } },
    };
  }

  private async sumPaymentRevenueCents(
    payBase: Prisma.PaymentWhereInput,
    rangeStart: Date,
    rangeEndExclusive: Date,
  ): Promise<number> {
    const agg = await this.prisma.payment.aggregate({
      where: {
        ...payBase,
        OR: [
          {
            completedAt: {
              gte: rangeStart,
              lt: rangeEndExclusive,
            },
          },
          {
            completedAt: null,
            createdAt: { gte: rangeStart, lt: rangeEndExclusive },
          },
        ],
      },
      _sum: { amountCents: true },
    });
    return agg._sum.amountCents ?? 0;
  }

  private async buildTrainerPaymentsPanel(gymId: string, gymUserId: string) {
    const planRows = await this.prisma.gymPlan.findMany({
      where: { gymId, trainerGymUserId: gymUserId },
      select: { id: true },
    });
    const planIds = planRows.map((p) => p.id);
    if (planIds.length === 0) {
      return {
        totalRevenue: 0,
        revenueChangePercent: null as number | null,
        chart: [1, 2, 3, 4].map((week) => ({ week, revenue: 0 })),
        items: [] as {
          memberName: string;
          memberAvatarUrl: string | null;
          subtitle: string | null;
          amount: number;
          method: string;
        }[],
      };
    }
    const payBase = this.trainerPaymentWhere(gymId, planIds);
    const today = startOfUtcDay(new Date());

    const weekRanges = [0, 1, 2, 3].map((w) => ({
      week: w + 1,
      start: new Date(today.getTime() - (4 - w) * 7 * 86_400_000),
      end: new Date(today.getTime() - (3 - w) * 7 * 86_400_000),
    }));

    const currentWindowStart = new Date(today.getTime() - 28 * 86_400_000);
    const prevWindowStart = new Date(today.getTime() - 56 * 86_400_000);

    const [totalAgg, recent, trendCents, currentWindowCents, prevWindowCents] =
      await Promise.all([
        this.prisma.payment.aggregate({
          where: payBase,
          _sum: { amountCents: true },
        }),
        this.prisma.payment.findMany({
          where: payBase,
          orderBy: [{ completedAt: 'desc' }, { createdAt: 'desc' }],
          take: 15,
          select: {
            amountCents: true,
            method: true,
            completedAt: true,
            createdAt: true,
            memberUser: { select: { fullName: true, avatarUrl: true } },
          },
        }),
        Promise.all(
          weekRanges.map((r) =>
            this.sumPaymentRevenueCents(payBase, r.start, r.end),
          ),
        ),
        this.sumPaymentRevenueCents(payBase, currentWindowStart, today),
        this.sumPaymentRevenueCents(
          payBase,
          prevWindowStart,
          currentWindowStart,
        ),
      ]);

    const currentWindow = Math.round(currentWindowCents / 100);
    const prevWindow = Math.round(prevWindowCents / 100);
    let revenueChangePercent: number | null = null;
    if (prevWindow > 0) {
      revenueChangePercent = Math.round(
        ((currentWindow - prevWindow) / prevWindow) * 100,
      );
    } else if (currentWindow > 0) {
      revenueChangePercent = 100;
    }

    return {
      totalRevenue: Math.round((totalAgg._sum.amountCents ?? 0) / 100),
      revenueChangePercent,
      chart: trendCents.map((cents, i) => ({
        week: i + 1,
        revenue: Math.round(cents / 100),
      })),
      items: recent.map((p) => ({
        memberName: p.memberUser?.fullName ?? '',
        memberAvatarUrl: p.memberUser?.avatarUrl ?? null,
        subtitle: null as string | null,
        amount: Math.round(p.amountCents / 100),
        method: paymentMethodToApi(p.method ?? undefined),
      })),
    };
  }

  private timeToMinutes(t: string): number {
    const [h, m] = t.split(':').map((x) => parseInt(x, 10));
    if (Number.isNaN(h) || Number.isNaN(m)) {
      return 0;
    }
    return h * 60 + m;
  }

  private punctualityForCheckIn(
    attendedOn: Date,
    checkedInAt: Date,
    shifts: { dayOfWeek: number; startTime: string }[],
  ): 'ON_TIME' | 'EARLY' | 'LATE' | 'REGULAR' {
    const dow = attendedOn.getUTCDay();
    const dayShifts = shifts.filter((s) => s.dayOfWeek === dow);
    if (!dayShifts.length) {
      return 'REGULAR';
    }
    const startMins = Math.min(
      ...dayShifts.map((s) => this.timeToMinutes(s.startTime)),
    );
    const checkMins =
      checkedInAt.getUTCHours() * 60 + checkedInAt.getUTCMinutes();
    if (checkMins < startMins) {
      return 'EARLY';
    }
    if (checkMins > startMins + 15) {
      return 'LATE';
    }
    return 'ON_TIME';
  }

  private async buildTrainerAttendancePanel(
    gymId: string,
    trainerUserId: string,
    shifts: { dayOfWeek: number; startTime: string; endTime: string }[],
  ) {
    const now = new Date();
    const y = now.getUTCFullYear();
    const mo = now.getUTCMonth() + 1;
    const { start: monthStart, endExclusive: monthEnd } = monthUtcRange(y, mo);
    const daysInMonth = new Date(Date.UTC(y, mo, 0)).getUTCDate();

    const [lifetimeCheckIns, monthRows, recentItems] = await Promise.all([
      this.prisma.trainerAttendanceRecord.count({
        where: { gymId, trainerUserId },
      }),
      this.prisma.trainerAttendanceRecord.findMany({
        where: {
          gymId,
          trainerUserId,
          attendedOn: { gte: monthStart, lt: monthEnd },
        },
        select: { attendedOn: true },
      }),
      this.prisma.trainerAttendanceRecord.findMany({
        where: { gymId, trainerUserId },
        orderBy: { checkedInAt: 'desc' },
        take: 20,
        select: {
          attendedOn: true,
          checkedInAt: true,
        },
      }),
    ]);

    const distinctDays = new Set(
      monthRows.map((r) => r.attendedOn.toISOString().slice(0, 10)),
    );

    return {
      summary: {
        yearMonth: `${y}-${String(mo).padStart(2, '0')}`,
        daysPresentThisMonth: distinctDays.size,
        daysInMonth,
        lifetimeCheckIns,
      },
      recentLogs: recentItems.map((r) => ({
        title: 'Check-In Success',
        checkedInAt: r.checkedInAt.toISOString(),
        status: this.punctualityForCheckIn(r.attendedOn, r.checkedInAt, shifts),
      })),
    };
  }

  private async buildTrainerSalaryPanel(
    gymId: string,
    gymUserId: string,
    salaryCents: number | null,
  ) {
    const monthlySalary = Math.round((salaryCents ?? 0) / 100);
    const now = new Date();
    const { start, endExclusive } = monthUtcRange(
      now.getUTCFullYear(),
      now.getUTCMonth() + 1,
    );
    const payments = await this.prisma.trainerSalaryPayment.findMany({
      where: {
        gymId,
        gymUserId,
        OR: [
          { paidAt: { gte: start, lt: endExclusive } },
          {
            paidAt: null,
            createdAt: { gte: start, lt: endExclusive },
          },
        ],
      },
      orderBy: [{ paidAt: 'desc' }, { createdAt: 'desc' }],
    });
    const paidCents = payments.reduce((s, p) => s + p.amountCents, 0);
    const paidAmount = Math.round(paidCents / 100);
    const pendingAmount = Math.max(0, monthlySalary - paidAmount);

    const history = await this.prisma.trainerSalaryPayment.findMany({
      where: { gymId, gymUserId },
      orderBy: [{ paidAt: 'desc' }, { createdAt: 'desc' }],
      take: 50,
    });

    const paymentHistory = history.map((h) => {
      const method = paymentMethodToApi(h.method ?? undefined);
      const title =
        method === 'unknown'
          ? 'Salary payment'
          : `${method.toUpperCase()} Transfer`;
      return {
        title,
        date: (h.paidAt ?? h.createdAt).toISOString().slice(0, 10),
        amount: Math.round(h.amountCents / 100),
        method,
      };
    });

    return {
      monthlySalary,
      paidAmount,
      pendingAmount,
      paymentHistory,
    };
  }

  private async syncTrainerPermissions(
    tx: Prisma.TransactionClient,
    gymUserId: string,
    flags: TrainerPermissionsDto,
  ) {
    const normalized = this.normalizePermissionFlags(flags);
    const perms = await tx.permission.findMany({
      where: { code: { in: ALL_TRAINER_PERMISSION_CODES } },
      select: { id: true, code: true },
    });
    const byCode = new Map(perms.map((p) => [p.code, p.id]));
    await tx.gymUserPermission.deleteMany({
      where: {
        gymUserId,
        permissionId: { in: perms.map((p) => p.id) },
      },
    });
    const add: { gymUserId: string; permissionId: string }[] = [];
    if (
      normalized.dashboard &&
      byCode.get(TRAINER_PERMISSION_CODES.dashboard)
    ) {
      add.push({
        gymUserId,
        permissionId: byCode.get(TRAINER_PERMISSION_CODES.dashboard)!,
      });
    }
    if (normalized.payments && byCode.get(TRAINER_PERMISSION_CODES.payments)) {
      add.push({
        gymUserId,
        permissionId: byCode.get(TRAINER_PERMISSION_CODES.payments)!,
      });
    }
    if (normalized.members && byCode.get(TRAINER_PERMISSION_CODES.members)) {
      add.push({
        gymUserId,
        permissionId: byCode.get(TRAINER_PERMISSION_CODES.members)!,
      });
    }
    if (normalized.admin && byCode.get(TRAINER_PERMISSION_CODES.admin)) {
      add.push({
        gymUserId,
        permissionId: byCode.get(TRAINER_PERMISSION_CODES.admin)!,
      });
    }
    if (add.length) {
      await tx.gymUserPermission.createMany({ data: add });
    }
  }

  private async applyExpertise(
    tx: Prisma.TransactionClient,
    gymId: string,
    gymUserId: string,
    names: string[],
  ) {
    await tx.trainerExpertise.deleteMany({ where: { gymUserId } });
    for (const raw of names) {
      const name = raw.trim();
      if (!name) continue;
      const tag = await tx.gymExpertiseTag.upsert({
        where: { gymId_name: { gymId, name } },
        create: { gymId, name },
        update: {},
      });
      await tx.trainerExpertise.create({
        data: { gymUserId, tagId: tag.id },
      });
    }
  }

  private async replaceShifts(
    tx: Prisma.TransactionClient,
    gymUserId: string,
    shifts: TrainerShiftDto[],
  ) {
    await tx.trainerShift.deleteMany({ where: { gymUserId } });
    if (!shifts.length) return;
    await tx.trainerShift.createMany({
      data: shifts.map((s) => ({
        gymUserId,
        dayOfWeek: s.dayOfWeek,
        startTime: this.normalizeTime(s.startTime),
        endTime: this.normalizeTime(s.endTime),
      })),
    });
  }

  private normalizeTime(t: string): string {
    const [h, m] = t.split(':').map((x) => parseInt(x, 10));
    if (
      Number.isNaN(h) ||
      Number.isNaN(m) ||
      h < 0 ||
      h > 23 ||
      m < 0 ||
      m > 59
    ) {
      throw new BadRequestException(`Invalid time: ${t}`);
    }
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }

  private async replacePlanAssignments(
    tx: Prisma.TransactionClient,
    gymId: string,
    gymUserId: string,
    planIds: string[],
  ) {
    await tx.trainerPlanAssignment.deleteMany({ where: { gymUserId } });
    if (!planIds.length) return;
    const unique = [...new Set(planIds)];
    const plans = await tx.subscriptionPlan.findMany({
      where: { id: { in: unique } },
      select: { id: true },
    });
    if (plans.length !== unique.length) {
      throw new BadRequestException(
        'One or more subscription plans are invalid',
      );
    }
    await tx.trainerPlanAssignment.createMany({
      data: unique.map((planId) => ({ gymUserId, planId })),
    });
  }

  private async pickUsername(
    db: Prisma.TransactionClient | PrismaService,
    fullName: string,
  ): Promise<string> {
    const slug = fullName
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_|_$/g, '')
      .slice(0, 24);
    const base = slug || 'trainer';
    for (let i = 0; i < 30; i++) {
      const suffix = randomBytes(3).toString('hex');
      const candidate = `${base}_${suffix}`;
      const taken = await db.user.findUnique({
        where: { username: candidate },
        select: { id: true },
      });
      if (!taken) {
        return candidate;
      }
    }
    throw new BadRequestException('Could not allocate a unique username');
  }

  private normalizePermissionFlags(
    flags: Partial<TrainerPermissionsDto> | undefined,
  ) {
    const dashboard = !!(flags?.dashboard || flags?.show_dashboard);
    const payments = !!(
      flags?.payments ||
      flags?.show_payments ||
      flags?.show_payment_in_details
    );
    const members = !!(flags?.members || flags?.add_clients);
    const admin = !!(flags?.admin || flags?.add_trainer);
    return {
      dashboard,
      payments,
      members,
      admin,
      show_dashboard: dashboard,
      show_payments: payments,
      show_payment_in_details: payments,
      add_clients: members,
      add_trainer: admin,
    };
  }

  private assertCredentialInput(
    username: string | undefined,
    password: string | undefined,
  ) {
    const hasUsername = !!username?.trim();
    const hasPassword = !!password?.trim();
    if (hasUsername !== hasPassword) {
      throw new BadRequestException(
        'Provide both username and password, or neither',
      );
    }
  }

  private async assertUsernameAvailable(
    db: Prisma.TransactionClient | PrismaService,
    usernameRaw: string,
    currentUserId?: string,
  ) {
    const username = usernameRaw.trim().toLowerCase();
    if (!username) {
      throw new BadRequestException('username cannot be empty');
    }
    const taken = await db.user.findUnique({
      where: { username },
      select: { id: true },
    });
    if (taken && taken.id !== currentUserId) {
      throw new ConflictException('Username is already in use');
    }
    return username;
  }

  private parseDateValue(value: string, label: string) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      throw new BadRequestException(`Invalid ${label}`);
    }
    return date;
  }
}
