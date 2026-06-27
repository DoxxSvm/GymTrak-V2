import type { GlobalRole, LastActiveRole } from '@prisma/client';

/** JWT claim + API-facing persona for gym owners using dual profiles (`member_profiles`). */
export type ActiveAppRole = 'owner' | 'member';

export interface JwtPayload {
  sub: string;
  phone: string;
  globalRole: GlobalRole;
  /**
   * Default gym for this session (owned gym, else first active trainer/staff/member gym).
   * Issued on login; refreshed on token refresh. Omitted on temp signup tokens.
   */
  gymId?: string | null;
  /**
   * Present when the user owns at least one gym: consumer persona after owner OTP login / switch APIs.
   * Derived from `User.lastActiveRole` + optional `MemberProfile`.
   */
  activeAppRole?: ActiveAppRole;
  /** Short-lived signup token (Bearer); only allowed on select-role / gym / gyms POST. */
  isTemp?: boolean;
}

export type JwtUser = JwtPayload;

/** Narrow DB snapshot used when deriving JWT active persona without extra queries. */
export type OwnerPersonaUserPick = {
  lastActiveRole: LastActiveRole;
  ownedGyms: { id: string }[];
  memberProfile: { id: string } | null;
};

/** Read-only persona for JWT / guards from a single User query (no DB heal). */
export function activePersonaFromOwnerRow(
  row: OwnerPersonaUserPick | null,
): ActiveAppRole | undefined {
  if (!row || row.ownedGyms.length === 0) return undefined;
  if (row.lastActiveRole === 'MEMBER' && row.memberProfile) {
    return 'member';
  }
  return 'owner';
}
