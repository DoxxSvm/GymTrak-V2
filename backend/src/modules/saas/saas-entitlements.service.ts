import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { GlobalRole, GymTrakSaasTier } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

const TIER_RANK: Record<GymTrakSaasTier, number> = {
  [GymTrakSaasTier.BASIC]: 1,
  [GymTrakSaasTier.PLUS]: 2,
  [GymTrakSaasTier.PREMIUM]: 3,
};

/** Product flags derived from GymTrak SaaS tier (revenue SKUs). */
export type SaasBusinessFeatures = {
  leads: boolean;
  expenses: boolean;
  expenseMonthlyAnalytics: boolean;
};

/**
 * Resolves GymTrak platform tier from GymSubscription + SubscriptionPlan.saasTier.
 * Gyms without a SaaS row default to PLUS so existing installs keep business tools
 * until billing is wired; super-admins always see PREMIUM for demos/support.
 */
@Injectable()
export class SaasEntitlementsService {
  constructor(private readonly prisma: PrismaService) {}

  async listSaasPlans() {
    const rows = await this.prisma.subscriptionPlan.findMany({
      where: { isActive: true, saasTier: { not: null } },
      orderBy: { priceCents: 'asc' },
      select: {
        id: true,
        name: true,
        interval: true,
        priceCents: true,
      },
    });
    return rows.map((r) => ({
      id: r.id,
      name: r.name,
      price: Math.round(r.priceCents / 100),
      billing_cycle: r.interval,
      features: [],
    }));
  }

  async createCustomPlan(input: {
    plan_name: string;
    billing_cycle: string;
    price: number;
    features?: string[];
  }) {
    const code = `custom_${Date.now()}`;
    const row = await this.prisma.subscriptionPlan.create({
      data: {
        name: input.plan_name.trim(),
        code,
        interval: input.billing_cycle.trim().toLowerCase(),
        priceCents: input.price * 100,
        isActive: true,
        saasTier: null,
      },
      select: { id: true, name: true, interval: true, priceCents: true },
    });
    return {
      id: row.id,
      name: row.name,
      price: Math.round(row.priceCents / 100),
      billing_cycle: row.interval,
      features: input.features ?? [],
    };
  }

  async getEffectiveTier(
    gymId: string,
    actorUserId?: string,
  ): Promise<GymTrakSaasTier> {
    if (actorUserId) {
      const actor = await this.prisma.user.findUnique({
        where: { id: actorUserId },
        select: { globalRole: true },
      });
      if (actor?.globalRole === GlobalRole.SUPER_ADMIN) {
        return GymTrakSaasTier.PREMIUM;
      }
    }

    const sub = await this.prisma.gymSubscription.findUnique({
      where: { gymId },
      include: { plan: { select: { saasTier: true } } },
    });

    const tier = sub?.plan?.saasTier;
    if (tier) {
      return tier;
    }

    return GymTrakSaasTier.PLUS;
  }

  async getEntitlements(gymId: string, actorUserId: string) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const sub = await this.prisma.gymSubscription.findUnique({
      where: { gymId },
      include: {
        plan: {
          select: {
            name: true,
            code: true,
            saasTier: true,
            interval: true,
          },
        },
      },
    });

    const effectiveTier = await this.getEffectiveTier(gymId, actorUserId);
    const features = this.featuresForTier(effectiveTier);

    return {
      gymId,
      effectiveTier,
      subscription: sub
        ? {
            status: sub.status,
            renewsAt: sub.renewsAt,
            planName: sub.plan.name,
            planCode: sub.plan.code,
            planSaasTier: sub.plan.saasTier,
          }
        : null,
      features,
    };
  }

  featuresForTier(tier: GymTrakSaasTier): SaasBusinessFeatures {
    const r = TIER_RANK[tier];
    return {
      leads: r >= TIER_RANK[GymTrakSaasTier.BASIC],
      expenses: r >= TIER_RANK[GymTrakSaasTier.PLUS],
      expenseMonthlyAnalytics: r >= TIER_RANK[GymTrakSaasTier.PREMIUM],
    };
  }

  assertLeads(gymId: string, actorUserId: string): Promise<void> {
    return this.assertMinTier(
      gymId,
      GymTrakSaasTier.BASIC,
      'Upgrade to GymTrak Basic or higher to use Leads & Enquiries.',
      actorUserId,
    );
  }

  assertExpenses(gymId: string, actorUserId: string): Promise<void> {
    return this.assertMinTier(
      gymId,
      GymTrakSaasTier.PLUS,
      'Upgrade to GymTrak Plus to use Expense Tracking.',
      actorUserId,
    );
  }

  assertExpenseAnalytics(gymId: string, actorUserId: string): Promise<void> {
    return this.assertMinTier(
      gymId,
      GymTrakSaasTier.PREMIUM,
      'Upgrade to GymTrak Premium for monthly expense analytics.',
      actorUserId,
    );
  }

  private async assertMinTier(
    gymId: string,
    min: GymTrakSaasTier,
    message: string,
    actorUserId: string,
  ): Promise<void> {
    const effective = await this.getEffectiveTier(gymId, actorUserId);
    if (TIER_RANK[effective] < TIER_RANK[min]) {
      throw new ForbiddenException(message);
    }
  }
}
