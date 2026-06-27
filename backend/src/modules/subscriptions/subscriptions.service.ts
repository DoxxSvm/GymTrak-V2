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
import { addUtcDays, startOfUtcDay } from '../../common/utils/utc-date';
import { monthUtcRange } from '../../common/utils/month-utc-range';
import {
  paymentMethodDisplayLabel,
  paymentMethodFromApi,
  paymentMethodToApi,
} from '../../common/utils/payment-method.util';
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
  getMemberSubscriptionUiBucket,
  getSubscriptionWindowStatus,
  initialStatusFromWindow,
  SubscriptionWindowStatus,
} from './subscription-lifecycle';
import type { CancelSubscriptionDto } from './dto/cancel-subscription.dto';
import type { ExtendSubscriptionDto } from './dto/extend-subscription.dto';
import type { FreezeSubscriptionDto } from './dto/freeze-subscription.dto';
import type { UpgradeSubscriptionDto } from './dto/upgrade-subscription.dto';
import type { RenewSubscriptionDto } from './dto/renew-subscription.dto';
import type { ReceiveGymPaymentDto } from './dto/receive-gym-payment.dto';
import type { CreateSubscriptionCompatDto } from './dto/create-subscription-compat.dto';
import type { CreateTrainerSalaryPaymentDto } from './dto/create-trainer-salary-payment.dto';
import type { TrainerSalaryHistoryQueryDto } from './dto/trainer-salary-history-query.dto';
import {
  memberPlanDisplayName,
  memberPlanSnapshot,
} from './member-plan-snapshot';
import { NOTIFICATION_EVENTS } from '../notifications/domain-events';
import { GSTCalculate } from 'src/common/utils/gst-calculate';

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
  ) { }

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
            (dto.price != null ? dto.price : plan.priceCents) -
            (dto.discount ?? 0),
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
            (dto.price != null ? dto.price : gp.priceCents) -
            (dto.discount ?? 0),
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
                (dto.additional_fee ?? 0),
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
            userId: actorUserId,
            memberUserId,
            memberSubscriptionId: sub.id,
            amountCents: (dto.freeze_fee ?? 0),
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
        return { total: 0, limit, offset, items: [], paymentHistory: [] };
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
          userId: true,
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
          memberSubscription: {
            select: {
              gymPlan: { select: { name: true } },
              plan: { select: { name: true } },
            },
          },
        },
      }),
    ]);

    const recorderIds = [
      ...new Set(rows.map((p) => p.userId).filter(Boolean)),
    ] as string[];
    const recorders =
      recorderIds.length === 0
        ? []
        : await this.prisma.user.findMany({
          where: { id: { in: recorderIds } },
          select: { id: true, fullName: true },
        });
    const receivedByNameByUserId = new Map(
      recorders.map((u) => [u.id, u.fullName] as const),
    );

    const planNameForPayment = (p: typeof rows[number]) =>
      p.memberSubscription?.gymPlan?.name ??
      p.memberSubscription?.plan?.name ??
      '';

    const receivedByForPayment = (p: typeof rows[number]) =>
      receivedByNameByUserId.get(p.userId) ?? null;

    const mapPaymentHistoryRow = (p: typeof rows[number]) => {
      const at = p.completedAt ?? p.createdAt;
      const gymPlanName = planNameForPayment(p);
      const receivedBy = receivedByForPayment(p);
      return {
        gymPlanName,
        plan_name: gymPlanName,
        paymentMethod: paymentMethodDisplayLabel(p.method),
        date: at.toISOString().slice(0, 10),
        receivedBy,
        received_by_name: receivedBy,
        amount: p.amountCents,
      };
    };

    const items = rows.map((p) => {
      const gymPlanName = planNameForPayment(p);
      const receivedBy = receivedByForPayment(p);
      return {
        id: p.id,
        amountCents: p.amountCents,
        currency: p.currency,
        status: p.status,
        method: p.method,
        reference: p.reference,
        description: p.description,
        memberSubscriptionId: p.memberSubscriptionId,
        invoiceId: p.invoiceId,
        completedAt: p.completedAt,
        createdAt: p.createdAt,
        memberUser: p.memberUser,
        gymPlanName,
        plan_name: gymPlanName,
        receivedBy,
        received_by_name: receivedBy,
      };
    });

    return {
      total,
      limit,
      offset,
      items,
      paymentHistory: rows.map(mapPaymentHistoryRow),
    };
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
        const remainderTowardCycle =
          sub.priceCents > 0 ? sub.paidCents % sub.priceCents : 0;
        const openForCycle = balanceDueCents(
          sub.priceCents,
          remainderTowardCycle,
        );
        const maxAccept =
          sub.priceCents > 0
            ? openForCycle + 120 * sub.priceCents
            : dto.amountCents;
        if (dto.amountCents > maxAccept) {
          throw new BadRequestException(
            'Amount exceeds remaining balance for this subscription',
          );
        }
      }

      if (dto.type === 'extend_plan') {
        throw new BadRequestException(
          'extend_plan only creates a MemberSubscription; use POST /payments extend_plan flow',
        );
      }

      const row = await tx.payment.create({
        data: {
          gymId,
          userId: actorUserId,
          amountCents: dto.amountCents,
          currency: dto.currency ?? 'INR',
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

      const now = new Date();
      if (status === PaymentStatus.COMPLETED && dto.memberSubscriptionId) {
        const subAfter = await tx.memberSubscription.update({
          where: { id: dto.memberSubscriptionId },
          data: {
            paidCents: { increment: dto.amountCents },
          },
          include: {
            gymPlan: { select: { durationDays: true } },
            plan: { select: { interval: true } },
          },
        });

        const renewal = this.applyPaidPeriodsToSubscription(subAfter, now);
        if (renewal) {
          await tx.memberSubscription.update({
            where: { id: dto.memberSubscriptionId },
            data: {
              endsAt: renewal.endsAt,
              paidCents: renewal.paidCents,
              status: renewal.status,
            },
          });
        }
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

    if (
      status === PaymentStatus.COMPLETED &&
      (dto.type === 'receive_payment' || dto.type === undefined)
    ) {
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

    const memberUserIds = [
      ...new Set(payments.map((p) => p.memberUserId).filter(Boolean)),
    ];
    const members =
      memberUserIds.length > 0
        ? await this.prisma.user.findMany({
          where: { id: { in: memberUserIds as string[] } },
          select: { id: true, fullName: true },
        })
        : [];
    const memberNameMap = new Map(members.map((m) => [m.id, m.fullName ?? '']));

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
      member_name: memberNameMap.get(p.memberUserId ?? '') ?? '',
      amount: p.amountCents,
      date: p.completedAt ?? p.createdAt,
      mode: (p.method ?? 'CASH').toLowerCase(),
    }));

    const chart_data = this.buildChartData(series, range, from, to);

    return {
      total_revenue: totalCents,
      total_transactions: payments.length,
      range,
      chart_data,
      recent_transactions,
      totalCents,
      currency: payments[0]?.currency ?? 'INR',
      series,
      items: payments,
    };
  }

  private buildChartData(
    series: Array<{ date: string; amountCents: number }>,
    range: 'weekly' | 'monthly' | 'yearly',
    from: Date,
    to: Date,
  ): Array<{ label: string; revenue: number }> {
    const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const MONTHS = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];

    if (range === 'weekly') {
      const dayMap = new Map<string, number>();
      for (const s of series) {
        dayMap.set(s.date, (dayMap.get(s.date) ?? 0) + s.amountCents);
      }
      const monday = new Date(from);
      monday.setUTCHours(0, 0, 0, 0);
      return WEEKDAYS.map((label, i) => {
        const d = new Date(monday);
        d.setUTCDate(monday.getUTCDate() + i);
        const key = d.toISOString().slice(0, 10);
        return {
          label,
          revenue: dayMap.get(key) ?? 0,
        };
      });
    }

    if (range === 'monthly') {
      const year = from.getUTCFullYear();
      const monthMap = new Map<number, number>();
      for (const s of series) {
        const y = parseInt(s.date.slice(0, 4), 10);
        if (y !== year) {
          continue;
        }
        const monthNum = parseInt(s.date.slice(5, 7), 10);
        if (!Number.isNaN(monthNum)) {
          monthMap.set(monthNum, (monthMap.get(monthNum) ?? 0) + s.amountCents);
        }
      }
      return MONTHS.map((label, i) => ({
        label,
        revenue: monthMap.get(i + 1) ?? 0,
      }));
    }

    const yearMap = new Map<number, number>();
    for (const s of series) {
      const y = parseInt(s.date.slice(0, 4), 10);
      if (!Number.isNaN(y)) {
        yearMap.set(y, (yearMap.get(y) ?? 0) + s.amountCents);
      }
    }
    return [...yearMap.entries()]
      .filter(([, revenue]) => revenue > 0)
      .sort(([a], [b]) => a - b)
      .map(([y, revenue]) => ({
        label: String(y),
        revenue,
      }));
  }

  async receivePaymentByMember(actorUserId: string, dto: ReceiveGymPaymentDto) {
    const extendExistingId = dto.member_subscription_id?.trim();
    if (extendExistingId) {
      return this.extendExistingSubscriptionViaPayment(actorUserId, {
        ...dto,
        type: dto.type ?? 'extend_plan',
        member_subscription_id: extendExistingId,
      });
    }
    if (dto.type === 'extend_plan') {
      throw new BadRequestException(
        'extend_plan on an existing subscription requires member_subscription_id',
      );
    }

    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: {
        id: true,
        gymId: true,
        role: true,
        user: { select: { fullName: true, username: true, phone: true } },
      },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);
    const memberName =
      member.user.fullName?.trim() ||
      member.user.username?.trim() ||
      member.user.phone?.trim() ||
      'Member';

    const plan = await this.prisma.gymPlan.findFirst({
      where: { id: dto.gym_plan_id, gymId: member.gymId },
      select: {
        id: true,
        durationDays: true,
        priceCents: true,
        currency: true,
        isActive: true,
      },
    });
    if (!plan) {
      throw new NotFoundException('Gym plan not found for this gym');
    }

    const periodStart = this.resolveGymPaymentPeriodStart(dto);
    const sellingPriceCents = this.resolveRequestSellingPrice(dto);
    const memberSubscriptionId =
      await this.createMemberSubscriptionForGymPlanPayment(
        dto.member_id,
        plan,
        periodStart,
        sellingPriceCents ?? undefined,
      );

    const createdSub = await this.prisma.memberSubscription.findUniqueOrThrow({
      where: { id: memberSubscriptionId },
      select: { priceCents: true, paidCents: true },
    });
    const amountPending = Math.max(
      0,
      createdSub.priceCents - createdSub.paidCents,
    );

    this.events.emit(NOTIFICATION_EVENTS.EXTEND_PLAN, {
      gymId: member.gymId,
      gymUserId: dto.member_id,
      memberName,
      memberSubscriptionId,
      actorUserId,
    });

    return {
      type: 'extend_plan' as const,
      memberSubscriptionId,
      price_cents: createdSub.priceCents,
      selling_price: sellingPriceCents ?? createdSub.priceCents,
      catalog_plan_price: plan.priceCents,
      paid_cents: createdSub.paidCents,
      amount_pending: amountPending,
    };
  }

  /**
   * `POST /payments` **extend_plan** with `member_subscription_id`: extend that in-window
   * subscription's `endsAt` to `date`. Only `fees` / `addition_fee` is added to `priceCents`;
   * `amount` records payment / `paidCents` only. Gym plan catalog `priceCents` is never applied.
   */
  private async extendExistingSubscriptionViaPayment(
    actorUserId: string,
    dto: ReceiveGymPaymentDto,
  ) {
    const memberSubscriptionId = dto.member_subscription_id!.trim();
    if (!dto.date?.trim()) {
      throw new BadRequestException(
        'date is required when extending an existing subscription (new expiry YYYY-MM-DD)',
      );
    }
    if (dto.amount != null && dto.amount > 0 && !dto.payment_mode) {
      throw new BadRequestException(
        'payment_mode is required when amount is provided for extend_plan',
      );
    }

    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: {
        id: true,
        gymId: true,
        role: true,
        user: { select: { fullName: true, username: true, phone: true } },
      },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);

    const existing = await this.prisma.memberSubscription.findFirst({
      where: { id: memberSubscriptionId, gymUserId: dto.member_id },
      select: {
        id: true,
        gymUserId: true,
        startsAt: true,
        endsAt: true,
        priceCents: true,
        paidCents: true,
        currency: true,
        status: true,
        freezeEndsAt: true,
        gymPlanId: true,
      },
    });
    if (!existing) {
      throw new NotFoundException(
        'Member subscription not found for this member',
      );
    }

    if (dto.gym_plan_id?.trim() && existing.gymPlanId) {
      if (dto.gym_plan_id.trim() !== existing.gymPlanId) {
        throw new BadRequestException(
          'gym_plan_id must match the subscription being extended',
        );
      }
    }

    const now = new Date();
    if (existing.status === MemberSubscriptionStatus.CANCELED) {
      throw new BadRequestException('Cannot extend a canceled subscription');
    }
    // Date window — not UI bucket (ENDED rows with a future endsAt are still extendable).
    const windowBucket = getSubscriptionWindowStatus(
      existing.startsAt,
      existing.endsAt,
      now,
    );
    if (windowBucket !== SubscriptionWindowStatus.CURRENT) {
      const reason =
        windowBucket === SubscriptionWindowStatus.UPCOMING
          ? 'subscription has not started yet'
          : 'subscription has already expired';
      throw new BadRequestException(
        `Extension applies only to an in-window active subscription (${reason})`,
      );
    }

    const previousEndsAt = existing.endsAt;
    const newEndsAt = this.resolveGymPaymentPeriodStart(dto);
    if (newEndsAt < previousEndsAt) {
      throw new BadRequestException(
        'date must not be before the current subscription expiry',
      );
    }
    const extendDays =
      newEndsAt > previousEndsAt
        ? this.utcCalendarDaysBetween(previousEndsAt, newEndsAt)
        : 0;

    const amountCents = dto.amount ?? 0;
    const fees = this.resolveExtendPlanFees(dto);
    const sellingPriceCents = this.resolveRequestSellingPrice(dto);
    if (
      extendDays < 1 &&
      fees <= 0 &&
      amountCents <= 0 &&
      sellingPriceCents == null
    ) {
      throw new BadRequestException(
        'Provide a later expiry date, `selling_price`, extension fee (`fees` / `addition_fee`), or payment `amount`',
      );
    }
    let nextStatus = initialStatusFromWindow(
      existing.startsAt,
      newEndsAt,
      now,
    );
    if (
      existing.status === MemberSubscriptionStatus.FROZEN &&
      existing.freezeEndsAt &&
      existing.freezeEndsAt > now
    ) {
      nextStatus = MemberSubscriptionStatus.FROZEN;
    }

    const memberName =
      member.user.fullName?.trim() ||
      member.user.username?.trim() ||
      member.user.phone?.trim() ||
      'Member';
    const memberUserId = await this.getMemberUserId(member.gymId, dto.member_id);

    const method =
      dto.payment_mode === 'cash'
        ? PaymentMethod.CASH
        : dto.payment_mode === 'card'
          ? PaymentMethod.CARD
          : dto.payment_mode === 'upi'
            ? PaymentMethod.UPI
            : undefined;

    const result = await this.prisma.$transaction(async (tx) => {
      let paymentId: string | undefined;

      if (amountCents > 0 && method) {
        const row = await tx.payment.create({
          data: {
            gymId: member.gymId,
            userId: actorUserId,
            amountCents,
            currency: existing.currency?.trim() || 'INR',
            status: PaymentStatus.COMPLETED,
            method,
            memberUserId,
            memberSubscriptionId: existing.id,
            completedAt: now,
            description: 'Plan extension payment',
          },
          select: { id: true },
        });
        paymentId = row.id;
      }

      const priceUpdate: Prisma.MemberSubscriptionUpdateInput = {};
      if (sellingPriceCents != null) {
        priceUpdate.priceCents = sellingPriceCents;
      }
      if (fees > 0) {
        priceUpdate.priceCents =
          sellingPriceCents != null
            ? sellingPriceCents + fees
            : { increment: fees };
      }

      const updated = await tx.memberSubscription.update({
        where: { id: existing.id },
        data: {
          ...(extendDays > 0 ? { endsAt: newEndsAt } : {}),
          status: nextStatus,
          ...priceUpdate,
          ...(amountCents > 0 ? { paidCents: { increment: amountCents } } : {}),
        },
        select: { id: true, priceCents: true, paidCents: true, endsAt: true },
      });

      await tx.$executeRaw(Prisma.sql`
        INSERT INTO subscription_history_events
          (id, subscription_id, gym_id, event_type, actor_user_id, payload_json)
        VALUES
          (${randomUUID()}, ${existing.id}, ${member.gymId}, 'EXTEND', ${actorUserId},
           ${JSON.stringify({
        extend_days: extendDays,
        fees,
        addition_fee: fees,
        amount: amountCents > 0 ? amountCents : null,
        selling_price: sellingPriceCents,
        previous_price_cents: existing.priceCents,
        previous_ends_at: previousEndsAt.toISOString(),
        new_ends_at: (extendDays > 0 ? newEndsAt : previousEndsAt).toISOString(),
        gym_plan_id: dto.gym_plan_id,
      })}::jsonb)
      `);

      await this.syncMembershipEndsAt(tx, dto.member_id);

      return { paymentId, updated };
    });

    this.events.emit(NOTIFICATION_EVENTS.EXTEND_PLAN, {
      gymId: member.gymId,
      gymUserId: dto.member_id,
      memberName,
      memberSubscriptionId: existing.id,
      actorUserId,
    });

    if (result.paymentId) {
      void this.whatsapp.enqueuePaymentConfirmation(
        member.gymId,
        memberUserId,
        result.paymentId,
      );
    }

    const priceCents = result.updated.priceCents;
    const paidCents = result.updated.paidCents;
    const amountPending = Math.max(0, priceCents - paidCents);
    const catalogPlanPrice = await this.resolveGymPlanSellingPriceCents(
      member.gymId,
      dto.gym_plan_id?.trim() || existing.gymPlanId,
    );
    const appliedSellingPrice =
      sellingPriceCents ?? priceCents;

    return {
      type: 'extend_plan' as const,
      memberSubscriptionId: existing.id,
      extend_days: extendDays,
      fees,
      addition_fee: fees,
      extension_fee_added: fees,
      previous_price_cents: existing.priceCents,
      price_cents: priceCents,
      selling_price: appliedSellingPrice,
      catalog_plan_price: catalogPlanPrice,
      paid_cents: paidCents,
      amount_pending: amountPending,
      previous_ends_at: previousEndsAt.toISOString(),
      new_ends_at: result.updated.endsAt.toISOString(),
      ...(result.paymentId ? { payment_id: result.paymentId } : {}),
    };
  }

  private resolveExtendPlanFees(
    dto: Pick<
      ReceiveGymPaymentDto,
      'fees' | 'addition_fee' | 'additional_fee'
    >,
  ): number {
    return dto.fees ?? dto.addition_fee ?? dto.additional_fee ?? 0;
  }

  /** Custom selling price from mobile (`selling_price` request field). */
  private resolveRequestSellingPrice(
    dto: Pick<ReceiveGymPaymentDto, 'selling_price'>,
  ): number | null {
    if (dto.selling_price == null) {
      return null;
    }
    const value = Number(dto.selling_price);
    if (!Number.isFinite(value) || value < 0) {
      return null;
    }
    return Math.round(value);
  }

  /** Gym catalog listed price (`GymPlan.priceCents`) — not subscription total. */
  private async resolveGymPlanSellingPriceCents(
    gymId: string,
    gymPlanId: string | null | undefined,
  ): Promise<number | null> {
    const id = gymPlanId?.trim();
    if (!id) {
      return null;
    }
    const row = await this.prisma.gymPlan.findFirst({
      where: { id, gymId },
      select: { priceCents: true },
    });
    return row?.priceCents ?? null;
  }

  private utcCalendarDaysBetween(from: Date, to: Date): number {
    const fromDay = startOfUtcDay(from);
    const toDay = startOfUtcDay(to);
    return Math.round((toDay.getTime() - fromDay.getTime()) / 86400000);
  }

  /**
   * `POST /payments` with `type: "receive_payment"`: record a completed `Payment` on an existing
   * `MemberSubscription`. `startsAt` from optional `date` (same rules as extend_plan); `endsAt` =
   * `startsAt` + `gym_plan_id`’s `durationDays`; `status` from the date window; `paidCents` incremented by `amount`.
   */
  async receivePaymentOnExistingSubscription(
    actorUserId: string,
    dto: ReceiveGymPaymentDto,
  ) {
    if (dto.type !== 'receive_payment') {
      throw new BadRequestException('Expected type receive_payment');
    }
    const memberSubscriptionId = dto.member_subscription_id!;
    const gymPlanId = dto.gym_plan_id;

    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);

    const plan = await this.prisma.gymPlan.findFirst({
      where: { id: gymPlanId, gymId: member.gymId },
      select: {
        id: true,
        durationDays: true,
        priceCents: true,
        currency: true,
        isActive: true,
      },
    });
    if (!plan) {
      throw new NotFoundException('Gym plan not found for this gym');
    }
    if (!plan.isActive) {
      throw new BadRequestException('Cannot use an inactive gym plan for receive_payment');
    }

    const existing = await this.prisma.memberSubscription.findFirst({
      where: { id: memberSubscriptionId, gymUserId: dto.member_id },
      select: { id: true, currency: true },
    });
    if (!existing) {
      throw new NotFoundException(
        'Member subscription not found for this member',
      );
    }

    const startsAt = this.resolveGymPaymentPeriodStart(dto);
    const endsAt = addUtcDays(startsAt, plan.durationDays);
    if (!(endsAt > startsAt)) {
      throw new BadRequestException(
        'Gym plan durationDays must be at least 1 for receive_payment',
      );
    }
    const now = new Date();
    const status = initialStatusFromWindow(startsAt, endsAt, now);

    const method =
      dto.payment_mode === 'cash'
        ? PaymentMethod.CASH
        : dto.payment_mode === 'card'
          ? PaymentMethod.CARD
          : PaymentMethod.UPI;

    const memberUserId = await this.getMemberUserId(member.gymId, dto.member_id);
    const paymentStatus = PaymentStatus.COMPLETED;

    const result = await this.prisma.$transaction(async (tx) => {
      const row = await tx.payment.create({
        data: {
          gymId: member.gymId,
          userId: actorUserId,
          amountCents: dto.amount!,
          currency: existing.currency?.trim() || 'INR',
          status: paymentStatus,
          method,
          memberUserId,
          memberSubscriptionId: existing.id,
          completedAt: new Date(),
        },
        select: { id: true },
      });

      await tx.memberSubscription.update({
        where: { id: existing.id },
        data: {
          startsAt,
          endsAt,
          status,
          paidCents: { increment: dto.amount },
        },
      });

      await this.syncMembershipEndsAt(tx, dto.member_id);

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

    void this.whatsapp.enqueuePaymentConfirmation(
      member.gymId,
      memberUserId,
      result.id,
    );
    this.events.emit(NOTIFICATION_EVENTS.PAYMENT_RECEIVED, {
      gymId: member.gymId,
      gymUserId: dto.member_id,
      paymentId: result.id,
      amountCents: result.amountCents,
      currency: result.currency,
      memberSubscriptionId: result.memberSubscriptionId,
      actorUserId: actorUserId,
    });

    return result;
  }

  /**
   * Assign a gym catalog plan to a member: new `MemberSubscription` with `paidCents: 0`.
   * Period rules match `POST /payments` (`date` → `startsAt`, plan `durationDays` → `endsAt`).
   */
  async assignGymPlanToMember(
    actorUserId: string,
    dto: Pick<ReceiveGymPaymentDto, 'member_id' | 'gym_plan_id' | 'date'>,
  ): Promise<{ success: boolean; subscription_id: string, message?: string }> {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }

    await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);

    const plan = await this.prisma.gymPlan.findFirst({
      where: { id: dto.gym_plan_id, gymId: member.gymId },
      select: {
        id: true,
        durationDays: true,
        priceCents: true,
        currency: true,
        isActive: true,
      },
    });
    if (!plan) {
      throw new NotFoundException('Gym plan not found for this gym');
    }
    const periodStart = this.resolveGymPaymentPeriodStart(dto);
    const subscriptionId = await this.createMemberSubscription(
      actorUserId,
      member.gymId,
      member.id,
      {
        gymPlanId: plan.id,
        startsAt: periodStart.toISOString(),
        endsAt: addUtcDays(periodStart, plan.durationDays).toISOString(),
        priceCents: plan.priceCents,
        currency: plan.currency,
      },
    );

    return { success: true as const, subscription_id: subscriptionId };
  }

  /**
   * Records multiple member payments in order (each in its own DB transaction).
   * Returns `{ payments, count }`; if one item throws, earlier payments are already committed.
   */
  async receivePaymentsByMemberBatch(
    actorUserId: string,
    items: ReceiveGymPaymentDto[],
  ) {
    const payments = [];
    for (const item of items) {
      if (item.type === 'receive_payment') {
        payments.push(
          await this.receivePaymentOnExistingSubscription(actorUserId, item),
        );
      } else {
        payments.push(await this.receivePaymentByMember(actorUserId, item));
      }
    }
    return { payments, count: payments.length };
  }

  async createTrainerSalaryPayment(
    actorUserId: string,
    dto: CreateTrainerSalaryPaymentDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, dto.gymId);
    await this.assertTrainerAtGym(dto.gymId, dto.trainer_id);

    const trainer = await this.prisma.gymUser.findUnique({
      where: { id: dto.trainer_id },
      select: {
        role: true,
        user: { select: { fullName: true, username: true, phone: true } },
      },
    });
    const trainerName =
      trainer?.user.fullName?.trim() ||
      trainer?.user.username?.trim() ||
      trainer?.user.phone?.trim() ||
      'Team member';
    const payeeRole: 'TRAINER' | 'STAFF' =
      trainer?.role === GymRole.STAFF ? 'STAFF' : 'TRAINER';

    const amountCents = dto.amount;
    const method = paymentMethodFromApi(dto.payment_mode);
    const anchorDay = dto.date?.trim()
      ? this.parseYmdDate(dto.date, 'date')
      : startOfUtcDay(new Date());
    const year = anchorDay.getUTCFullYear();
    const month = anchorDay.getUTCMonth() + 1;
    const { start: periodStart, endExclusive } = monthUtcRange(year, month);
    const periodEnd = new Date(endExclusive.getTime() - 86_400_000);
    const paidAt = new Date();

    const row = await this.prisma.trainerSalaryPayment.create({
      data: {
        gymId: dto.gymId,
        gymUserId: dto.trainer_id,
        amountCents,
        currency: 'INR',
        method,
        periodStart,
        periodEnd,
        paidAt,
      },
      select: { id: true },
    });

    if (row.id) {
      await this.prisma.expense.create({
        data: {
          gymId: dto.gymId,
          amountCents: amountCents,
          currency: 'INR',
          category: 'SALARY',
          description: 'Trainer Salary Payment',
          occurredOn: paidAt,
          method,
          gstPercent: new Prisma.Decimal(0),
          trainerGymUserId: dto.trainer_id,
          recordedByUserId: actorUserId,
        },
        select: { id: true },
      });
    }

    const paidInMonth = await this.prisma.trainerSalaryPayment.aggregate({
      where: {
        gymId: dto.gymId,
        gymUserId: dto.trainer_id,
        OR: [
          { paidAt: { gte: periodStart, lt: endExclusive } },
          { paidAt: null, createdAt: { gte: periodStart, lt: endExclusive } },
        ],
      },
      _sum: { amountCents: true },
    });

    const salaryProfile = await this.prisma.trainerProfile.findUnique({
      where: { gymUserId: dto.trainer_id },
      select: { salaryCents: true },
    });
    const monthlySalary = salaryProfile?.salaryCents ?? 0;
    const paidAmountThisMonth = paidInMonth._sum.amountCents ?? 0;

    this.events.emit(NOTIFICATION_EVENTS.TRAINER_SALARY_PAID, {
      gymId: dto.gymId,
      trainerGymUserId: dto.trainer_id,
      payeeRole,
      trainerName,
      salaryPaymentId: row.id,
      amountCents,
      currency: 'INR',
      actorUserId,
    });

    return {
      success: true as const,
      payment_id: row.id,
      monthly_salary: monthlySalary,
      paid_amount_this_month: paidAmountThisMonth,
      pending_amount: Math.max(0, monthlySalary - paidAmountThisMonth),
    };
  }

  async getTrainerSalaryHistory(
    actorUserId: string,
    query: TrainerSalaryHistoryQueryDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, query.gymId);
    await this.assertTrainerAtGym(query.gymId, query.trainer_id);

    const anchorDay = query.date?.trim()
      ? this.parseYmdDate(query.date, 'date')
      : startOfUtcDay(new Date());
    const year = anchorDay.getUTCFullYear();
    const month = anchorDay.getUTCMonth() + 1;
    const { start, endExclusive } = monthUtcRange(year, month);

    const profile = await this.prisma.trainerProfile.findUnique({
      where: { gymUserId: query.trainer_id },
      select: { salaryCents: true },
    });
    const monthlySalary = profile?.salaryCents ?? 0;

    const paidInMonth = await this.prisma.trainerSalaryPayment.aggregate({
      where: {
        gymId: query.gymId,
        gymUserId: query.trainer_id,
        OR: [
          { paidAt: { gte: start, lt: endExclusive } },
          { paidAt: null, createdAt: { gte: start, lt: endExclusive } },
        ],
      },
      _sum: { amountCents: true },
    });
    const paidAmount = paidInMonth._sum.amountCents ?? 0;

    const historyRows = await this.prisma.trainerSalaryPayment.findMany({
      where: { gymId: query.gymId, gymUserId: query.trainer_id },
      orderBy: [{ paidAt: 'desc' }, { createdAt: 'desc' }],
      take: 50,
      select: {
        id: true,
        amountCents: true,
        method: true,
        paidAt: true,
        createdAt: true,
      },
    });

    return {
      monthly_salary: monthlySalary,
      paid_amount: paidAmount,
      pending_amount: Math.max(0, monthlySalary - paidAmount),
      payment_history: historyRows.map((row) => ({
        id: row.id,
        amount: row.amountCents,
        date: (row.paidAt ?? row.createdAt).toISOString().slice(0, 10),
        mode: paymentMethodToApi(row.method),
      })),
    };
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

    // await this.permissions.assertOwnerOrPermission(
    //   actorUserId,
    //   member.gymId,
    //   PERMISSION_CODES.MEMBERS,
    // );

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

  /**
   * Aligns `MemberSubscription.status` with the date window (except CANCELED; FROZEN kept until period ends),
   * and sets `isCurrentSubscription` on at most one in-window row (latest `endsAt`, tie-break `startsAt`).
   */
  private async reconcileMemberSubscriptionWindows(
    db: Tx | PrismaService,
    gymUserId: string,
    now: Date,
  ): Promise<void> {
    const subs = await db.memberSubscription.findMany({
      where: { gymUserId },
      select: {
        id: true,
        startsAt: true,
        endsAt: true,
        status: true,
        isCurrentSubscription: true,
      },
    });
    if (!subs.length) {
      return;
    }

    const nonCanceled = subs.filter(
      (s) => s.status !== MemberSubscriptionStatus.CANCELED,
    );
    const t = now.getTime();
    const inWindow = nonCanceled.filter(
      (s) => t >= s.startsAt.getTime() && t <= s.endsAt.getTime(),
    );

    let currentId: string | null = null;
    if (inWindow.length) {
      currentId = inWindow.reduce((a, b) => {
        if (b.endsAt.getTime() > a.endsAt.getTime()) {
          return b;
        }
        if (b.endsAt.getTime() < a.endsAt.getTime()) {
          return a;
        }
        return b.startsAt.getTime() >= a.startsAt.getTime() ? b : a;
      }).id;
    }

    await Promise.all(
      subs.map(async (s) => {
        if (s.status === MemberSubscriptionStatus.CANCELED) {
          if (!s.isCurrentSubscription) {
            return;
          }
          await db.memberSubscription.update({
            where: { id: s.id },
            data: { isCurrentSubscription: false },
          });
          return;
        }

        let nextStatus: MemberSubscriptionStatus | undefined;
        if (s.status === MemberSubscriptionStatus.FROZEN) {
          if (s.endsAt <= now) {
            nextStatus = MemberSubscriptionStatus.ENDED;
          }
        } else {
          nextStatus = initialStatusFromWindow(s.startsAt, s.endsAt, now);
        }

        const isCurrent = s.id === currentId;
        const data: Prisma.MemberSubscriptionUpdateInput = {};
        if (nextStatus !== undefined && nextStatus !== s.status) {
          data.status = nextStatus;
        }
        if (isCurrent !== s.isCurrentSubscription) {
          data.isCurrentSubscription = isCurrent;
        }
        if (Object.keys(data).length > 0) {
          await db.memberSubscription.update({
            where: { id: s.id },
            data,
          });
        }
      }),
    );
  }

  async syncMembershipEndsAt(db: Tx | PrismaService, gymUserId: string) {
    const now = new Date();
    await this.reconcileMemberSubscriptionWindows(db, gymUserId, now);

    // Use the latest ACTIVE or FROZEN subscription — member is currently enrolled
    const activeSub = await db.memberSubscription.findFirst({
      where: {
        gymUserId,
        status: {
          in: [
            MemberSubscriptionStatus.ACTIVE,
            MemberSubscriptionStatus.FROZEN,
          ],
        },
      },
      orderBy: { endsAt: 'desc' },
      select: { endsAt: true },
    });
    if (activeSub) {
      await db.gymUser.update({
        where: { id: gymUserId },
        data: { membershipEndsAt: activeSub.endsAt, isActive: true },
      });
      return;
    }

    // No active subscription — use the latest ENDED/CANCELED sub's endsAt
    // so the member shows as "expired" with a past date.
    // SCHEDULED subs are ignored: membership hasn't started yet.
    const latestEnded = await db.memberSubscription.findFirst({
      where: {
        gymUserId,
        status: {
          in: [
            MemberSubscriptionStatus.ENDED,
            MemberSubscriptionStatus.CANCELED,
          ],
        },
      },
      orderBy: { endsAt: 'desc' },
      select: { endsAt: true },
    });
    await db.gymUser.update({
      where: { id: gymUserId },
      data: { membershipEndsAt: latestEnded?.endsAt ?? null },
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

  /**
   * `POST /payments` **extend_plan**: creates a **new** `MemberSubscription`
   * (`startsAt` + `GymPlan.durationDays` → `endsAt`). No `Payment` row.
   */
  private async createMemberSubscriptionForGymPlanPayment(
    gymUserId: string,
    plan: {
      id: string;
      durationDays: number;
      priceCents: number;
      currency: string;
      isActive: boolean;
    },
    startsAt: Date,
    priceCentsOverride?: number,
  ): Promise<string> {
    if (!plan.isActive) {
      throw new BadRequestException(
        'Cannot record payment against an inactive gym plan',
      );
    }
    const clock = new Date();
    const endsAt = addUtcDays(startsAt, plan.durationDays);
    const status = initialStatusFromWindow(startsAt, endsAt, clock);
    const created = await this.prisma.memberSubscription.create({
      data: {
        gymUserId,
        planId: null,
        gymPlanId: plan.id,
        startsAt,
        endsAt,
        status,
        priceCents: priceCentsOverride ?? plan.priceCents,
        paidCents: 0,
        currency: plan.currency,
      },
      select: { id: true },
    });
    await this.syncMembershipEndsAt(this.prisma, gymUserId);
    return created.id;
  }

  /** Anchor for new subscription window from `ReceiveGymPaymentDto.date` / `start_date`. */
  private resolveGymPaymentPeriodStart(
    dto: { date?: string; start_date?: string },
  ): Date {
    const raw = dto.date?.trim() || dto.start_date?.trim();
    if (!raw) {
      return startOfUtcDay(new Date());
    }
    const ymd = /^(\d{4})-(\d{2})-(\d{2})$/.exec(raw);
    if (ymd) {
      return new Date(
        Date.UTC(+ymd[1], +ymd[2] - 1, +ymd[3], 0, 0, 0, 0),
      );
    }
    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
      throw new BadRequestException(
        'date must be YYYY-MM-DD or a valid ISO date-time string',
      );
    }
    return parsed;
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
      priceCents: Math.max(0, (dto.price - discount)),
      currency: 'INR',
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

  private async assertTrainerAtGym(gymId: string, trainerGymUserId: string) {
    const trainer = await this.prisma.gymUser.findFirst({
      where: {
        id: trainerGymUserId,
        gymId,
        role: { in: [GymRole.TRAINER, GymRole.STAFF] },
      },
      select: { id: true },
    });
    if (!trainer) {
      throw new NotFoundException(
        'Trainer or staff member not found at this gym',
      );
    }
  }

  /**
   * After incrementing paidCents: if full period(s) are covered, extend endsAt from
   * max(now, current endsAt) and roll paidCents to the remainder (payment renewal).
   */
  private applyPaidPeriodsToSubscription(
    sub: {
      paidCents: number;
      priceCents: number;
      endsAt: Date;
      startsAt: Date;
      status: MemberSubscriptionStatus;
      gymPlanId: string | null;
      planId: string | null;
      gymPlan: { durationDays: number } | null;
      plan: { interval: string } | null;
    },
    now: Date,
  ):
    | { endsAt: Date; paidCents: number; status: MemberSubscriptionStatus }
    | null {
    if (sub.priceCents <= 0) {
      return null;
    }
    const periods = Math.floor(sub.paidCents / sub.priceCents);
    if (periods < 1) {
      return null;
    }
    if (
      sub.status === MemberSubscriptionStatus.CANCELED ||
      sub.status === MemberSubscriptionStatus.FROZEN
    ) {
      return null;
    }

    const remainder = sub.paidCents % sub.priceCents;
    let newEndsAt = new Date(
      Math.max(now.getTime(), sub.endsAt.getTime()),
    );

    if (sub.gymPlanId && sub.gymPlan) {
      const d = sub.gymPlan.durationDays;
      for (let i = 0; i < periods; i++) {
        newEndsAt = addUtcDays(newEndsAt, d);
      }
    } else if (sub.planId && sub.plan) {
      for (let i = 0; i < periods; i++) {
        newEndsAt = applyPlanInterval(newEndsAt, sub.plan.interval);
      }
    } else {
      return null;
    }

    return {
      endsAt: newEndsAt,
      paidCents: remainder,
      status: initialStatusFromWindow(sub.startsAt, newEndsAt, now),
    };
  }

  private parseYmdDate(value: string, fieldName: string): Date {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
    if (!m) {
      throw new BadRequestException(`${fieldName} must be YYYY-MM-DD`);
    }
    return new Date(Date.UTC(+m[1], +m[2] - 1, +m[3], 0, 0, 0, 0));
  }
}
