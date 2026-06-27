import type { GymUser } from '@prisma/client';
import { endOfUtcDayAfter, startOfUtcDay } from '../../common/utils/utc-date';

export type MemberLifecycleStatus =
  | 'lead'
  | 'inactive'
  | 'expired'
  | 'expiring'
  | 'active';

export function computeMemberLifecycleStatus(
  row: Pick<GymUser, 'isLead' | 'isActive' | 'membershipEndsAt'>,
  now: Date = new Date(),
): MemberLifecycleStatus {
  if (row.isLead) {
    return 'lead';
  }
  if (!row.isActive) {
    return 'inactive';
  }
  const start = startOfUtcDay(now);
  const end = row.membershipEndsAt;
  if (!end) {
    return 'expired';
  }
  if (end < start) {
    return 'expired';
  }
  const windowEnd = endOfUtcDayAfter(start, 7);
  if (end >= start && end <= windowEnd) {
    return 'expiring';
  }
  return 'active';
}
