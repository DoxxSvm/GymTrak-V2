import { GymRole } from '@prisma/client';
import { PrismaService } from 'src/modules/prisma/prisma.service';

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

export const ALL_PERMISSIONS = [
  'dashboard',
  'payments',
  'members',
  'admin',
  'leaveCreate',
  'leaveRead',
  'leaveUpdate',
  'leaveDelete',
  'leaveApprove',
  'leaveReject',
  'productCreate',
  'productRead',
  'productUpdate',
  'productDelete',
  'dashboardView',
  'dashboardNotifications',
  'dashboardPaymentsWidget',
  'dashboardAnalytics',
  'clientRead',
  'clientCreate',
  'clientUpdate',
  'clientDelete',
  'clientDetailsRead',
  'clientDetailsUpdate',
  'clientDetailsDelete',
  'subscriptionRead',
  'subscriptionCreate',
  'subscriptionRenew',
  'subscriptionUpgrade',
  'subscriptionFreeze',
  'paymentRead',
  'paymentCreate',
  'paymentUpdate',
  'paymentDelete',
  'invoiceGenerate',
  'invoiceShare',
  'attendanceRead',
  'biometricCreate',
  'biometricDelete',
  'biometricBlock',
  'workoutAssign',
  'dietAssign',
  'progressTrack',
  'leadRead',
  'leadCreate',
  'leadUpdate',
  'leadDelete',
  'leadConvert',
  'planRead',
  'planCreate',
  'planUpdate',
  'planDelete',
  'planClientsView',
  'trainerRead',
  'trainerCreate',
  'trainerUpdate',
  'trainerDelete',
  'trainerCredentialsManage',
  'trainerPermissionsAssign',
  'salaryRead',
  'salaryCreate',
  'salaryUpdate',
  'salaryDelete',
  'expenseRead',
  'expenseCreate',
  'expenseUpdate',
  'expenseDelete',
  'gymRead',
  'gymUpdate',
  'gymDelete',
  'broadcastWhatsapp',
  'broadcastMessage',
  'qrView',
];

export const getTrainerPermissions = async (
  prisma: PrismaService,
  trainer_id: string,
  gym_id: string,
) => {
  const gymUser = await prisma.gymUser.findFirst({
    where: {
      userId: trainer_id,
      gymId: gym_id,
    },
    select: { id: true, role: true },
  });
  if (!gymUser) {
    return [];
  }

  if (gymUser.role === GymRole.OWNER) {
    return ALL_PERMISSIONS;
  }
  const permissionsRow = (await (prisma as any).trainerProfile.findUnique({
    where: { gymUserId: gymUser.id },
    select: { trainerPermission: true },
  })) as { trainerPermission?: string[] } | null;
  return permissionsRow?.trainerPermission ?? [];
};

export const createTrainerPermissions = async (
  prisma: PrismaService,
  trainer_id: string,
  permissions: string[],
) => {
  await (prisma as any).trainerProfile.create({
    where: { gymUserId: trainer_id },
    update: { trainerPermission: permissions },
    create: { gymUserId: trainer_id, trainerPermission: permissions },
  });
};

export const updateTrainerPermissions = async (
  prisma: PrismaService,
  trainer_id: string,
  permissions: string[],
) => {
  await (prisma as any).trainerProfile.update({
    where: { gymUserId: trainer_id },
    data: { trainerPermission: permissions },
  });
};
