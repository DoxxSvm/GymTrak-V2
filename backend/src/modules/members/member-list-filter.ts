import { Prisma } from '@prisma/client';
import { endOfUtcDayAfter, startOfUtcDay } from '../../common/utils/utc-date';

export enum MemberListFilter {
  ALL = 'all',
  ACTIVE = 'active',
  EXPIRING = 'expiring',
  INACTIVE = 'inactive',
  EXPIRED = 'expired',
  LEADS = 'leads',
}

/**
 * Extra `where` for GymUser rows (role MEMBER) matching dashboard-style lifecycle buckets.
 */
export function memberListFilterWhere(
  filter: MemberListFilter | undefined,
  now: Date,
): Prisma.GymUserWhereInput {
  if (filter == null || filter === MemberListFilter.ALL) {
    return {};
  }
  const start = startOfUtcDay(now);
  const windowEnd = endOfUtcDayAfter(start, 7);

  switch (filter) {
    case MemberListFilter.LEADS:
      return { isLead: true };
    case MemberListFilter.INACTIVE:
      return { isLead: false, isActive: false };
    case MemberListFilter.EXPIRED:
      return {
        isLead: false,
        isActive: true,
        membershipEndsAt: { not: null, lt: start },
      };
    case MemberListFilter.EXPIRING:
      return {
        isLead: false,
        isActive: true,
        membershipEndsAt: {
          not: null,
          gte: start,
          lte: windowEnd,
        },
      };
    case MemberListFilter.ACTIVE:
      return {
        isLead: false,
        isActive: true,
        OR: [
          { membershipEndsAt: null },
          { membershipEndsAt: { gt: windowEnd } },
        ],
      };
    default:
      return {};
  }
}
