import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { EventEmitter2 } from '@nestjs/event-emitter';
import {
  AuditAction,
  AuditEntityType,
  GymRole,
  MemberSubscriptionStatus,
  PaymentMethod,
  PaymentStatus,
  Prisma,
  UserStatus,
} from '@prisma/client';
import {
  classifyAttendancePunctuality,
  formatCheckInRelativeLine,
  monthShortLabel,
  punctualityDisplayLabel,
} from '../../common/utils/attendance-punctuality';
import { normalizeEmailForStorage } from '../../common/utils/normalize-email';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { DietService } from '../diet/diet.service';
import { WhatsAppAutomationService } from '../messaging/whatsapp-automation.service';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import type { AddMemberSubscriptionDto } from './dto/add-subscription.dto';
import type { AddDietEntryDto } from './dto/add-diet-entry.dto';
import type { AddWorkoutDto } from './dto/add-workout.dto';
import type { CreateMemberDto } from './dto/create-member.dto';
import type { ReceivePaymentDto } from './dto/receive-payment.dto';
import type { UpdateMemberDto } from './dto/update-member.dto';
import { computeMemberLifecycleStatus } from './member-lifecycle';
import {
  getMemberSubscriptionUiBucket,
  SubscriptionWindowStatus,
} from '../subscriptions/subscription-lifecycle';
import { MemberListFilter, memberListFilterWhere } from './member-list-filter';
import { AuditService } from '../audit/audit.service';
import { NOTIFICATION_EVENTS } from '../notifications/domain-events';
import {
  computeMemberWellness,
  type ActivityLevelTier,
  type GenderForBmr,
  type MemberWellnessResult,
} from '../onboarding/member-wellness.util';

type MemberSubscriptionWithPlanNames = Prisma.MemberSubscriptionGetPayload<{
  include: {
    plan: { select: { name: true } };
    gymPlan: { select: { name: true; priceCents: true; id: true } };
  };
}>;

@Injectable()
export class MembersService {
  private readonly logger = new Logger(MembersService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly subscriptions: SubscriptionsService,
    private readonly diet: DietService,
    private readonly whatsapp: WhatsAppAutomationService,
    private readonly events: EventEmitter2,
    private readonly audit: AuditService,
  ) { }

  private static macroGramsToKcal(proteinG: number, carbsG: number, fatG: number) {
    return {
      proteinKcal: Math.round(proteinG * 4),
      carbsKcal: Math.round(carbsG * 4),
      fatKcal: Math.round(fatG * 9),
    };
  }

  private buildDashboardNutrition(
    consumed: {
      caloriesConsumed: number;
      proteinG: number;
      carbsG: number;
      fatG: number;
    },
    calorieGoal: number,
  ) {
    const hasConsumption = consumed.caloriesConsumed > 0;
    const macroKcal = hasConsumption
      ? MembersService.macroGramsToKcal(
        consumed.proteinG,
        consumed.carbsG,
        consumed.fatG,
      )
      : { proteinKcal: null, carbsKcal: null, fatKcal: null };
    return {
      caloriesConsumed: consumed.caloriesConsumed,
      calorieGoal,
      ...macroKcal,
      fatPending: !hasConsumption,
    };
  }

  /** Shared include for member detail + profile (`MemberSubscription` rows loaded separately). */
  private memberDetailInclude(): Prisma.GymUserInclude {
    return {
      user: {
        select: {
          id: true,
          phone: true,
          email: true,
          fullName: true,
          ageYears: true,
          address: true,
          aadhaarNumber: true,
          heightCm: true,
          weightKg: true,
          gender: true,
          activityLevel: true,
          fitnessGoal: true,
          wellness: true,
          createdAt: true,
          avatarUrl: true,
        },
      },
    };
  }

  private parseWellnessFromUserJson(
    value: Prisma.JsonValue | null | undefined,
  ): MemberWellnessResult | null {
    if (value == null || typeof value !== 'object' || Array.isArray(value)) {
      return null;
    }
    const o = value as Record<string, unknown>;
    if (typeof o.bmi !== 'number' || typeof o.bmiCategory !== 'string') {
      return null;
    }
    const mc = o.maintenanceCalories;
    return {
      bmi: o.bmi,
      bmiCategory: o.bmiCategory as MemberWellnessResult['bmiCategory'],
      maintenanceCalories:
        mc === null || mc === undefined
          ? null
          : typeof mc === 'number'
            ? mc
            : null,
    };
  }

  private mapGenderForWellness(
    raw: string | null | undefined,
  ): GenderForBmr | undefined {
    if (!raw) return undefined;
    const u = raw.trim().toUpperCase();
    if (u === 'MALE' || u === 'M' || u === 'MAN') return 'MALE';
    if (u === 'FEMALE' || u === 'F' || u === 'WOMAN') return 'FEMALE';
    if (u === 'OTHER') return 'OTHER';
    return undefined;
  }

  private mapActivityForWellness(
    raw: string | null | undefined,
  ): ActivityLevelTier | undefined {
    if (!raw) return undefined;
    const u = raw.trim().toUpperCase();
    if (u === 'LOW' || u === 'MODERATE' || u === 'HIGH')
      return u as ActivityLevelTier;
    return undefined;
  }

  private computeWellnessSnapshotForMember(
    now: Date,
    user: {
      heightCm: Prisma.Decimal | null;
      weightKg: Prisma.Decimal | null;
      ageYears: number | null;
      gender: string | null;
      activityLevel: string | null;
      wellness?: Prisma.JsonValue | null;
    },
    gymUserGender?: string | null,
    dateOfBirth?: Date | null,
  ): MemberWellnessResult | null {
    const heightCm =
      user.heightCm != null ? Number(user.heightCm) : null;
    const weightKg =
      user.weightKg != null ? Number(user.weightKg) : null;
    if (
      heightCm == null ||
      weightKg == null ||
      !Number.isFinite(heightCm) ||
      !Number.isFinite(weightKg)
    ) {
      return this.parseWellnessFromUserJson(user.wellness ?? null);
    }
    const ageYears = this.computeMemberAgeYears(
      now,
      user.ageYears,
      dateOfBirth ?? null,
    );
    const gender =
      this.mapGenderForWellness(user.gender) ??
      this.mapGenderForWellness(gymUserGender);
    const computed = computeMemberWellness({
      heightCm,
      weightKg,
      ageYears: ageYears ?? undefined,
      gender,
      activityLevel: this.mapActivityForWellness(user.activityLevel),
    });
    const stored = this.parseWellnessFromUserJson(user.wellness ?? null);
    return {
      ...computed,
      maintenanceCalories: stored?.maintenanceCalories ?? null,
    };
  }

  private memberProfileWellnessFields(
    now: Date,
    user: {
      heightCm: Prisma.Decimal | null;
      weightKg: Prisma.Decimal | null;
      ageYears: number | null;
      gender: string | null;
      activityLevel: string | null;
      fitnessGoal: string | null;
      wellness?: Prisma.JsonValue | null;
    },
    gymUserGender?: string | null,
    dateOfBirth?: Date | null,
  ) {
    const wellness = this.computeWellnessSnapshotForMember(
      now,
      user,
      gymUserGender,
      dateOfBirth,
    );
    return {
      heightCm: user.heightCm != null ? Number(user.heightCm) : null,
      weightKg: user.weightKg != null ? Number(user.weightKg) : null,
      activityLevel: user.activityLevel,
      fitnessGoal: user.fitnessGoal,
      wellness,
      maintenanceCalories: wellness?.maintenanceCalories ?? null,
      /** Legacy mobile typo — same value as `maintenanceCalories`. */
      maintenanceCaleries: wellness?.maintenanceCalories ?? null,
    };
  }

  private parseOptionalMetricNumber(
    primary: number | undefined,
    alias: unknown,
  ): number | undefined {
    const raw = primary ?? alias;
    if (raw === undefined || raw === null || raw === '') {
      return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
  }

  private normalizeActivityLevelField(
    primary: string | undefined,
    alias: string | undefined,
  ): string | null | undefined {
    const raw = primary ?? alias;
    if (raw === undefined) {
      return undefined;
    }
    if (raw == null || String(raw).trim() === '') {
      return null;
    }
    const u = String(raw).trim().toUpperCase();
    if (u === 'LOW' || u === 'MODERATE' || u === 'HIGH') {
      return u;
    }
    throw new BadRequestException(
      'activityLevel must be LOW, MODERATE, or HIGH',
    );
  }

  private applyMemberMetricFieldsFromDto(
    dto: UpdateMemberDto,
    userData: Prisma.UserUpdateInput,
  ): void {
    const heightCm = this.parseOptionalMetricNumber(
      dto.heightCm,
      dto.height_cm,
    );
    if (heightCm != null) {
      userData.heightCm = new Prisma.Decimal(heightCm);
    }
    const weightKg = this.parseOptionalMetricNumber(
      dto.weightKg,
      dto.weight_kg,
    );
    if (weightKg != null) {
      userData.weightKg = new Prisma.Decimal(weightKg);
    }
    const activityLevel = this.normalizeActivityLevelField(
      dto.activityLevel,
      dto.activity_level,
    );
    if (activityLevel !== undefined) {
      userData.activityLevel = activityLevel;
    }
  }

  private parseMaintenanceCaloriesFromDto(
    dto: UpdateMemberDto,
  ): number | null | undefined {
    if (
      dto.maintenanceCalories === undefined &&
      dto.maintenance_calories === undefined
    ) {
      return undefined;
    }
    const raw = dto.maintenanceCalories ?? dto.maintenance_calories;
    if (raw === null) {
      return null;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? Math.round(n) : undefined;
  }

  private async persistMaintenanceCaloriesWithoutMetricsTx(
    tx: Prisma.TransactionClient,
    userId: string,
    maintenanceCaloriesOverride?: number | null,
  ): Promise<void> {
    const existingUser = await tx.user.findUnique({
      where: { id: userId },
      select: { wellness: true },
    });
    const existingWellness = this.parseWellnessFromUserJson(
      existingUser?.wellness ?? null,
    );
    if (maintenanceCaloriesOverride !== undefined) {
      const wellness: MemberWellnessResult = existingWellness ?? {
        bmi: 0,
        bmiCategory: 'normal',
        maintenanceCalories: null,
      };
      wellness.maintenanceCalories = maintenanceCaloriesOverride;
      await tx.user.update({
        where: { id: userId },
        data: {
          wellness: wellness as unknown as Prisma.InputJsonValue,
        },
      });
      return;
    }
    if (existingWellness?.maintenanceCalories != null) {
      return;
    }
    await tx.user.update({
      where: { id: userId },
      data: { wellness: Prisma.DbNull },
    });
  }

  /** Recompute `User.wellness` BMI from height/weight; preserve user-set `maintenanceCalories`. */
  private async syncUserWellnessTx(
    tx: Prisma.TransactionClient,
    userId: string,
    gymUserId: string | null,
    maintenanceCaloriesOverride?: number | null,
  ): Promise<void> {
    const [user, gu] = await Promise.all([
      tx.user.findUnique({
        where: { id: userId },
        select: {
          heightCm: true,
          weightKg: true,
          ageYears: true,
          gender: true,
          activityLevel: true,
        },
      }),
      gymUserId
        ? tx.gymUser.findUnique({
          where: { id: gymUserId },
          select: { gender: true, dateOfBirth: true },
        })
        : Promise.resolve(null),
    ]);
    if (!user?.heightCm || !user.weightKg) {
      await this.persistMaintenanceCaloriesWithoutMetricsTx(
        tx,
        userId,
        maintenanceCaloriesOverride,
      );
      return;
    }
    const heightCm = Number(user.heightCm);
    const weightKg = Number(user.weightKg);
    if (!Number.isFinite(heightCm) || !Number.isFinite(weightKg)) {
      await this.persistMaintenanceCaloriesWithoutMetricsTx(
        tx,
        userId,
        maintenanceCaloriesOverride,
      );
      return;
    }
    const gender =
      this.mapGenderForWellness(user.gender) ??
      this.mapGenderForWellness(gu?.gender ?? undefined);
    const ageYears = this.computeMemberAgeYears(
      new Date(),
      user.ageYears,
      gu?.dateOfBirth ?? null,
    );
    const snapshot = computeMemberWellness({
      heightCm,
      weightKg,
      ageYears: ageYears ?? undefined,
      gender,
      activityLevel: this.mapActivityForWellness(user.activityLevel),
    });
    const existingUser = await tx.user.findUnique({
      where: { id: userId },
      select: { wellness: true },
    });
    const existingWellness = this.parseWellnessFromUserJson(
      existingUser?.wellness ?? null,
    );
    if (maintenanceCaloriesOverride !== undefined) {
      snapshot.maintenanceCalories = maintenanceCaloriesOverride;
    } else {
      snapshot.maintenanceCalories =
        existingWellness?.maintenanceCalories ?? null;
    }
    await tx.user.update({
      where: { id: userId },
      data: {
        wellness: snapshot as unknown as Prisma.InputJsonValue,
      },
    });
  }

  private fetchMemberSubscriptionsForDetail(
    gymUserId: string,
  ): Promise<MemberSubscriptionWithPlanNames[]> {
    return this.prisma.memberSubscription.findMany({
      where: { gymUserId },
      orderBy: [{ endsAt: 'desc' }, { startsAt: 'desc' }],
      include: {
        plan: { select: { name: true } },
        gymPlan: { select: { name: true, priceCents: true, id: true } },
      },
    });
  }

  private static paymentMethodDisplay(method: PaymentMethod | null): string {
    switch (method) {
      case PaymentMethod.UPI:
        return 'UPI';
      case PaymentMethod.CARD:
        return 'Card';
      case PaymentMethod.CASH:
      default:
        return 'Cash';
    }
  }

  /** Payments block for `GET /members/:id` (UTC calendar year + subscription balance owed). */
  private async buildMemberDetailPayments(
    gymId: string,
    gymUserRowId: string,
    memberUserId: string,
    now: Date,
  ): Promise<{
    paymentSummary: {
      paidThisYear: number;
      outstandingAmount: number;
      total_price_cents: number;
      total_paid_cents: number;
    };
    paymentHistory: Array<{
      gymPlanName: string;
      paymentMethod: string;
      date: string;
      receivedBy: string | null;
      amount: number;
    }>;
  }> {
    const y = now.getUTCFullYear();
    const yearStart = new Date(Date.UTC(y, 0, 1, 0, 0, 0, 0));
    const yearEnd = new Date(Date.UTC(y + 1, 0, 1, 0, 0, 0, 0));

    const [paidYearAgg, subs, historyRows] = await Promise.all([
      this.prisma.payment.aggregate({
        where: {
          gymId,
          memberUserId,
          status: PaymentStatus.COMPLETED,
          OR: [
            { completedAt: { gte: yearStart, lt: yearEnd } },
            {
              completedAt: null,
              createdAt: { gte: yearStart, lt: yearEnd },
            },
          ],
        },
        _sum: { amountCents: true },
      }),
      this.prisma.memberSubscription.findMany({
        where: {
          gymUserId: gymUserRowId,
          status: { not: MemberSubscriptionStatus.CANCELED },
        },
        select: { priceCents: true, paidCents: true },
      }),
      this.prisma.payment.findMany({
        where: {
          gymId,
          memberUserId,
          status: PaymentStatus.COMPLETED,
        },
        orderBy: [{ completedAt: 'desc' }, { createdAt: 'desc' }],
        take: 50,
        select: {
          amountCents: true,
          userId: true,
          method: true,
          completedAt: true,
          createdAt: true,
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
      ...new Set(historyRows.map((p) => p.userId).filter(Boolean)),
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

    const outstandingCents = subs.reduce(
      (s, x) => s + Math.max(0, x.priceCents - x.paidCents),
      0,
    );

    const totalPriceCents = subs.reduce((s, x) => s + x.priceCents, 0);
    const totalPaidCents = subs.reduce((s, x) => s + x.paidCents, 0);

    return {
      paymentSummary: {
        paidThisYear: paidYearAgg._sum.amountCents ?? 0,
        outstandingAmount: outstandingCents,
        total_price_cents: totalPriceCents,
        total_paid_cents: totalPaidCents,
      },
      paymentHistory: historyRows.map((p) => {
        const at = p.completedAt ?? p.createdAt;
        return {
          gymPlanName:
            p.memberSubscription?.gymPlan?.name ??
            p.memberSubscription?.plan?.name ??
            '',
          paymentMethod: MembersService.paymentMethodDisplay(p.method),
          date: at.toISOString().slice(0, 10),
          receivedBy:
            receivedByNameByUserId.get(p.userId) ?? null,
          amount: p.amountCents,
        };
      }),
    };
  }

  private static formatSubscriptionYmd(d: Date): string {
    return d.toISOString().slice(0, 10);
  }

  private async loadExtensionFeesBySubscriptionId(
    subscriptionIds: string[],
  ): Promise<Map<string, number>> {
    if (!subscriptionIds.length) {
      return new Map();
    }
    const rows = await this.prisma.subscriptionHistoryEvent.findMany({
      where: {
        subscriptionId: { in: subscriptionIds },
        eventType: 'EXTEND',
      },
      select: { subscriptionId: true, payloadJson: true },
    });
    const map = new Map<string, number>();
    for (const row of rows) {
      const payload = row.payloadJson as Record<string, unknown>;
      const fee = Number(
        payload.fees ?? payload.addition_fee ?? payload.additional_fee ?? 0,
      );
      if (!Number.isFinite(fee) || fee <= 0) {
        continue;
      }
      map.set(
        row.subscriptionId,
        (map.get(row.subscriptionId) ?? 0) + Math.round(fee),
      );
    }
    return map;
  }

  private static memberSubscriptionDetailCard(
    row: MemberSubscriptionWithPlanNames,
    now: Date,
    extensionFeesFromHistory = 0,
  ): {
    member_subscription_id: string;
    gym_plan_id: string | null;
    plan_name: string;
    start_date: string;
    expiry_date: string;
    remaining_days: number;
    catalog_plan_price: number | null;
    /** @deprecated Use `catalog_plan_price` — gym catalog base price only. */
    plan_price: number | null;
    price_cents: number;
    /** Custom / negotiated subscription price (`MemberSubscription.priceCents`). */
    selling_price: number;
    extension_fees_total: number;
    /** @deprecated Use `extension_fees_total`. */
    extension_fee_total: number;
    amount_paid: number;
    paid_cents: number;
    amount_pending: number;
  } {
    const catalogPlanPrice = row.gymPlan?.priceCents ?? null;
    const pendingCents = Math.max(0, row.priceCents - row.paidCents);
    const extensionFeesTotal = extensionFeesFromHistory;
    const bucket =
      getMemberSubscriptionUiBucket(row, now) ?? SubscriptionWindowStatus.EXPIRED;
    const remainingDays =
      bucket === SubscriptionWindowStatus.EXPIRED
        ? 0
        : Math.max(
          0,
          Math.ceil((row.endsAt.getTime() - now.getTime()) / 86_400_000),
        );
    return {
      member_subscription_id: row.id,
      gym_plan_id: row.gymPlanId,
      plan_name: row.gymPlan?.name ?? row.plan?.name ?? '',
      start_date: MembersService.formatSubscriptionYmd(row.startsAt),
      expiry_date: MembersService.formatSubscriptionYmd(row.endsAt),
      remaining_days: remainingDays,
      catalog_plan_price: catalogPlanPrice,
      plan_price: catalogPlanPrice,
      price_cents: row.priceCents,
      selling_price: row.priceCents,
      extension_fees_total: extensionFeesTotal,
      extension_fee_total: extensionFeesTotal,
      amount_paid: row.paidCents,
      paid_cents: row.paidCents,
      amount_pending: pendingCents,
    };
  }

  private static memberFreezeSubscriptionDetailCard(
    row: MemberSubscriptionWithPlanNames,
    now: Date,
    extensionFeesFromHistory = 0,
  ): ReturnType<typeof MembersService.memberSubscriptionDetailCard> & {
    freeze_start_date: string;
    freeze_end_date: string;
    duration_days: number;
  } {
    const base = MembersService.memberSubscriptionDetailCard(
      row,
      now,
      extensionFeesFromHistory,
    );
    const freezeStart = row.freezeStartedAt;
    const freezeEnd = row.freezeEndsAt;
    const durationDays =
      freezeStart && freezeEnd
        ? Math.max(
          1,
          Math.ceil(
            (freezeEnd.getTime() - freezeStart.getTime()) / 86_400_000,
          ),
        )
        : 0;
    return {
      ...base,
      freeze_start_date: freezeStart
        ? freezeStart.toISOString().slice(0, 10)
        : '',
      freeze_end_date: freezeEnd ? freezeEnd.toISOString().slice(0, 10) : '',
      duration_days: durationDays,
    };
  }

  private async buildSubscriptionSummary(
    memberSubscriptions: MemberSubscriptionWithPlanNames[],
    now: Date,
  ): Promise<{
    stats: {
      active_subscription: number;
      pending_payment: number;
      overdue: number;
    };
    current_subscriptions: Array<
      ReturnType<typeof MembersService.memberSubscriptionDetailCard>
    >;
    /** @deprecated Prefer `current_subscriptions` — latest in-window period (same as first entry). */
    current_subscription: ReturnType<
      typeof MembersService.memberSubscriptionDetailCard
    > | null;
    upcoming_subscriptions: Array<
      ReturnType<typeof MembersService.memberSubscriptionDetailCard>
    >;
    expired_subscriptions: Array<
      ReturnType<typeof MembersService.memberSubscriptionDetailCard>
    >;
    freeze_subscriptions: Array<
      ReturnType<typeof MembersService.memberFreezeSubscriptionDetailCard>
    >;
  }> {
    const extensionFeesBySubId = await this.loadExtensionFeesBySubscriptionId(
      memberSubscriptions.map((s) => s.id),
    );
    const card = (row: MemberSubscriptionWithPlanNames) =>
      MembersService.memberSubscriptionDetailCard(
        row,
        now,
        extensionFeesBySubId.get(row.id) ?? 0,
      );

    const rows = memberSubscriptions.filter(
      (s) => getMemberSubscriptionUiBucket(s, now) != null,
    );

    const outstandingCents = (s: MemberSubscriptionWithPlanNames) =>
      Math.max(0, s.priceCents - s.paidCents);

    const currentRows = rows.filter(
      (s) =>
        getMemberSubscriptionUiBucket(s, now) === SubscriptionWindowStatus.CURRENT,
    );

    const current_subscriptions = currentRows
      .sort((a, b) => {
        const endDiff = b.endsAt.getTime() - a.endsAt.getTime();
        if (endDiff !== 0) {
          return endDiff;
        }
        return b.startsAt.getTime() - a.startsAt.getTime();
      })
      .map((s) => card(s));

    const current_subscription = current_subscriptions[0] ?? null;

    const upcoming_subscriptions = rows
      .filter(
        (s) =>
          getMemberSubscriptionUiBucket(s, now) ===
          SubscriptionWindowStatus.UPCOMING,
      )
      .sort((a, b) => a.startsAt.getTime() - b.startsAt.getTime())
      .map((s) => card(s));

    const expired_subscriptions = rows
      .filter(
        (s) =>
          getMemberSubscriptionUiBucket(s, now) ===
          SubscriptionWindowStatus.EXPIRED,
      )
      .sort((a, b) => b.endsAt.getTime() - a.endsAt.getTime())
      .map((s) => card(s));

    const freeze_subscriptions = memberSubscriptions
      .filter(
        (s) =>
          s.status === MemberSubscriptionStatus.FROZEN && s.endsAt > now,
      )
      .sort(
        (a, b) =>
          (b.freezeStartedAt?.getTime() ?? 0) -
          (a.freezeStartedAt?.getTime() ?? 0),
      )
      .map((s) =>
        MembersService.memberFreezeSubscriptionDetailCard(
          s,
          now,
          extensionFeesBySubId.get(s.id) ?? 0,
        ),
      );

    const pending_payment = rows.filter((s) => outstandingCents(s) > 0).length;
    const overdue = rows.filter((s) => {
      const b = getMemberSubscriptionUiBucket(s, now);
      return (
        b === SubscriptionWindowStatus.EXPIRED && outstandingCents(s) > 0
      );
    }).length;

    return {
      stats: {
        active_subscription: currentRows.length,
        pending_payment,
        overdue,
      },
      current_subscriptions,
      current_subscription,
      upcoming_subscriptions,
      expired_subscriptions,
      freeze_subscriptions,
    };
  }

  private buildProfileSubscriptionBlock(
    subscription: Awaited<ReturnType<MembersService['buildSubscriptionSummary']>>,
  ) {
    return {
      stats: subscription.stats,
      current_subscriptions: subscription.current_subscriptions,
      current_subscription: subscription.current_subscription,
      upcoming_subscriptions: subscription.upcoming_subscriptions,
      expired_subscriptions: subscription.expired_subscriptions,
      past_subscriptions: subscription.expired_subscriptions,
      freeze_subscriptions: subscription.freeze_subscriptions,
    };
  }

  private async loadGymBasicForProfile(gymId: string) {
    const gym = await this.prisma.gym.findFirst({
      where: { id: gymId },
      select: {
        id: true,
        name: true,
        slug: true,
        address: true,
        latitude: true,
        longitude: true,
        timezone: true,
        gstin: true,
        logoUrl: true,
      },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    return {
      id: gym.id,
      name: gym.name,
      slug: gym.slug,
      address: gym.address,
      timezone: gym.timezone,
      gstin: gym.gstin,
      logo_url: gym.logoUrl,
      latitude: gym.latitude ? gym.latitude.toString() : null,
      longitude: gym.longitude ? gym.longitude.toString() : null,
    };
  }

  private memberListProfileFields(
    now: Date,
    r: {
      user: {
        fullName: string | null;
        ageYears: number | null;
        address: string | null;
        aadhaarNumber?: string | null;
      };
      notes: string | null;
      emergencyContactName: string | null;
      emergencyContactPhone: string | null;
      dateOfBirth: Date | null;
    },
  ) {
    const { first_name, last_name } = this.splitFullName(r.user.fullName);
    const parsed = this.parseStructuredMemberNotes(r.notes);
    const address =
      (r.user.address?.trim() ? r.user.address.trim() : null) ??
      parsed.address ??
      null;
    const aadhaar =
      (r.user.aadhaarNumber?.trim() ? r.user.aadhaarNumber.trim() : null) ??
      parsed.aadhaar_number ??
      null;
    return {
      first_name,
      last_name,
      address,
      dob: this.formatMemberListDob(r.dateOfBirth),
      aadhaar_number: aadhaar,
      emergency_name: r.emergencyContactName,
      emergency_contact_phone: r.emergencyContactPhone,
      notes: r.notes,
      age: this.computeMemberAgeYears(
        now,
        r.user.ageYears,
        r.dateOfBirth,
      ),
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
                address: {
                  contains: q.trim(),
                  mode: Prisma.QueryMode.insensitive,
                },
              },
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
          notes: true,
          emergencyContactName: true,
          emergencyContactPhone: true,
          dateOfBirth: true,
          user: {
            select: {
              id: true,
              fullName: true,
              phone: true,
              email: true,
              avatarUrl: true,
              ageYears: true,
              address: true,
              aadhaarNumber: true,
            },
          },
          memberSubscriptions: {
            orderBy: { endsAt: 'desc' },
            take: 1,
            select: {
              gymPlan: { select: { name: true } },
            },
          },
        },
      }),
    ]);

    const [active, inactive, expired, totalMembers] = await Promise.all([
      this.prisma.gymUser.count({
        where: {
          gymId: resolvedGymId,
          role: GymRole.MEMBER,
          isLead: false,
          isActive: true,
          membershipEndsAt: { not: null, gte: now },
        },
      }),
      this.prisma.gymUser.count({
        where: { gymId: resolvedGymId, role: GymRole.MEMBER, isActive: false },
      }),
      this.prisma.gymUser.count({
        where: {
          gymId: resolvedGymId,
          role: GymRole.MEMBER,
          isLead: false,
          isActive: true,
          OR: [
            { membershipEndsAt: null },
            { membershipEndsAt: { lt: now } },
          ],
        },
      }),
      this.prisma.gymUser.count({
        where: { gymId: resolvedGymId, role: GymRole.MEMBER },
      }),
    ]);

    const page = Math.floor(offset / Math.max(limit, 1)) + 1;
    const totalPages = Math.max(1, Math.ceil(total / Math.max(limit, 1)));
    const mapped = rows.map((r) => {
      const extras = this.memberListProfileFields(now, r);
      return {
        id: r.id,
        name: r.user.fullName ?? '',
        phone: r.user.phone,
        status: computeMemberLifecycleStatus(r, now),
        plan_name: r.memberSubscriptions[0]?.gymPlan?.name ?? '',
        expiry_date: r.membershipEndsAt
          ? MembersService.formatSubscriptionYmd(r.membershipEndsAt)
          : null,
        profile_image: r.user.avatarUrl ?? '',
        ...extras,
      };
    });

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
      items: rows.map((r) => {
        const extras = this.memberListProfileFields(now, r);
        return {
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
          ...extras,
        };
      }),
    };
  }

  async updateMemberProfile(
    actorUserId: string,
    gymId: string | undefined,
    memberId: string,
    dto: UpdateMemberDto,
  ) {
    const trimmedGymId = gymId?.trim();
    if (trimmedGymId) {
      await this.patchGymMember(actorUserId, trimmedGymId, memberId, dto);
      return this.getProfile(actorUserId, trimmedGymId, memberId);
    }
    return this.patchMemberProfileSelf(actorUserId, memberId, dto);
  }

  async deleteMember(actorUserId: string, memberId: string) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: memberId },
      select: { gymId: true },
    });
    if (!member) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);
    await this.prisma.gymUser.delete({ where: { id: memberId } });
    return { message: 'Member deleted successfully' };
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
      const existingLinks = await this.prisma.gymUser.findMany({
        where: { gymId, userId: existingUserByPhone.id },
        select: { id: true, role: true },
      });
      if (existingLinks.some((l) => l.role === GymRole.MEMBER)) {
        throw new ConflictException(
          'A member with this phone already exists at this gym',
        );
      }
      const nonMemberLink = existingLinks.find(
        (l) => l.role !== GymRole.MEMBER,
      );
      if (nonMemberLink) {
        const roleLabel =
          nonMemberLink.role === GymRole.TRAINER
            ? 'a trainer'
            : nonMemberLink.role === GymRole.STAFF
              ? 'staff'
              : nonMemberLink.role === GymRole.OWNER
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
            selectedOnboardingRole: GymRole.MEMBER,
            onboardingCompletedAt: new Date(),
            address: dto.address?.trim() ?? null,
            ...(emailNorm ? { email: emailNorm } : {}),
            ...(dto.avatarUrl?.trim()
              ? { avatarUrl: dto.avatarUrl.trim() }
              : {}),
            ...(dto.aadhaar_number
              ? { aadhaarNumber: dto.aadhaar_number }
              : {}),
          },
          update: {
            fullName,
            ...(emailNorm ? { email: emailNorm } : {}),
            ...(dto.avatarUrl !== undefined
              ? { avatarUrl: dto.avatarUrl?.trim() || null }
              : {}),
            ...(dto.address !== undefined
              ? { address: dto.address?.trim() || null }
              : {}),
            ...(dto.aadhaar_number !== undefined
              ? { aadhaarNumber: dto.aadhaar_number }
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
            data: { ...userUpdate, selectedOnboardingRole: GymRole.MEMBER, onboardingCompletedAt: new Date() },
          });
        }

        const hasSubscription = !!(dto.initialSubscription || membershipEndsAt);
        const gu = await tx.gymUser.create({
          data: {
            userId: user.id,
            gymId,
            role: GymRole.MEMBER,
            isLead: dto.isLead ?? false,
            isActive: dto.isLead || hasSubscription,
            membershipEndsAt,
            notes: dto.notes?.trim() ?? null,
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

        await this.syncUserWellnessTx(tx, user.id, gu.id);
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
    // const isgymUserId = await this.prisma.gymUser.findUnique({
    //   where: { userId: actorUserId },
    //   select: { id: true },
    // });

    // if (isgymUserId) {}

    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const [row, memberSubscriptions] = await Promise.all([
      this.prisma.gymUser.findFirst({
        where: {
          id: gymUserId,
          gymId,
          role: GymRole.MEMBER,
        },
        include: this.memberDetailInclude(),
      }),
      this.fetchMemberSubscriptionsForDetail(gymUserId),
    ]);

    if (!row) {
      throw new NotFoundException('Member not found');
    }

    const digits = row.user.phone.replace(/\D/g, '');
    const whatsappUrl = digits.length > 0 ? `https://wa.me/${digits}` : null;
    const [subscription, lifetimeCheckIns, lastAttendance, paymentsBlock] =
      await Promise.all([
      this.buildSubscriptionSummary(memberSubscriptions, now),
      this.prisma.attendanceRecord.count({
        where: { gymId, memberUserId: row.userId },
      }),
      this.prisma.attendanceRecord.findFirst({
        where: { gymId, memberUserId: row.userId },
        orderBy: { checkedInAt: 'desc' },
        select: { attendedOn: true, checkedInAt: true },
      }),
      this.buildMemberDetailPayments(gymId, row.id, row.userId, now),
    ]);

    const primarySub = subscription.current_subscriptions[0] ?? null;

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
      age: this.computeMemberAgeYears(
        now,
        row.user.ageYears,
        row.dateOfBirth,
      ),
      /** Same shape as entries in `GET /members` → `members[]` (list card). */
      summary: {
        id: row.id,
        name: row.user.fullName ?? '',
        phone: row.user.phone,
        status: computeMemberLifecycleStatus(row, now),
        plan_name:
          subscription.current_subscriptions
            .map((s) => s.plan_name)
            .filter(Boolean)
            .join(', ') ||
          subscription.freeze_subscriptions[0]?.plan_name ||
          '',
        expiry_date: row.membershipEndsAt
          ? MembersService.formatSubscriptionYmd(row.membershipEndsAt)
          : primarySub?.expiry_date ?? null,
        member_subscription_id: primarySub?.member_subscription_id ?? null,
        price_cents: primarySub?.price_cents ?? null,
        selling_price: primarySub?.selling_price ?? null,
        paid_cents: primarySub?.paid_cents ?? null,
        amount_pending: primarySub?.amount_pending ?? null,
        extension_fees_total: primarySub?.extension_fees_total ?? null,
        profile_image: row.user.avatarUrl ?? '',
        ...this.memberListProfileFields(now, {
          user: {
            fullName: row.user.fullName,
            ageYears: row.user.ageYears,
            address: row.user.address ?? null,
            aadhaarNumber: row.user.aadhaarNumber ?? null,
          },
          notes: row.notes,
          emergencyContactName: row.emergencyContactName,
          emergencyContactPhone: row.emergencyContactPhone,
          dateOfBirth: row.dateOfBirth,
        }),
      },
      subscription: {
        stats: subscription.stats,
        current_subscriptions: subscription.current_subscriptions,
        current_subscription: subscription.current_subscription,
        upcoming_subscriptions: subscription.upcoming_subscriptions,
        expired_subscriptions: subscription.expired_subscriptions,
        freeze_subscriptions: subscription.freeze_subscriptions,
      },
      user: {
        id: row.user.id,
        fullName: row.user.fullName,
        phone: row.user.phone,
        email: row.user.email,
        heightCm: row.user.heightCm != null ? Number(row.user.heightCm) : null,
        weightKg: row.user.weightKg != null ? Number(row.user.weightKg) : null,
        activityLevel: row.user.activityLevel,
        fitnessGoal: row.user.fitnessGoal,
        wellness: this.computeWellnessSnapshotForMember(
          now,
          row.user,
          row.gender,
          row.dateOfBirth,
        ),
        createdAt: row.user.createdAt,
        avatarUrl: row.user.avatarUrl,
        profile_image: row.user.avatarUrl ?? '',
        age: this.computeMemberAgeYears(
          now,
          row.user.ageYears,
          row.dateOfBirth,
        ),
        address: row.user.address ?? null,
        aadhaar_number:
          row.user.aadhaarNumber?.trim() ||
          this.parseStructuredMemberNotes(row.notes).aadhaar_number,
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
      paymentSummary: paymentsBlock.paymentSummary,
      paymentHistory: paymentsBlock.paymentHistory,
    };
  }

  /**
   * Legacy flat profile card.
   * With `gymId`: same as list/detail — `memberId` is GymUser id; staff/owner or the member themselves.
   * Without `gymId`: `memberId` is User id; gym subscriptions are absent; caller may read **only** their own row (`memberId` === JWT `sub`).
   */
  async getProfile(
    actorUserId: string,
    gymId: string | undefined,
    memberId: string,
  ) {
    const now = new Date();
    const trimmedGymId = gymId?.trim();

    if (trimmedGymId) {
      const row = await this.prisma.gymUser.findFirst({
        where: {
          id: memberId,
          gymId: trimmedGymId,
          role: GymRole.MEMBER,
        },
        include: this.memberDetailInclude(),
      });

      if (!row) {
        throw new NotFoundException('Member not found');
      }

      if (row.userId === actorUserId) {
        await this.gymAccess.assertMemberAtGym(actorUserId, trimmedGymId);
      } else {
        await this.gymAccess.assertCanManageGym(actorUserId, trimmedGymId);
      }

      const [memberSubscriptions, gym] = await Promise.all([
        this.fetchMemberSubscriptionsForDetail(row.id),
        this.loadGymBasicForProfile(trimmedGymId),
      ]);
      const subscription = await this.buildSubscriptionSummary(
        memberSubscriptions,
        now,
      );
      const subscriptionBlock =
        this.buildProfileSubscriptionBlock(subscription);

      return {
        id: row.id,
        name: row.user.fullName ?? '',
        phone: row.user.phone,
        gender: row.gender,
        dob: row.dateOfBirth,
        age: this.computeMemberAgeYears(
          now,
          row.user.ageYears,
          row.dateOfBirth,
        ),
        join_date: row.joinedAt,
        status: computeMemberLifecycleStatus(row, now),
        profile_image: row.user.avatarUrl ?? '',
        ...this.memberProfileWellnessFields(
          now,
          row.user,
          row.gender,
          row.dateOfBirth,
        ),
        gym,
        subscription: subscriptionBlock,
        stats: subscription.stats,
        current_subscriptions: subscription.current_subscriptions,
        current_subscription: subscription.current_subscription,
        upcoming_subscriptions: subscription.upcoming_subscriptions,
        expired_subscriptions: subscription.expired_subscriptions,
        past_subscriptions: subscription.expired_subscriptions,
        freeze_subscriptions: subscription.freeze_subscriptions,
      };
    }

    if (memberId !== actorUserId) {
      throw new ForbiddenException(
        'Without gymId you may only load your own profile (memberId must match the authenticated user).',
      );
    }

    const user = await this.prisma.user.findUnique({
      where: { id: actorUserId },
      select: {
        id: true,
        phone: true,
        fullName: true,
        gender: true,
        avatarUrl: true,
        createdAt: true,
        status: true,
        ageYears: true,
        heightCm: true,
        weightKg: true,
        activityLevel: true,
        fitnessGoal: true,
        wellness: true,
      },
    });
    if (!user) {
      throw new NotFoundException('Member not found');
    }

    const subscription = await this.buildSubscriptionSummary([], now);
    const status =
      user.status === UserStatus.ACTIVE ? ('active' as const) : ('inactive' as const);

    return {
      id: user.id,
      name: user.fullName ?? '',
      phone: user.phone,
      gender: user.gender,
      age: this.computeMemberAgeYears(
        now,
        user.ageYears,
        null,
      ),
      dob: null,
      join_date: user.createdAt,
      status,
      profile_image: user.avatarUrl ?? '',
      ...this.memberProfileWellnessFields(now, user, null, null),
      stats: subscription.stats,
      current_subscriptions: subscription.current_subscriptions,
      current_subscription: subscription.current_subscription,
      upcoming_subscriptions: subscription.upcoming_subscriptions,
      expired_subscriptions: subscription.expired_subscriptions,
      freeze_subscriptions: subscription.freeze_subscriptions,
    };
  }

  async update(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: UpdateMemberDto,
  ) {
    await this.patchGymMember(actorUserId, gymId, gymUserId, dto);
    return this.getDetail(actorUserId, gymId, gymUserId);
  }

  /** Shared gym-scoped member PATCH — **`User`** fields + gym row; freeform **`notes`** only (no structured address/aadhaar lines). */
  private async patchGymMember(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    dto: UpdateMemberDto,
  ): Promise<void> {
    const row = await this.prisma.gymUser.findFirst({
      where: { id: gymUserId, gymId, role: GymRole.MEMBER },
      select: {
        id: true,
        userId: true,
        notes: true,
        user: { select: { fullName: true } },
      },
    });
    if (!row) {
      throw new NotFoundException('Member not found');
    }

    if (row.userId === actorUserId) {
      await this.gymAccess.assertMemberAtGym(actorUserId, gymId);
    } else {
      await this.gymAccess.assertCanManageGym(actorUserId, gymId);
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
      const nextName = this.resolveUpdatedFullName(dto, row.user.fullName);
      if (nextName !== undefined) {
        userData.fullName = nextName;
      }
      if (dto.email !== undefined) {
        userData.email = normalizeEmailForStorage(dto.email);
      }
      this.applyMemberMetricFieldsFromDto(dto, userData);
      if (dto.age !== undefined) {
        userData.ageYears = dto.age;
      }
      if (dto.avatarUrl !== undefined) {
        userData.avatarUrl = dto.avatarUrl.trim() || null;
      } else if (dto.profile_image !== undefined) {
        userData.avatarUrl = dto.profile_image.trim() || null;
      }
      if (dto.aadhaar_number !== undefined) {
        userData.aadhaarNumber = dto.aadhaar_number;
      }
      if (dto.address !== undefined) {
        userData.address = dto.address;
      }
      if (dto.fitnessGoal !== undefined) {
        userData.fitnessGoal =
          dto.fitnessGoal != null && dto.fitnessGoal.trim().length > 0
            ? dto.fitnessGoal.trim()
            : null;
      }
      if (dto.gender !== undefined) {
        userData.gender = dto.gender?.trim() || null;
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
      const mergedNotes = this.mergeStructuredNotesOnUpdate(
        row.notes ?? null,
        dto,
      );
      if (mergedNotes !== undefined) {
        guData.notes = mergedNotes;
      }
      if (
        dto.emergencyContactName !== undefined ||
        dto.emergency_name !== undefined
      ) {
        const v =
          dto.emergencyContactName !== undefined
            ? dto.emergencyContactName
            : dto.emergency_name ?? null;
        guData.emergencyContactName = v?.trim() || null;
      }
      if (
        dto.emergencyContactPhone !== undefined ||
        dto.emergency_contact_phone !== undefined
      ) {
        const v =
          dto.emergencyContactPhone !== undefined
            ? dto.emergencyContactPhone
            : dto.emergency_contact_phone ?? null;
        guData.emergencyContactPhone = v?.trim() || null;
      }
      if (dto.dateOfBirth !== undefined || dto.dob !== undefined) {
        const raw = dto.dateOfBirth ?? dto.dob ?? '';
        guData.dateOfBirth = raw.trim()
          ? this.parseNullableDateField('dateOfBirth', raw.trim())
          : null;
      }
      if (dto.gender !== undefined) {
        guData.gender = dto.gender?.trim() || null;
      }
      if (dto.membershipEndsAt !== undefined) {
        guData.membershipEndsAt = dto.membershipEndsAt
          ? this.parseNullableDateField(
            'membershipEndsAt',
            dto.membershipEndsAt.trim(),
          )
          : null;
      }
      if (dto.date_of_joining !== undefined) {
        const joined = this.parseOptionalDateField(
          'date_of_joining',
          dto.date_of_joining,
        );
        if (joined !== undefined) {
          guData.joinedAt = joined;
        }
      }
      if (Object.keys(guData).length > 0) {
        await tx.gymUser.update({
          where: { id: row.id },
          data: guData,
        });
      }

      await this.syncUserWellnessTx(
        tx,
        row.userId,
        row.id,
        this.parseMaintenanceCaloriesFromDto(dto),
      );
    });

    if (dto.membershipEndsAt !== undefined) {
      await this.subscriptions.syncMembershipEndsAt(this.prisma, gymUserId);
    }
  }

  private async patchMemberProfileSelf(
    actorUserId: string,
    memberId: string,
    dto: UpdateMemberDto,
  ) {
    if (memberId !== actorUserId) {
      throw new ForbiddenException(
        'Without gymId, memberId must match the authenticated user.',
      );
    }
    const gymOnly =
      dto.notes !== undefined ||
      dto.emergencyContactName !== undefined ||
      dto.emergency_name !== undefined ||
      dto.emergencyContactPhone !== undefined ||
      dto.emergency_contact_phone !== undefined ||
      dto.isLead !== undefined ||
      dto.isActive !== undefined ||
      dto.membershipEndsAt !== undefined ||
      dto.date_of_joining !== undefined ||
      dto.dateOfBirth !== undefined ||
      dto.dob !== undefined;
    if (gymOnly) {
      throw new BadRequestException(
        'Provide query parameter gymId to update gym-member fields (same fields as POST /members create: notes, dob, emergencies, membership dates, joinedAt). `address`, `aadhaar_number`, and other **`User`** fields can be updated without `gymId` (self only).',
      );
    }

    if (dto.email !== undefined) {
      const emailNorm = normalizeEmailForStorage(dto.email);
      if (emailNorm) {
        const emailTaken = await this.prisma.user.findFirst({
          where: { email: emailNorm, NOT: { id: memberId } },
          select: { id: true },
        });
        if (emailTaken) {
          throw new ConflictException(
            'This email is already registered to another account',
          );
        }
      }
    }

    const current = await this.prisma.user.findUnique({
      where: { id: memberId },
      select: { fullName: true },
    });
    if (!current) {
      throw new NotFoundException('Member not found');
    }

    await this.prisma.$transaction(async (tx) => {
      const userData: Prisma.UserUpdateInput = {};
      const nextName = this.resolveUpdatedFullName(dto, current.fullName);
      if (nextName !== undefined) {
        userData.fullName = nextName;
      }
      if (dto.email !== undefined) {
        userData.email = normalizeEmailForStorage(dto.email);
      }
      this.applyMemberMetricFieldsFromDto(dto, userData);
      if (dto.age !== undefined) {
        userData.ageYears = dto.age;
      }
      if (dto.avatarUrl !== undefined) {
        userData.avatarUrl = dto.avatarUrl.trim() || null;
      } else if (dto.profile_image !== undefined) {
        userData.avatarUrl = dto.profile_image.trim() || null;
      }
      if (dto.gender !== undefined) {
        userData.gender = dto.gender?.trim() || null;
      }
      if (dto.aadhaar_number !== undefined) {
        userData.aadhaarNumber = dto.aadhaar_number;
      }
      if (dto.address !== undefined) {
        userData.address = dto.address;
      }
      if (dto.fitnessGoal !== undefined) {
        userData.fitnessGoal =
          dto.fitnessGoal != null && dto.fitnessGoal.trim().length > 0
            ? dto.fitnessGoal.trim()
            : null;
      }

      const gymMember = await tx.gymUser.findFirst({
        where: { userId: memberId, role: GymRole.MEMBER, isActive: true },
        orderBy: { joinedAt: 'desc' },
        select: { id: true },
      });

      const maintenanceCalories = this.parseMaintenanceCaloriesFromDto(dto);
      if (Object.keys(userData).length === 0) {
        await this.syncUserWellnessTx(
          tx,
          memberId,
          gymMember?.id ?? null,
          maintenanceCalories,
        );
        return;
      }
      await tx.user.update({
        where: { id: memberId },
        data: userData,
      });

      await this.syncUserWellnessTx(
        tx,
        memberId,
        gymMember?.id ?? null,
        maintenanceCalories,
      );
    });

    return this.getProfile(actorUserId, undefined, memberId);
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
      timezone?: string;
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
    const timeZone =
      opts?.timezone?.trim() || gym?.timezone?.trim() || 'UTC';
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

    const daySessionsMap = new Map<string, number>();
    for (const r of records) {
      const key = r.attendedOn.toISOString().slice(0, 10);
      daySessionsMap.set(key, (daySessionsMap.get(key) ?? 0) + 1);
    }

    const calendar: {
      date: string;
      status: string;
      sessions: number;
    }[] = [];
    for (let d = 1; d <= daysInMonth; d++) {
      const dt = new Date(Date.UTC(y, m - 1, d));
      const key = dt.toISOString().slice(0, 10);
      const sessions = daySessionsMap.get(key) ?? 0;
      calendar.push({
        date: key,
        status: sessions > 0 ? 'present' : 'absent',
        sessions,
      });
    }

    const recent_logs = recentRaw
      .flatMap((r) => this.toAttendancePunchEntries(r, timeZone, now))
      .sort(
        (a, b) =>
          new Date(b.time).getTime() - new Date(a.time).getTime(),
      );

    return {
      filter: { year: y, month: m, month_label: monthShortLabel(m) },
      gym_timezone: timeZone,
      stats: {
        days_present_month: daySessionsMap.size,
        total_sessions_month: records.length,
        days_in_month: daysInMonth,
        lifetime_check_ins: lifetime,
        present_days: daySessionsMap.size,
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
    timezone?: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const memberUserId = await this.getMemberUserId(gymId, gymUserId);
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { timezone: true },
    });
    const timeZone =
      timezone?.trim() || gym?.timezone?.trim() || 'UTC';
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

    const items = rows
      .flatMap((r) => this.toAttendancePunchEntries(r, timeZone, now))
      .sort(
        (a, b) =>
          new Date(b.time).getTime() - new Date(a.time).getTime(),
      );

    return {
      filters: { from: from?.trim() ?? null, to: to?.trim() ?? null },
      gym_timezone: timeZone,
      total,
      limit,
      offset,
      items,
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

  /** Monday 00:00 UTC (ISO week boundary; used for “this week” workout counts). */
  private static utcMondayStartUtc(d: Date): Date {
    const x = new Date(d);
    const dow = x.getUTCDay();
    const delta = dow === 0 ? -6 : 1 - dow;
    x.setUTCDate(x.getUTCDate() + delta);
    x.setUTCHours(0, 0, 0, 0);
    return x;
  }

  private static firstNameFromFullName(
    fullName: string | null | undefined,
  ): string {
    const s = (fullName ?? '').trim();
    if (!s) {
      return '';
    }
    return s.split(/\s+/)[0] ?? '';
  }

  private static greetingLine(now: Date, timeZone: string): string {
    const hourParts = new Intl.DateTimeFormat('en', {
      timeZone,
      hour: 'numeric',
      hour12: false,
    }).formatToParts(now);
    const hour = parseInt(
      hourParts.find((p) => p.type === 'hour')?.value ?? '12',
      10,
    );
    if (hour < 12) {
      return 'Good morning';
    }
    if (hour < 17) {
      return 'Good afternoon';
    }
    return 'Good evening';
  }

  /** `YYYY-MM-DD` calendar day in `timeZone` for an instant stored as UTC `Date`. */
  private static calendarDayKey(d: Date, timeZone: string): string {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(d);
  }

  /** DietMeal.repeatDays index: Monday = 0 … Sunday = 6 (schema). */
  private static todayRepeatDayIndex(now: Date, timeZone: string): number {
    const dayName = new Intl.DateTimeFormat('en-US', {
      timeZone,
      weekday: 'long',
    }).format(now);
    const map: Record<string, number> = {
      Monday: 0,
      Tuesday: 1,
      Wednesday: 2,
      Thursday: 3,
      Friday: 4,
      Saturday: 5,
      Sunday: 6,
    };
    return map[dayName] ?? 0;
  }

  private static addCalendarDaysYmd(ymd: string, deltaDays: number): string {
    const [y, m, d] = ymd.split('-').map(Number);
    const dt = new Date(Date.UTC(y, m - 1, d));
    dt.setUTCDate(dt.getUTCDate() + deltaDays);
    const y2 = dt.getUTCFullYear();
    const m2 = String(dt.getUTCMonth() + 1).padStart(2, '0');
    const d2 = String(dt.getUTCDate()).padStart(2, '0');
    return `${y2}-${m2}-${d2}`;
  }

  /** Consecutive days with attendance ending today (or streak through yesterday if not checked in yet). */
  private static computeAttendanceStreak(
    dateKeys: Set<string>,
    now: Date,
    timeZone: string,
  ): number {
    const today = MembersService.calendarDayKey(now, timeZone);
    const yesterday = MembersService.addCalendarDaysYmd(today, -1);
    let cursor = dateKeys.has(today) ? today : yesterday;
    if (!dateKeys.has(cursor)) {
      return 0;
    }
    let streak = 0;
    while (dateKeys.has(cursor)) {
      streak += 1;
      cursor = MembersService.addCalendarDaysYmd(cursor, -1);
    }
    return streak;
  }

  private toAttendancePunchEntries(
    r: {
      id: string;
      attendedOn: Date;
      checkedInAt: Date;
      checkedOutAt: Date | null;
    },
    timeZone: string,
    now: Date,
  ) {
    const tz = timeZone || 'UTC';
    const punctuality = classifyAttendancePunctuality(r.checkedInAt, tz);

    const formatTime = (d: Date) =>
      new Intl.DateTimeFormat('en-US', {
        timeZone: tz,
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      }).format(d);

    const formatDate = (d: Date) =>
      new Intl.DateTimeFormat('en-US', {
        timeZone: tz,
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      }).format(d);

    const entries: Array<{
      id: string;
      record_id: string;
      type: 'check_in' | 'check_out';
      headline: string;
      punctuality: string;
      punctuality_label: string;
      attended_on: string;
      time: string;
      display_time: string;
      display_date: string;
      display_relative: string;
    }> = [];

    entries.push({
      id: `${r.id}_in`,
      record_id: r.id,
      type: 'check_in',
      headline: 'Check In',
      punctuality,
      punctuality_label: punctualityDisplayLabel(punctuality),
      attended_on: r.attendedOn.toISOString().slice(0, 10),
      time: r.checkedInAt.toISOString(),
      display_time: formatTime(r.checkedInAt),
      display_date: formatDate(r.checkedInAt),
      display_relative: formatCheckInRelativeLine(r.checkedInAt, now, tz),
    });

    if (r.checkedOutAt) {
      entries.push({
        id: `${r.id}_out`,
        record_id: r.id,
        type: 'check_out',
        headline: 'Check Out',
        punctuality,
        punctuality_label: punctualityDisplayLabel(punctuality),
        attended_on: r.attendedOn.toISOString().slice(0, 10),
        time: r.checkedOutAt.toISOString(),
        display_time: formatTime(r.checkedOutAt),
        display_date: formatDate(r.checkedOutAt),
        display_relative: formatCheckInRelativeLine(r.checkedOutAt, now, tz),
      });
    }

    return entries;
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
        amount: r.amountCents,
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
      total_paid: paidAgg._sum.amountCents ?? 0,
      outstanding: pendingAgg._sum.amountCents ?? 0,
      last_payment: last
        ? {
          amount: last.amountCents,
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

  /**
   * Dashboard without gym context: `User` row + unread notifications across all gyms.
   * Gym-scoped sections are empty defaults.
   */
  private async memberDashboardUserOnly(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, fullName: true, avatarUrl: true },
    });
    if (!user) {
      throw new NotFoundException('User not found');
    }
    const now = new Date();
    const unreadNotifications = await this.prisma.notification.count({
      where: { recipientUserId: userId, readAt: null },
    });
    const firstName = MembersService.firstNameFromFullName(user.fullName);
    const greeting = MembersService.greetingLine(now, 'UTC');
    const consumed = await this.diet.summarizeDailyConsumption(userId);

    return {
      gymId: null as string | null,
      user: {
        id: user.id,
        firstName,
        displayName: user.fullName ?? firstName,
        avatarUrl: user.avatarUrl,
        greeting,
        tagline: "Ready for today's workout?",
        unreadNotifications,
      },
      membership: {
        expiresAt: null as string | null,
        daysRemaining: null as number | null,
        totalDays: null as number | null,
        percentRemaining: null as number | null,
        statusLabel: null as string | null,
        progressLabel: null as string | null,
      },
      todayWorkout: null,
      stats: {
        sessionsThisWeek: 0,
        streakDays: 0,
        totalHours: 0,
      },
      nutrition: this.buildDashboardNutrition(consumed, 2200),
      attendance: {
        daysAttended: 0,
        periodDays: 0,
        lifetimeCheckIns: 0,
        percentileAmongMembers: null as number | null,
        insightLabel: null as string | null,
      },
    };
  }

  /**
   * Member app home dashboard: `userId` = JWT `sub`.
   * With `gymId`: active **member** at that gym required.
   * Without `gymId`: **user-only** snapshot (no gym membership metrics).
   */
  async getSelfMemberDashboard(userId: string, gymId?: string) {
    const resolvedGymId = gymId?.trim();
    if (!resolvedGymId) {
      return this.memberDashboardUserOnly(userId);
    }

    const { gymUserId } = await this.gymAccess.assertMemberAtGym(
      userId,
      resolvedGymId,
    );
    const now = new Date();

    const gym = await this.prisma.gym.findUnique({
      where: { id: resolvedGymId },
      select: { timezone: true, name: true },
    });
    const timeZone = gym?.timezone?.trim() || 'UTC';

    const weekStartUtc = MembersService.utcMondayStartUtc(now);

    const [
      gu,
      unreadNotifications,
      activeWorkout,
      attendanceGroup,
      sessionsThisWeek,
      workoutDurations,
      dietMeals,
      membershipEnd,
    ] = await Promise.all([
      this.prisma.gymUser.findFirst({
        where: { id: gymUserId, gymId: resolvedGymId },
        select: {
          membershipEndsAt: true,
          joinedAt: true,
          user: {
            select: {
              id: true,
              fullName: true,
              avatarUrl: true,
            },
          },
        },
      }),
      this.prisma.notification.count({
        where: {
          gymId: resolvedGymId,
          recipientUserId: userId,
          readAt: null,
        },
      }),
      this.prisma.memberWorkoutPlan.findFirst({
        where: { gymUserId, gymId: resolvedGymId, completed: false },
        orderBy: [{ completed: 'asc' }, { updatedAt: 'desc' }],
        include: {
          exercises: {
            take: 1,
            orderBy: { createdAt: 'asc' },
            include: {
              exercise: { select: { assetUrl: true, name: true } },
            },
          },
          _count: { select: { exercises: true } },
        },
      }),
      this.prisma.attendanceRecord.groupBy({
        by: ['memberUserId'],
        where: { gymId: resolvedGymId },
        _count: { _all: true },
      }),
      this.prisma.memberWorkoutPlan.count({
        where: {
          gymUserId,
          completed: true,
          updatedAt: { gte: weekStartUtc },
        },
      }),
      this.prisma.memberWorkoutPlan.findMany({
        where: {
          gymUserId,
          completed: true,
          startedAt: { not: null },
          endedAt: { not: null },
        },
        select: { startedAt: true, endedAt: true },
      }),
      this.prisma.dietMeal.findMany({
        where: { gymUserId, gymId: resolvedGymId },
        include: { foodLines: true },
      }),
      this.prisma.memberSubscription.findFirst({
        where: {
          gymUserId,
          endsAt: { gt: now },
          startsAt: { lte: now },
          status: {
            in: [
              MemberSubscriptionStatus.ACTIVE,
              MemberSubscriptionStatus.FROZEN,
            ],
          },
        },
        orderBy: { endsAt: 'desc' },
        select: { startsAt: true, endsAt: true },
      }),
    ]);

    if (!gu) {
      throw new NotFoundException('Membership not found');
    }

    const subsFallback = await this.prisma.memberSubscription.findFirst({
      where: { gymUserId },
      orderBy: { endsAt: 'desc' },
      select: { startsAt: true, endsAt: true },
    });
    const currentSub = membershipEnd ?? subsFallback;

    let daysRemaining: number | null = null;
    let totalMembershipDays: number | null = null;
    let membershipPercentRemaining: number | null = null;
    let expiryLabel: string | null = null;

    const endDate = gu.membershipEndsAt ?? currentSub?.endsAt ?? null;
    const startDate = currentSub?.startsAt ?? gu.joinedAt;

    if (endDate) {
      daysRemaining = Math.max(
        0,
        Math.ceil((endDate.getTime() - now.getTime()) / 86_400_000),
      );
      totalMembershipDays = Math.max(
        1,
        Math.ceil((endDate.getTime() - startDate.getTime()) / 86_400_000),
      );
      membershipPercentRemaining =
        totalMembershipDays > 0
          ? Math.min(
            100,
            Math.round((daysRemaining / totalMembershipDays) * 100),
          )
          : null;
      expiryLabel =
        daysRemaining === 0
          ? 'Membership expired'
          : daysRemaining === 1
            ? 'Expires tomorrow'
            : `Expires in ${daysRemaining} days`;
    }

    const firstName = MembersService.firstNameFromFullName(gu.user.fullName);
    const greeting = MembersService.greetingLine(now, timeZone);
    const tagline = "Ready for today's workout?";

    const myAttendanceCount =
      attendanceGroup.find((g) => g.memberUserId === userId)?._count._all ?? 0;
    const peerTotals = attendanceGroup.map((g) => g._count._all);
    const belowMe = peerTotals.filter((c) => c < myAttendanceCount).length;
    const percentileAmongMembers =
      peerTotals.length > 0
        ? Math.round((belowMe / peerTotals.length) * 100)
        : null;

    const attendedDates = await this.prisma.attendanceRecord.findMany({
      where: { gymId: resolvedGymId, memberUserId: userId },
      select: { attendedOn: true },
      orderBy: { attendedOn: 'desc' },
      take: 800,
    });
    const dateKeys = new Set(
      attendedDates.map((r) =>
        MembersService.calendarDayKey(r.attendedOn, timeZone),
      ),
    );
    const streakDays = MembersService.computeAttendanceStreak(
      dateKeys,
      now,
      timeZone,
    );

    let totalHours = 0;
    for (const w of workoutDurations) {
      if (w.startedAt && w.endedAt) {
        totalHours += (w.endedAt.getTime() - w.startedAt.getTime()) / 3_600_000;
      }
    }
    const totalHoursRounded = Math.round(totalHours * 10) / 10;

    const todayIdx = MembersService.todayRepeatDayIndex(now, timeZone);
    const consumed = await this.diet.summarizeDailyConsumption(userId, {
      gymId: resolvedGymId,
    });

    let calorieGoal = 2200;
    let scheduledTodayKcal = 0;
    for (const meal of dietMeals) {
      if (
        meal.repeatEnabled &&
        meal.repeatDays.length > 0 &&
        !meal.repeatDays.includes(todayIdx)
      ) {
        continue;
      }
      for (const line of meal.foodLines) {
        scheduledTodayKcal += line.calories * line.quantity;
      }
    }
    if (scheduledTodayKcal > 0) {
      calorieGoal = Math.max(calorieGoal, scheduledTodayKcal);
    } else if (dietMeals.length > 0) {
      let templateSum = 0;
      for (const meal of dietMeals) {
        for (const line of meal.foodLines) {
          templateSum += line.calories * line.quantity;
        }
      }
      if (templateSum > 0) {
        calorieGoal = Math.max(calorieGoal, templateSum);
      }
    }

    const nutrition = this.buildDashboardNutrition(consumed, calorieGoal);

    const periodDays = Math.max(
      1,
      Math.ceil((now.getTime() - startDate.getTime()) / 86_400_000),
    );
    const attendanceDaysInPeriod = await this.prisma.attendanceRecord.count({
      where: {
        gymId: resolvedGymId,
        memberUserId: userId,
        attendedOn: { gte: startDate, lte: now },
      },
    });

    const exerciseCount = activeWorkout?._count.exercises ?? 0;
    const heroImage = activeWorkout?.exercises[0]?.exercise.assetUrl ?? null;

    return {
      gymId: resolvedGymId,
      gymUserId: gymUserId,
      userId: gu.user.id,
      user: {
        id: gu.user.id,
        firstName,
        displayName: gu.user.fullName ?? firstName,
        avatarUrl: gu.user.avatarUrl,
        greeting,
        tagline,
        unreadNotifications: unreadNotifications,
      },
      membership: {
        expiresAt: endDate?.toISOString() ?? null,
        daysRemaining,
        totalDays: totalMembershipDays,
        percentRemaining: membershipPercentRemaining,
        statusLabel: expiryLabel,
        progressLabel: expiryLabel,
      },
      todayWorkout: activeWorkout
        ? {
          id: activeWorkout.id,
          title: activeWorkout.title,
          exerciseCount,
          tags: activeWorkout.completed
            ? ([] as string[])
            : ['TRAINER PLAN', 'RECOMMENDED'],
          imageUrl: heroImage,
          completed: activeWorkout.completed,
        }
        : null,
      stats: {
        sessionsThisWeek: sessionsThisWeek,
        streakDays,
        totalHours: totalHoursRounded,
      },
      nutrition,
      attendance: {
        daysAttended: attendanceDaysInPeriod,
        periodDays,
        lifetimeCheckIns: myAttendanceCount,
        percentileAmongMembers,
        insightLabel:
          percentileAmongMembers != null
            ? `Better than ${percentileAmongMembers}% of members!`
            : null,
      },
    };
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

  /** Split display name into list/detail fields when first/last are not stored separately. */
  private splitFullName(fullName: string | null | undefined): {
    first_name: string;
    last_name: string;
  } {
    const s = fullName?.trim() ?? '';
    if (!s) {
      return { first_name: '', last_name: '' };
    }
    const m = s.match(/^\S+/);
    const first = m?.[0] ?? s;
    const rest = s.slice(first.length).trim();
    return { first_name: first, last_name: rest };
  }

  /** Apply create-style **`fullName`** vs **`first_name`** / **`last_name`** on PATCH. */
  private resolveUpdatedFullName(
    dto: UpdateMemberDto,
    currentFullName: string | null,
  ): string | undefined {
    if (dto.fullName !== undefined) {
      const t = dto.fullName.trim();
      return t || currentFullName?.trim() || undefined;
    }
    if (dto.first_name === undefined && dto.last_name === undefined) {
      return undefined;
    }
    const cur = this.splitFullName(currentFullName);
    const nf =
      dto.first_name !== undefined ? dto.first_name.trim() : cur.first_name;
    const nl =
      dto.last_name !== undefined ? dto.last_name.trim() : cur.last_name;
    const merged = `${nf} ${nl}`.trim();
    return merged || currentFullName?.trim() || undefined;
  }

  private parseStructuredMemberNotes(notes: string | null | undefined): {
    address: string | null;
    aadhaar_number: string | null;
  } {
    if (!notes?.trim()) {
      return { address: null, aadhaar_number: null };
    }
    let address: string | null = null;
    let aadhaar_number: string | null = null;
    for (const raw of notes.split('\n')) {
      const line = raw.trim();
      if (!line) continue;
      const low = line.toLowerCase();
      if (
        address === null &&
        low.startsWith('address:')
      ) {
        address = line.slice(line.indexOf(':') + 1).trim() || null;
        continue;
      }
      if (
        aadhaar_number === null &&
        low.startsWith('aadhaar:')
      ) {
        aadhaar_number =
          line.slice(line.indexOf(':') + 1).trim() || null;
        continue;
      }
    }
    return { address, aadhaar_number };
  }

  /** Freeform `GymUser.notes` only (no structured `Address:` / `Aadhaar:` lines — those live on **`User`**). */
  private composeFreeformNotes(notes?: string): string | null {
    const t = notes?.trim();
    return t ? t : null;
  }

  private stripStructuredNoteLines(notes: string | null | undefined): string {
    if (!notes?.trim()) {
      return '';
    }
    return notes
      .split('\n')
      .map((l) => l.trim())
      .filter(
        (l) =>
          l &&
          !l.toLowerCase().startsWith('address:') &&
          !l.toLowerCase().startsWith('aadhaar:'),
      )
      .join('\n')
      .trim();
  }

  /**
   * Updates freeform `GymUser.notes` and strips legacy `Address:` / `Aadhaar:` lines when
   * **`User.address`** / **`User.aadhaarNumber`** are PATCHed.
   */
  private mergeStructuredNotesOnUpdate(
    currentNotes: string | null | undefined,
    dto: Pick<UpdateMemberDto, 'notes' | 'address' | 'aadhaar_number'>,
  ): string | null | undefined {
    if (
      dto.notes === undefined &&
      dto.address === undefined &&
      dto.aadhaar_number === undefined
    ) {
      return undefined;
    }
    const freeform =
      dto.notes !== undefined
        ? dto.notes?.trim() ?? ''
        : this.stripStructuredNoteLines(currentNotes ?? '');
    return this.composeFreeformNotes(freeform);
  }

  private computeMemberAgeYears(
    now: Date,
    ageYears: number | null | undefined,
    dateOfBirth: Date | null | undefined,
  ): number | null {
    if (ageYears != null && ageYears >= 0) {
      return ageYears;
    }
    if (!dateOfBirth || Number.isNaN(dateOfBirth.getTime())) {
      return null;
    }
    const d = dateOfBirth;
    let age = now.getFullYear() - d.getFullYear();
    const mo = now.getMonth() - d.getMonth();
    if (mo < 0 || (mo === 0 && now.getDate() < d.getDate())) {
      age -= 1;
    }
    return age;
  }

  private formatMemberListDob(
    dateOfBirth: Date | null | undefined,
  ): string | null {
    if (!dateOfBirth || Number.isNaN(dateOfBirth.getTime())) return null;
    return dateOfBirth.toISOString().slice(0, 10);
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
