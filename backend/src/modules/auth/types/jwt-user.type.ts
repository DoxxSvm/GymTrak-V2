import type { GlobalRole } from '@prisma/client';

export interface JwtPayload {
  sub: string;
  phone: string;
  globalRole: GlobalRole;
  /**
   * Default gym for this session (owned gym, else first active trainer/staff/member gym).
   * Issued on login; refreshed on token refresh. Omitted on temp signup tokens.
   */
  gymId?: string | null;
  /** Short-lived signup token (Bearer); only allowed on select-role / gym / gyms POST. */
  isTemp?: boolean;
}

export type JwtUser = JwtPayload;
