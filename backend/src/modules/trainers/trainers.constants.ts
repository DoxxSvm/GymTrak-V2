import { PERMISSION_CODES } from '../../common/permissions/permission-codes';

/** Maps UI toggles to `Permission.code` rows (seeded in migrations). */
export const TRAINER_PERMISSION_CODES = {
  dashboard: PERMISSION_CODES.DASHBOARD,
  payments: PERMISSION_CODES.PAYMENTS,
  members: PERMISSION_CODES.MEMBERS,
  admin: PERMISSION_CODES.ADMIN,
} as const;

export const ALL_TRAINER_PERMISSION_CODES: string[] = [
  TRAINER_PERMISSION_CODES.dashboard,
  TRAINER_PERMISSION_CODES.payments,
  TRAINER_PERMISSION_CODES.members,
  TRAINER_PERMISSION_CODES.admin,
];
