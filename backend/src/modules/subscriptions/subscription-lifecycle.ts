import { MemberSubscriptionStatus } from '@prisma/client';
import { addUtcMonths, addUtcYears } from '../../common/utils/utc-date';

/** Catalog interval string → end date from anchor (typically startsAt or current endsAt). */
export function applyPlanInterval(from: Date, interval: string): Date {
  const i = interval.trim().toLowerCase();
  if (i === 'yearly' || i === 'year' || i === 'annual') {
    return addUtcYears(from, 1);
  }
  if (i === 'monthly' || i === 'month') {
    return addUtcMonths(from, 1);
  }
  return addUtcMonths(from, 1);
}

export function balanceDueCents(priceCents: number, paidCents: number): number {
  return Math.max(0, priceCents - paidCents);
}

/**
 * Canonical status for API responses: time-based ENDED/SCHEDULED/ACTIVE, plus CANCELED/FROZEN overrides.
 */
export function effectiveSubscriptionStatus(
  row: { startsAt: Date; endsAt: Date; status: MemberSubscriptionStatus },
  now: Date,
): MemberSubscriptionStatus {
  if (row.status === MemberSubscriptionStatus.CANCELED) {
    return MemberSubscriptionStatus.CANCELED;
  }
  if (row.endsAt <= now) {
    return MemberSubscriptionStatus.ENDED;
  }
  if (row.status === MemberSubscriptionStatus.FROZEN) {
    return MemberSubscriptionStatus.FROZEN;
  }
  if (row.startsAt > now) {
    return MemberSubscriptionStatus.SCHEDULED;
  }
  return MemberSubscriptionStatus.ACTIVE;
}

export function initialStatusFromWindow(
  startsAt: Date,
  endsAt: Date,
  now: Date,
): MemberSubscriptionStatus {
  if (endsAt <= now) {
    return MemberSubscriptionStatus.ENDED;
  }
  if (startsAt > now) {
    return MemberSubscriptionStatus.SCHEDULED;
  }
  return MemberSubscriptionStatus.ACTIVE;
}

/** API / aggregation bucket (inclusive end: still CURRENT when `now === endsAt`). */
export enum SubscriptionWindowStatus {
  CURRENT = 'CURRENT',
  UPCOMING = 'UPCOMING',
  EXPIRED = 'EXPIRED',
}

export function getSubscriptionWindowStatus(
  startsAt: Date,
  endsAt: Date,
  now: Date,
): SubscriptionWindowStatus {
  const t = now.getTime();
  const st = startsAt.getTime();
  const en = endsAt.getTime();
  if (t >= st && t <= en) {
    return SubscriptionWindowStatus.CURRENT;
  }
  if (st > t) {
    return SubscriptionWindowStatus.UPCOMING;
  }
  return SubscriptionWindowStatus.EXPIRED;
}

/**
 * Buckets for member profile lists. `ENDED` is always **expired** in the UI (renew/upgrade history),
 * even if dates were adjusted oddly — avoids missing rows in `expired_subscriptions`. `CANCELED` → omit.
 * `FROZEN` → omit here (shown only in `freeze_subscriptions` on member detail, not `current_subscriptions`).
 */
export function getMemberSubscriptionUiBucket(
  row: { startsAt: Date; endsAt: Date; status: MemberSubscriptionStatus },
  now: Date,
): SubscriptionWindowStatus | null {
  if (row.status === MemberSubscriptionStatus.CANCELED) {
    return null;
  }
  if (row.status === MemberSubscriptionStatus.FROZEN) {
    return null;
  }
  if (row.status === MemberSubscriptionStatus.ENDED) {
    return SubscriptionWindowStatus.EXPIRED;
  }
  return getSubscriptionWindowStatus(row.startsAt, row.endsAt, now);
}
