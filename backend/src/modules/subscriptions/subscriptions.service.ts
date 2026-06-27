import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { randomUUID } from 'crypto';
import {
  GymRole,
  PaymentMethod,
  MemberSubscriptionStatus,
  PaymentStatus,
  Prisma,
} from '@prisma/client';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { addUtcDays } from '../../common/utils/utc-date';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { WhatsAppAutomationService } from '../messaging/whatsapp-automation.service';
import type { AddMemberSubscriptionDto } from '../members/dto/add-subscription.dto';
import type { ReceivePaymentDto } from '../members/dto/receive-payment.dto';
import type { InitialMemberSubscriptionDto } from '../members/dto/create-member.dto';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import {
  applyPlanInterval,
  balanceDueCents,
  effectiveSubscriptionStatus,
  initialStatusFromWindow,
} from './subscription-lifecycle';
import type { CancelSubscriptionDto } from './dto/cancel-subscription.dto';
import type { ExtendSubscriptionDto } from './dto/extend-subscription.dto';
import type { FreezeSubscriptionDto } from './dto/freeze-subscription.dto';
import type { UpgradeSubscriptionDto } from './dto/upgrade-subscription.dto';
import type { RenewSubscriptionDto } from './dto/renew-subscription.dto';
import type { ReceiveGymPaymentDto } from './dto/receive-gym-payment.dto';
import type { CreateSubscriptionCompatDto } from './dto/create-subscription-compat.dto';
import {
  memberPlanDisplayName,
  memberPlanSnapshot,
} from './member-plan-snapshot';
import { NOTIFICATION_EVENTS } from '../notifications/domain-events';

type Tx = Prisma.TransactionClient;

const gymPlanListSelect = {
  id: true,
  name: true,
  type: true,
  durationDays: true,
  priceCents: true,
  currency: true,
} as const;

@Injectable()
export class SubscriptionsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly whatsapp: WhatsAppAutomationService,
    private readonly events: EventEmitter2,
    private readonly permissions: PermissionEngineService,
  ) {}

  async listForGym(
    actorUserId: string,
    gymId: string,
    tab: 'active' | 'completed',
    limit: number,
    offset: number,
    q?: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const where: Prisma.MemberSubscriptionWhereInput = {
      gymUser: {
        gymId,
        role: GymRole.MEMBER,
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
                ],
              },
            }
          : {}),
      },
      ...(tab === 'active' ? this.whereActive(now) : this.whereCompleted(now)),
    };

    const [total, rows] = await Promise.all([
      this.prisma.memberSubscription.count({ where }),
      this.prisma.memberSubscription.findMany({
        where,
        orderBy: { endsAt: 'desc' },
        take: limit,
        skip: offset,
        include: {
          plan: {
            select: {
              id: true,
              name: true,
              code: true,
              interval: true,
              priceCents: true,
              currency: true,
            },
          },
          gymPlan: { select: gymPlanListSelect },
          gymUser: {
            select: {
              id: true,
              user: {
                select: {
                  id: true,
                  fullName: true,
                  phone: true,
                },
              },
            },
          },
        },
      }),
    ]);

    return {
      total,
      limit,
      offset,
      items: rows.map((s) => ({
        id: s.id,
        status: effectiveSubscriptionStatus(s, now),
        startsAt: s.startsAt,
        endsAt: s.endsAt,
        priceCents: s.priceCents,
        paidCents: s.paidCents,
        balanceDueCents: balanceDueCents(s.priceCents, s.paidCents),
        currency: s.currency,
        freezeStartedAt: s.freezeStartedAt,
        freezeEndsAt: s.freezeEndsAt,
        plan: memberPlanSnapshot(s.plan, s.gymPlan),
        member: {
          gymUserId: s.gymUser.id,
          name: s.gymUser.user.fullName,
          phone: s.gymUser.user.phone,
        },
      })),
    };
  }

  async getDetail(actorUserId: string, gymId: string, subscriptionId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const s = await this.prisma.memberSubscription.findFirst({
      where: { id: subscriptionId, gymUser: { gymId } },
      include: {
        plan: true,
        gymPlan: true,
        gymUser: {
          select: {
            id: true,
            user: {
              select: {
                id: true,
                fullName: true,
                phone: true,
                email: true,
              },
            },
          },
        },
        payments: {
          orderBy: { createdAt: 'desc' },
          take: 50,
          select: {
            id: true,
            amountCents: true,
            currency: true,
            status: true,
            method: true,
            reference: true,
            description: true,
            completedAt: true,
            createdAt: true,
          },
        },
        invoices: {
          orderBy: { issuedAt: 'desc' },
          take: 20,
          select: {
            id: true,
            invoiceYear: true,
            invoiceNumber: true,
            totalCents: true,
            currency: true,
            status: true,
            issuedAt: true,
            lineSummary: true,
          },
        },
      },
    });
    if (!s) {
      throw new NotFoundException('Subscription not found');
    }

    return {
      id: s.id,
      status: effectiveSubscriptionStatus(s, now),
      startsAt: s.startsAt,
      endsAt: s.endsAt,
      priceCents: s.priceCents,
      paidCents: s.paidCents,
      balanceDueCents: balanceDueCents(s.priceCents, s.paidCents),
      currency: s.currency,
      freezeStartedAt: s.freezeStartedAt,
      freezeEndsAt: s.freezeEndsAt,
      plan: memberPlanSnapshot(s.plan, s.gymPlan),
      member: {
        gymUserId: s.gymUser.id,
        userId: s.gymUser.user.id,
        name: s.gymUser.user.fullName,
        phone: s.gymUser.user.phone,
        email: s.gymUser.user.email,
      },
      payments: s.payments,
      invoices: s.invoices,
    };
  }

  async renew(
    actorUserId: string,
    gymId: string,
    subscriptionId: string,
    dto: RenewSubscriptionDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    let targetId = subscriptionId;

    await this.prisma.$transaction(async (tx) => {
      const sub = await this.requireSubscriptionTx(tx, gymId, subscriptionId);
      const catalogPlanId = dto.planId ?? dto.plan_id ?? sub.planId;
      const gymPlanRef = dto.gymPlanId ?? dto.gym_plan_id ?? sub.gymPlanId;
      const refCount = [catalogPlanId, gymPlanRef].filter(Boolean).length;
      if (refCount !== 1) {
        throw new BadRequestException(
          'Subscription must reference exactly one plan (catalog planId or gymPlanId)',
        );
      }

      // Advanced renew flow: always creates a new record, never overwrites old
      const ended = true;

      if (catalogPlanId) {
        const plan = await this.assertPlanTx(tx, catalogPlanId);
        if (ended) {
          const startsAt = dto.start_date ? new Date(dto.start_date) : now;
          const endsAt = applyPlanInterval(startsAt, plan.interval);
          const status = initialStatusFromWindow(startsAt, endsAt, now);
          const priceCents = Math.max(
            0,
            (dto.price != null ? dto.price * 100 : plan.priceCents) -
              (dto.discount ?? 0) * 100,
          );
          const created = await tx.memberSubscription.create({
            data: {
              gymUserId: sub.gymUserId,
              planId: plan.id,
              gymPlanId: null,
              startsAt,
              endsAt,
              status,
              priceCents,
              paidCents: 0,
              currency: plan.currency,
            },
          });
          targetId = created.id;
        }
      } else {
        const gp = await this.assertGymPlanTx(tx, gymId, gymPlanRef!);
        if (ended) {
          const startsAt = dto.start_date ? new Date(dto.start_date) : now;
          const endsAt = addUtcDays(startsAt, gp.durationDays);
          const status = initialStatusFromWindow(startsAt, endsAt, now);
          const priceCents = Math.max(
            0,
            (dto.price != null ? dto.price * 100 : gp.priceCents) -
              (dto.discount ?? 0) * 100,
          );
          const created = await tx.memberSubscription.create({
            data: {
              gymUserId: sub.gymUserId,
              planId: null,
              gymPlanId: gp.id,
              startsAt,
              endsAt,
              status,
              priceCents,
              paidCents: 0,
              currency: gp.currency,
            },
          });
          targetId = created.id;
        }
      }

      await tx.$executeRaw(Prisma.sql`
        INSERT INTO subscription_history_events
          (id, subscription_id, gym_id, event_type, actor_user_id, payload_json)
        VALUES
          (${randomUUID()}, ${targetId}, ${gymId}, 'RENEW', ${actorUserId},
           ${JSON.stringify({
             previous_subscription_id: sub.id,
             start_date: dto.start_date ?? null,
             price: dto.price ?? null,
             discount: dto.discount ?? 0,
           })}::jsonb)
      `);
      await this.syncMembershipEndsAt(tx, sub.gymUserId);
    });

    return this.getDetail(actorUserId, gymId, targetId);
  }

  async extend(
    actorUserId: string,
    gymId: string,
    subscriptionId: string,
    dto: ExtendSubscriptionDto,
  ) {
    const addDays = dto.additional_days ?? dto.addDays;
    if (!addDays && !dto.newEndsAt) {
      throw new BadRequestException('Provide addDays or newEndsAt');
    }
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    await this.prisma.$transaction(async (tx) => {
      const sub = await this.requireSubscriptionTx(tx, gymId, subscriptionId);
      let nextEnd: Date;
      if (dto.newEndsAt) {
        nextEnd = new Date(dto.newEndsAt);
        if (!(nextEnd > sub.endsAt)) {
          throw new BadRequestException(
            'newEndsAt must be after current endsAt',
          );
        }
      } else {
        nextEnd = addUtcDays(sub.endsAt, addDays!);
      }

      await tx.memberSubscription.update({
        where: { id: sub.id },
        data: {
          endsAt: nextEnd,
          ...(dto.additionalPriceCents != null || dto.additional_fee != null
            ? {
                priceCents:
                  sub.priceCents +
                  (dto.additionalPriceCents ?? 0) +
                  (dto.additional_fee ?? 0) * 100,
              }
            : {}),
        },
      });

      await tx.$executeRaw(Prisma.sql`
        INSERT INTO subscription_history_events
          (id, subscription_id, gym_id, event_type, actor_user_id, payload_json)
        VALUES
          (${randomUUID()}, ${sub.id}, ${gymId}, 'EXTEND', ${actorUserId},
           ${JSON.stringify({
             additional_days: addDays ?? null,
             additional_fee: dto.additional_fee ?? 0,
             reason: dto.reason ?? null,
             new_ends_at: nextEnd.toISOString(),
           })}::jsonb)
      `);

      await this.syncMembershipEndsAt(tx, sub.gymUserId);
    });

    return this.getDetail(actorUserId, gymId, subscriptionId);
  }

  async upgrade(
    actorUserId: string,
    gymId: string,
    subscriptionId: string,
    dto: UpgradeSubscriptionDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    let targetId = subscriptionId;

    await this.prisma.$transaction(async (tx) => {
      const sub = await this.requireSubscriptionTx(tx, gymId, subscriptionId);
      const catalogPlanId = dto.planId?.trim();
      const gymPlanRef = dto.gymPlanId?.trim();
      const n = [catalogPlanId, gymPlanRef].filter(Boolean).length;
      if (n !== 1) {
        throw new BadRequestException(
          'Provide exactly one of planId or gymPlanId',
        );
      }

      const capEnd =
        sub.endsAt < now
          ? sub.endsAt
          : new Date(Math.min(sub.endsAt.getTime(), now.getTime()));
      await tx.memberSubscription.update({
        where: { id: sub.id },
        data: {
          status: MemberSubscriptionStatus.ENDED,
          endsAt: capEnd,
        },
      });

      const startsAt = dto.startsAt ? new Date(dto.startsAt) : now;
      let endsAt: Date;
      let created;

      if (catalogPlanId) {
        const plan = await this.assertPlanTx(tx, catalogPlanId);
        endsAt = dto.endsAt
          ? new Date(dto.endsAt)
          : applyPlanInterval(startsAt, plan.interval);
        if (!(endsAt > startsAt)) {
          throw new BadRequestException('endsAt must be after startsAt');
        }
        const status = initialStatusFromWindow(startsAt, endsAt, now);
        created = await tx.memberSubscription.create({
          data: {
            gymUserId: sub.gymUserId,
            planId: plan.id,
            gymPlanId: null,
            startsAt,
            endsAt,
            status,
            priceCents: plan.priceCents,
            paidCents: 0,
            currency: plan.currency,
          },
        });
      } else {
        const gp = await this.assertGymPlanTx(tx, gymId, gymPlanRef!);
        endsAt = dto.endsAt
          ? new Date(dto.endsAt)
          : addUtcDays(startsAt, gp.durationDays);
        if (!(endsAt > startsAt)) {
          throw new BadRequestException('endsAt must be after startsAt');
        }
        const status = initialStatusFromWindow(startsAt, endsAt, now);
        created = await tx.memberSubscription.create({
          data: {
            gymUserId: sub.gymUserId,
            planId: null,
            gymPlanId: gp.id,
            startsAt,
            endsAt,
            status,
            priceCents: gp.priceCents,
            paidCents: 0,
            currency: gp.currency,
          },
        });
      }

      targetId = created.id;

      await this.syncMembershipEndsAt(tx, sub.gymUserId);
    });

    return this.getDetail(actorUserId, gymId, targetId);
  }

  async freeze(
    actorUserId: string,
    gymId: string,
    subscriptionId: string,
    dto: FreezeSubscriptionDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const freezeDays = dto.duration_days ?? dto.freezeDays;
    if (!freezeDays) {
      throw new BadRequestException('freezeDays or duration_days is required');
    }
    const maxFreezeDays = Number(
      process.env.SUBSCRIPTION_MAX_FREEZE_DAYS ?? 60,
    );
    if (freezeDays > maxFreezeDays) {
      throw new BadRequestException(
        `freeze duration exceeds max ${maxFreezeDays} days`,
      );
    }

    await this.prisma.$transaction(async (tx) => {
      const sub = await this.requireSubscriptionTx(tx, gymId, subscriptionId);
      if (sub.endsAt <= now) {
        throw new BadRequestException(
          'Cannot freeze an already ended subscription',
        );
      }
      if (sub.status === MemberSubscriptionStatus.FROZEN) {
        throw new BadRequestException('Subscription is already frozen');
      }
      if (sub.freezeEndsAt && sub.freezeEndsAt > now) {
        throw new BadRequestException('Cannot overlap existing freeze');
      }

      const freezeStart = dto.freeze_start_date
        ? new Date(dto.freeze_start_date)
        : now;
      const freezeEndsAt = addUtcDays(freezeStart, freezeDays);
      const newEndsAt = addUtcDays(sub.endsAt, freezeDays);

      await tx.memberSubscription.update({
        where: { id: sub.id },
        data: {
          status: MemberSubscriptionStatus.FROZEN,
          endsAt: newEndsAt,
          freezeStartedAt: freezeStart,
          freezeEndsAt,
        },
      });

      await tx.$executeRaw(Prisma.sql`
        INSERT INTO subscription_history_events
          (id, subscription_id, gym_id, event_type, actor_user_id, payload_json)
        VALUES
          (${randomUUID()}, ${sub.id}, ${gymId}, 'FREEZE', ${actorUserId},
           ${JSON.stringify({
             freeze_start_date: freezeStart.toISOString().slice(0, 10),
             duration_days: freezeDays,
             freeze_fee: dto.freeze_fee ?? 0,
             reason: dto.reason ?? null,
           })}::jsonb)
      `);

      if ((dto.freeze_fee ?? 0) > 0) {
        const memberUserId = await this.getMemberUserId(gymId, sub.gymUserId);
        await tx.payment.create({
          data: {
            gymId,
            memberUserId,
            memberSubscriptionId: sub.id,
            amountCents: (dto.freeze_fee ?? 0) * 100,
            currency: sub.currency,
            status: PaymentStatus.COMPLETED,
            completedAt: new Date(),
            description: 'Freeze fee',
          },
        });
      }

      await this.syncMembershipEndsAt(tx, sub.gymUserId);
    });

    return this.getDetail(actorUserId, gymId, subscriptionId);
  }

  async unfreeze(actorUserId: string, gymId: string, subscriptionId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();

    await this.prisma.$transaction(async (tx) => {
      const sub = await this.requireSubscriptionTx(tx, gymId, subscriptionId);
      if (sub.status !== MemberSubscriptionStatus.FROZEN) {
        throw new BadRequestException('Subscription is not frozen');
      }

      const nextStatus =
        sub.endsAt <= now
          ? MemberSubscriptionStatus.ENDED
          : sub.startsAt > now
            ? MemberSubscriptionStatus.SCHEDULED
            : MemberSubscriptionStatus.ACTIVE;

      await tx.memberSubscription.update({
        where: { id: sub.id },
        data: {
          status: nextStatus,
          freezeStartedAt: null,
          freezeEndsAt: null,
        },
      });

      await this.syncMembershipEndsAt(tx, sub.gymUserId);
    });

    return this.getDetail(actorUserId, gymId, subscriptionId);
  }

  async cancel(
    actorUserId: string,
    gymId: string,
    subscriptionId: string,
    dto: CancelSubscriptionDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();

    await this.prisma.$transaction(async (tx) => {
      const sub = await this.requireSubscriptionTx(tx, gymId, subscriptionId);
      if (sub.status === MemberSubscriptionStatus.CANCELED) {
        throw new BadRequestException('Subscription is already canceled');
      }
      if (sub.status === MemberSubscriptionStatus.ENDED || sub.endsAt <= now) {
        throw new BadRequestException('Cannot cancel an ended subscription');
      }

      await tx.memberSubscription.update({
        where: { id: sub.id },
        data: {
          status: MemberSubscriptionStatus.CANCELED,
          endsAt: sub.startsAt > now ? sub.endsAt : now,
          freezeStartedAt: null,
          freezeEndsAt: null,
        },
      });

      await tx.$executeRaw(Prisma.sql`
        INSERT INTO subscription_history_events
          (id, subscription_id, gym_id, event_type, actor_user_id, payload_json)
        VALUES
          (${randomUUID()}, ${sub.id}, ${gymId}, 'CANCEL', ${actorUserId},
           ${JSON.stringify({
             reason: dto.reason?.trim() || null,
             canceled_at: now.toISOString(),
             previous_ends_at: sub.endsAt.toISOString(),
           })}::jsonb)
      `);

      await this.syncMembershipEndsAt(tx, sub.gymUserId);
    });

    return this.getDetail(actorUserId, gymId, subscriptionId);
  }

  async generateInvoice(
    actorUserId: string,
    gymId: string,
    subscriptionId: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const year = now.getUTCFullYear();

    const invoice = await this.prisma.$transaction(async (tx) => {
      const sub = await tx.memberSubscription.findFirst({
        where: { id: subscriptionId, gymUser: { gymId } },
        include: {
          plan: { select: { name: true, interval: true } },
          gymPlan: { select: { name: true, durationDays: true } },
          gymUser: {
            select: {
              id: true,
              user: { select: { fullName: true, phone: true } },
            },
          },
        },
      });
      if (!sub) {
        throw new NotFoundException('Subscription not found');
      }

      const seq = await tx.gymInvoiceSequence.upsert({
        where: { gymId_year: { gymId, year } },
        create: { gymId, year, lastNumber: 1 },
        update: { lastNumber: { increment: 1 } },
      });

      const invoiceNumber = seq.lastNumber;
      const planLabel = memberPlanDisplayName(sub.plan, sub.gymPlan);
      const lineSummary = `${planLabel} — ${sub.startsAt.toISOString().slice(0, 10)} → ${sub.endsAt.toISOString().slice(0, 10)} — ${sub.gymUser.user.fullName ?? sub.gymUser.user.phone}`;

      return tx.invoice.create({
        data: {
          gymId,
          gymUserId: sub.gymUserId,
          memberSubscriptionId: sub.id,
          invoiceYear: year,
          invoiceNumber,
          subtotalCents: sub.priceCents,
          totalCents: sub.priceCents,
          currency: sub.currency,
          lineSummary,
        },
        select: {
          id: true,
          invoiceYear: true,
          invoiceNumber: true,
          subtotalCents: true,
          totalCents: true,
          currency: true,
          status: true,
          issuedAt: true,
          lineSummary: true,
        },
      });
    });

    return invoice;
  }

  async getInvoice(actorUserId: string, gymId: string, invoiceId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const inv = await this.prisma.invoice.findFirst({
      where: { id: invoiceId, gymId },
      include: {
        memberSubscription: {
          include: {
            plan: {
              select: {
                id: true,
                name: true,
                code: true,
                interval: true,
                priceCents: true,
                currency: true,
              },
            },
            gymPlan: { select: gymPlanListSelect },
          },
        },
        gymUser: {
          include: {
            user: {
              select: { fullName: true, phone: true, email: true },
            },
          },
        },
      },
    });
    if (!inv) {
      throw new NotFoundException('Invoice not found');
    }

    return {
      id: inv.id,
      invoiceYear: inv.invoiceYear,
      invoiceNumber: inv.invoiceNumber,
      subtotalCents: inv.subtotalCents,
      totalCents: inv.totalCents,
      currency: inv.currency,
      status: inv.status,
      issuedAt: inv.issuedAt,
      lineSummary: inv.lineSummary,
      member: {
        gymUserId: inv.gymUserId,
        name: inv.gymUser.user.fullName,
        phone: inv.gymUser.user.phone,
        email: inv.gymUser.user.email,
      },
      subscription: {
        id: inv.memberSubscription.id,
        plan: memberPlanSnapshot(
          inv.memberSubscription.plan,
          inv.memberSubscription.gymPlan,
        ),
        startsAt: inv.memberSubscription.startsAt,
        endsAt: inv.memberSubscription.endsAt,
      },
    };
  }

  async listPaymentsForGym(
    actorUserId: string,
    gymId: string,
    status: PaymentStatus | undefined,
    memberGymUserId: string | undefined,
    limit: number,
    offset: number,
    search?: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    let memberUserId: string | undefined;
    if (memberGymUserId) {
      const gu = await this.prisma.gymUser.findFirst({
        where: { id: memberGymUserId, gymId, role: GymRole.MEMBER },
        select: { userId: true },
      });
      if (!gu) {
        return { total: 0, limit, offset, items: [] };
      }
      memberUserId = gu.userId;
    }

    const where: Prisma.PaymentWhereInput = {
      gymId,
      ...(status ? { status } : {}),
      ...(memberUserId ? { memberUserId } : {}),
      ...(search?.trim()
        ? {
            OR: [
              { reference: { contains: search.trim(), mode: 'insensitive' } },
              { description: { contains: search.trim(), mode: 'insensitive' } },
              {
                memberUser: {
                  fullName: { contains: search.trim(), mode: 'insensitive' },
                },
              },
            ],
          }
        : {}),
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
          memberUser: {
            select: {
              id: true,
              fullName: true,
              phone: true,
            },
          },
        },
      }),
    ]);

    return { total, limit, offset, items: rows };
  }

  async addMemberSubscription(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: AddMemberSubscriptionDto,
  ) {
    await this.createMemberSubscription(actorUserId, gymId, gymUserId, dto);
    return this.listMemberSubscriptions(actorUserId, gymId, gymUserId, 20, 0);
  }

  async createMemberSubscription(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: Pick<
      AddMemberSubscriptionDto,
      'planId' | 'gymPlanId' | 'startsAt' | 'endsAt' | 'priceCents' | 'currency'
    >,
  ): Promise<string> {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.ensureMember(gymId, gymUserId);
    const now = new Date();

    let newSubId: string | undefined;
    await this.prisma.$transaction(async (tx) => {
      newSubId = await this.createSubscriptionTx(tx, gymUserId, dto, now);
      await this.syncMembershipEndsAt(tx, gymUserId);
    });

    if (newSubId) {
      this.events.emit(NOTIFICATION_EVENTS.PLAN_ASSIGNED, {
        gymId,
        gymUserId,
        memberSubscriptionId: newSubId,
        actorUserId: actorUserId,
      });
    }

    return newSubId!;
  }

  async createInitialSubscriptionTx(
    tx: Tx,
    gymUserId: string,
    dto: InitialMemberSubscriptionDto,
    now: Date,
  ): Promise<string> {
    return this.createSubscriptionTx(
      tx,
      gymUserId,
      {
        planId: dto.planId,
        gymPlanId: dto.gymPlanId,
        startsAt: dto.startsAt,
        endsAt: dto.endsAt,
        priceCents: dto.priceCents,
        currency: dto.currency,
      },
      now,
    );
  }

  /**
   * Records a payment; completed amounts update subscription paidCents when linked.
   */
  async receiveMemberPayment(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: ReceivePaymentDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const memberUserId = await this.getMemberUserId(gymId, gymUserId);

    const status = dto.status ?? PaymentStatus.COMPLETED;

    const result = await this.prisma.$transaction(async (tx) => {
      if (status === PaymentStatus.COMPLETED && dto.memberSubscriptionId) {
        const sub = await tx.memberSubscription.findFirst({
          where: { id: dto.memberSubscriptionId, gymUserId },
        });
        if (!sub) {
          throw new BadRequestException(
            'Invalid memberSubscriptionId for this member',
          );
        }
        const due = balanceDueCents(sub.priceCents, sub.paidCents);
        if (dto.amountCents > due) {
          throw new BadRequestException(
            'Amount exceeds remaining balance for this subscription',
          );
        }
      }

      const row = await tx.payment.create({
        data: {
          gymId,
          amountCents: dto.amountCents,
          currency: dto.currency ?? 'USD',
          status,
          method: dto.method,
          reference: dto.reference?.trim(),
          description: dto.description?.trim(),
          memberUserId,
          memberSubscriptionId: dto.memberSubscriptionId,
          completedAt: status === PaymentStatus.COMPLETED ? new Date() : null,
        },
        select: { id: true },
      });

      if (status === PaymentStatus.COMPLETED && dto.memberSubscriptionId) {
        await tx.memberSubscription.update({
          where: { id: dto.memberSubscriptionId },
          data: {
            paidCents: { increment: dto.amountCents },
          },
        });
      }

      await this.syncMembershipEndsAt(tx, gymUserId);

      return tx.payment.findUniqueOrThrow({
        where: { id: row.id },
        select: {
          id: true,
          amountCents: true,
          currency: true,
          status: true,
          method: true,
          reference: true,
          description: true,
          memberSubscriptionId: true,
          completedAt: true,
          createdAt: true,
        },
      });
    });

    if (status === PaymentStatus.COMPLETED) {
      void this.whatsapp.enqueuePaymentConfirmation(
        gymId,
        memberUserId,
        result.id,
      );
      this.events.emit(NOTIFICATION_EVENTS.PAYMENT_RECEIVED, {
        gymId,
        gymUserId,
        paymentId: result.id,
        amountCents: result.amountCents,
        currency: result.currency,
        memberSubscriptionId: result.memberSubscriptionId,
        actorUserId: actorUserId,
      });
    }

    return result;
  }

  async paymentAnalytics(
    actorUserId: string,
    gymId: string,
    fromIso: string,
    toIso: string,
    range: 'weekly' | 'monthly' | 'yearly' = 'monthly',
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const from = new Date(fromIso);
    const to = new Date(toIso);
    if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) {
      throw new BadRequestException('Invalid date range');
    }
    if (from > to) {
      throw new BadRequestException('from must be before or equal to to');
    }

    const payments = await this.prisma.payment.findMany({
      where: {
        gymId,
        status: PaymentStatus.COMPLETED,
        completedAt: { gte: from, lte: to },
      },
      orderBy: { completedAt: 'desc' },
      take: 500,
      select: {
        id: true,
        amountCents: true,
        currency: true,
        method: true,
        reference: true,
        description: true,
        memberSubscriptionId: true,
        completedAt: true,
        createdAt: true,
        memberUserId: true,
      },
    });

    const byDay = new Map<string, number>();
    for (const p of payments) {
      const at = p.completedAt ?? p.createdAt;
      const d = at.toISOString().slice(0, 10);
      byDay.set(d, (byDay.get(d) ?? 0) + p.amountCents);
    }
    const series = [...byDay.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, amountCents]) => ({ date, amountCents }));
    const totalCents = payments.reduce((s, p) => s + p.amountCents, 0);
    const recent_transactions = payments.slice(0, 20).map((p) => ({
      member_name: '',
      amount: Math.round(p.amountCents / 100),
      date: p.completedAt ?? p.createdAt,
      mode: (p.method ?? 'CASH').toLowerCase(),
    }));
    const chart_data = series.map((p) => ({
      month:
        range === 'yearly'
          ? p.date.slice(0, 4)
          : range === 'monthly'
            ? p.date.slice(5, 7)
            : p.date.slice(5, 10),
      revenue: Math.round(p.amountCents / 100),
    }));
    return {
      total_revenue: Math.round(totalCents / 100),
      total_transactions: payments.length,
      chart_data,
      recent_transactions,
      totalCents,
      currency: payments[0]?.currency ?? 'USD',
      series,
      items: payments,
    };
  }

  async receivePaymentByMember(actorUserId: string, dto: ReceiveGymPaymentDto) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);
    const method =
      dto.payment_mode === 'cash'
        ? PaymentMethod.CASH
        : dto.payment_mode === 'card'
          ? PaymentMethod.CARD
          : PaymentMethod.UPI;
    return this.receiveMemberPayment(actorUserId, member.gymId, dto.member_id, {
      amountCents: dto.amount * 100,
      method,
      status: PaymentStatus.COMPLETED,
      memberSubscriptionId: dto.subscription_id,
    });
  }

  async createSubscriptionCompat(
    actorUserId: string,
    dto: CreateSubscriptionCompatDto,
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.permissions.assertOwnerOrPermission(
      actorUserId,
      member.gymId,
      PERMISSION_CODES.MEMBERS,
    );

    const subscriptionId = await this.createMemberSubscription(
      actorUserId,
      member.gymId,
      member.id,
      this.mapCompatToAddMemberSubscription(dto),
    );
    return { success: true as const, subscription_id: subscriptionId };
  }

  async createSubscriptionTx(
    tx: Tx,
    gymUserId: string,
    dto: Pick<
      AddMemberSubscriptionDto,
      'planId' | 'gymPlanId' | 'startsAt' | 'endsAt' | 'priceCents' | 'currency'
    >,
    now: Date,
  ): Promise<string> {
    const gu = await tx.gymUser.findUniqueOrThrow({
      where: { id: gymUserId },
      select: { gymId: true },
    });

    const hasCatalog = !!dto.planId?.trim();
    const hasGym = !!dto.gymPlanId?.trim();
    if (hasCatalog === hasGym) {
      throw new BadRequestException(
        'Provide exactly one of planId (catalog) or gymPlanId (gym plan)',
      );
    }

    const startsAt = new Date(dto.startsAt);
    const endsAt = new Date(dto.endsAt);
    if (!(endsAt > startsAt)) {
      throw new BadRequestException('endsAt must be after startsAt');
    }

    const status = initialStatusFromWindow(startsAt, endsAt, now);

    if (hasCatalog) {
      const plan = await tx.subscriptionPlan.findFirst({
        where: { id: dto.planId, isActive: true },
      });
      if (!plan) {
        throw new NotFoundException('Subscription plan not found or inactive');
      }
      const priceCents = dto.priceCents ?? plan.priceCents;
      const currency = dto.currency ?? plan.currency;
      const created = await tx.memberSubscription.create({
        data: {
          gymUserId,
          planId: plan.id,
          gymPlanId: null,
          startsAt,
          endsAt,
          status,
          priceCents,
          paidCents: 0,
          currency,
        },
        select: { id: true },
      });
      return created.id;
    }

    const gp = await this.assertGymPlanTx(tx, gu.gymId, dto.gymPlanId!);
    const priceCents = dto.priceCents ?? gp.priceCents;
    const currency = dto.currency ?? gp.currency;
    const created = await tx.memberSubscription.create({
      data: {
        gymUserId,
        planId: null,
        gymPlanId: gp.id,
        startsAt,
        endsAt,
        status,
        priceCents,
        paidCents: 0,
        currency,
      },
      select: { id: true },
    });
    return created.id;
  }

  async syncMembershipEndsAt(db: Tx | PrismaService, gymUserId: string) {
    const latest = await db.memberSubscription.findFirst({
      where: {
        gymUserId,
        status: {
          notIn: [
            MemberSubscriptionStatus.ENDED,
            MemberSubscriptionStatus.CANCELED,
          ],
        },
      },
      orderBy: { endsAt: 'desc' },
    });
    await db.gymUser.update({
      where: { id: gymUserId },
      data: {
        membershipEndsAt: latest?.endsAt ?? null,
      },
    });
  }

  async listMemberSubscriptions(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    limit: number,
    offset: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.ensureMember(gymId, gymUserId);
    const now = new Date();
    const where = { gymUserId };
    const [total, rows] = await Promise.all([
      this.prisma.memberSubscription.count({ where }),
      this.prisma.memberSubscription.findMany({
        where,
        orderBy: { startsAt: 'desc' },
        take: limit,
        skip: offset,
        include: {
          plan: {
            select: {
              id: true,
              name: true,
              code: true,
              interval: true,
              priceCents: true,
              currency: true,
            },
          },
          gymPlan: { select: gymPlanListSelect },
        },
      }),
    ]);

    return {
      total,
      limit,
      offset,
      items: rows.map((s) => ({
        id: s.id,
        status: effectiveSubscriptionStatus(s, now),
        startsAt: s.startsAt,
        endsAt: s.endsAt,
        priceCents: s.priceCents,
        paidCents: s.paidCents,
        balanceDueCents: balanceDueCents(s.priceCents, s.paidCents),
        currency: s.currency,
        freezeStartedAt: s.freezeStartedAt,
        freezeEndsAt: s.freezeEndsAt,
        plan: memberPlanSnapshot(s.plan, s.gymPlan),
      })),
    };
  }

  private whereActive(now: Date): Prisma.MemberSubscriptionWhereInput {
    return {
      endsAt: { gt: now },
      status: {
        notIn: [
          MemberSubscriptionStatus.CANCELED,
          MemberSubscriptionStatus.ENDED,
        ],
      },
    };
  }

  private whereCompleted(now: Date): Prisma.MemberSubscriptionWhereInput {
    return {
      OR: [
        { endsAt: { lte: now } },
        { status: MemberSubscriptionStatus.CANCELED },
        { status: MemberSubscriptionStatus.ENDED },
      ],
    };
  }

  private async requireSubscriptionTx(
    tx: Tx,
    gymId: string,
    subscriptionId: string,
  ) {
    const sub = await tx.memberSubscription.findFirst({
      where: { id: subscriptionId, gymUser: { gymId } },
    });
    if (!sub) {
      throw new NotFoundException('Subscription not found');
    }
    return sub;
  }

  private async assertPlanTx(tx: Tx, planId: string) {
    const plan = await tx.subscriptionPlan.findFirst({
      where: { id: planId, isActive: true },
    });
    if (!plan) {
      throw new NotFoundException('Subscription plan not found or inactive');
    }
    return plan;
  }

  private async assertGymPlanTx(tx: Tx, gymId: string, gymPlanId: string) {
    const gp = await tx.gymPlan.findFirst({
      where: { id: gymPlanId, gymId, isActive: true },
    });
    if (!gp) {
      throw new NotFoundException('Gym plan not found or inactive');
    }
    return gp;
  }

  private async ensureMember(gymId: string, gymUserId: string) {
    await this.getMemberUserId(gymId, gymUserId);
  }

  private mapCompatToAddMemberSubscription(
    dto: CreateSubscriptionCompatDto,
  ): AddMemberSubscriptionDto {
    const start = new Date(dto.start_date);
    if (Number.isNaN(start.getTime())) {
      throw new BadRequestException('Invalid start_date');
    }
    const duration = dto.duration_months ?? 1;
    const end = new Date(start);
    end.setUTCMonth(end.getUTCMonth() + duration);
    const discount = dto.discount ?? 0;

    return {
      planId: dto.plan_id,
      startsAt: start.toISOString(),
      endsAt: end.toISOString(),
      priceCents: Math.max(0, (dto.price - discount) * 100),
      currency: 'USD',
    };
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
}
