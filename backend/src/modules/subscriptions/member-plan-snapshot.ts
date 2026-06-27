import type { GymPlan, SubscriptionPlan } from '@prisma/client';

export type MemberPlanSnapshot =
  | {
      source: 'catalog';
      id: string;
      name: string;
      code: string;
      interval: string;
      priceCents: number;
      currency: string;
    }
  | {
      source: 'gym';
      id: string;
      name: string;
      type: GymPlan['type'];
      durationDays: number;
      priceCents: number;
      currency: string;
    };

export function memberPlanSnapshot(
  plan: Pick<
    SubscriptionPlan,
    'id' | 'name' | 'code' | 'interval' | 'priceCents' | 'currency'
  > | null,
  gymPlan: Pick<
    GymPlan,
    'id' | 'name' | 'type' | 'durationDays' | 'priceCents' | 'currency'
  > | null,
): MemberPlanSnapshot | null {
  if (plan) {
    return {
      source: 'catalog',
      id: plan.id,
      name: plan.name,
      code: plan.code,
      interval: plan.interval,
      priceCents: plan.priceCents,
      currency: plan.currency,
    };
  }
  if (gymPlan) {
    return {
      source: 'gym',
      id: gymPlan.id,
      name: gymPlan.name,
      type: gymPlan.type,
      durationDays: gymPlan.durationDays,
      priceCents: gymPlan.priceCents,
      currency: gymPlan.currency,
    };
  }
  return null;
}

export function memberPlanDisplayName(
  plan: Pick<SubscriptionPlan, 'name'> | null,
  gymPlan: Pick<GymPlan, 'name'> | null,
): string {
  return plan?.name ?? gymPlan?.name ?? 'Plan';
}
