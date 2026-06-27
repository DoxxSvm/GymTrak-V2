/**
 * Canonical permission codes (`Permission.code`). Seeded in migrations.
 * Feature gates (`GymFeatureKey`) stack on top for gym-wide module toggles.
 */
export const PERMISSION_CODES = {
  DASHBOARD: 'dashboard:access',
  PAYMENTS: 'payments:access',
  MEMBERS: 'members:manage',
  ADMIN: 'admin:access',
  LEAVE_CREATE: 'leave:create',
  LEAVE_READ: 'leave:read',
  LEAVE_UPDATE: 'leave:update',
  LEAVE_DELETE: 'leave:delete',
  LEAVE_APPROVE: 'leave:approve',
  LEAVE_REJECT: 'leave:reject',
  PRODUCT_CREATE: 'product:create',
  PRODUCT_READ: 'product:read',
  PRODUCT_UPDATE: 'product:update',
  PRODUCT_DELETE: 'product:delete',
} as const;

export type PermissionCode =
  (typeof PERMISSION_CODES)[keyof typeof PERMISSION_CODES];

export const ALL_PERMISSION_CODES: PermissionCode[] = [
  PERMISSION_CODES.DASHBOARD,
  PERMISSION_CODES.PAYMENTS,
  PERMISSION_CODES.MEMBERS,
  PERMISSION_CODES.ADMIN,
  PERMISSION_CODES.LEAVE_CREATE,
  PERMISSION_CODES.LEAVE_READ,
  PERMISSION_CODES.LEAVE_UPDATE,
  PERMISSION_CODES.LEAVE_DELETE,
  PERMISSION_CODES.LEAVE_APPROVE,
  PERMISSION_CODES.LEAVE_REJECT,
  PERMISSION_CODES.PRODUCT_CREATE,
  PERMISSION_CODES.PRODUCT_READ,
  PERMISSION_CODES.PRODUCT_UPDATE,
  PERMISSION_CODES.PRODUCT_DELETE,
];
