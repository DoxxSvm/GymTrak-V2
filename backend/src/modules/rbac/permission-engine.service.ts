import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { GlobalRole, GymFeatureKey, GymRole } from '@prisma/client';
import {
  PERMISSION_CODES,
  type PermissionCode,
} from '../../common/permissions/permission-codes';
import { PrismaService } from '../prisma/prisma.service';
import { GymFeaturesService } from '../gym-features/gym-features.service';

export type EffectivePermissionMatrix = {
  dashboard: boolean;
  payments: boolean;
  members: boolean;
  admin: boolean;
  leaveCreate: boolean;
  leaveRead: boolean;
  leaveUpdate: boolean;
  leaveDelete: boolean;
  leaveApprove: boolean;
  leaveReject: boolean;
  productCreate: boolean;
  productRead: boolean;
  productUpdate: boolean;
  productDelete: boolean;
};

export type ExpandedEffectivePermissionMatrix = EffectivePermissionMatrix & {
  dashboardView: boolean;
  dashboardNotifications: boolean;
  dashboardPaymentsWidget: boolean;
  dashboardAnalytics: boolean;
  clientRead: boolean;
  clientCreate: boolean;
  clientUpdate: boolean;
  clientDelete: boolean;
  clientDetailsRead: boolean;
  clientDetailsUpdate: boolean;
  clientDetailsDelete: boolean;
  subscriptionRead: boolean;
  subscriptionCreate: boolean;
  subscriptionRenew: boolean;
  subscriptionUpgrade: boolean;
  subscriptionFreeze: boolean;
  paymentRead: boolean;
  paymentCreate: boolean;
  paymentUpdate: boolean;
  paymentDelete: boolean;
  invoiceGenerate: boolean;
  invoiceShare: boolean;
  attendanceRead: boolean;
  biometricCreate: boolean;
  biometricDelete: boolean;
  biometricBlock: boolean;
  workoutAssign: boolean;
  dietAssign: boolean;
  progressTrack: boolean;
  leadRead: boolean;
  leadCreate: boolean;
  leadUpdate: boolean;
  leadDelete: boolean;
  leadConvert: boolean;
  planRead: boolean;
  planCreate: boolean;
  planUpdate: boolean;
  planDelete: boolean;
  planClientsView: boolean;
  trainerRead: boolean;
  trainerCreate: boolean;
  trainerUpdate: boolean;
  trainerDelete: boolean;
  trainerCredentialsManage: boolean;
  trainerPermissionsAssign: boolean;
  salaryRead: boolean;
  salaryCreate: boolean;
  salaryUpdate: boolean;
  salaryDelete: boolean;
  expenseRead: boolean;
  expenseCreate: boolean;
  expenseUpdate: boolean;
  expenseDelete: boolean;
  gymRead: boolean;
  gymUpdate: boolean;
  gymDelete: boolean;
  broadcastWhatsapp: boolean;
  broadcastMessage: boolean;
  qrView: boolean;
};

@Injectable()
export class PermissionEngineService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly features: GymFeaturesService,
  ) {}

  async isGymOwner(userId: string, gymId: string): Promise<boolean> {
    const g = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { ownerId: true },
    });
    return g?.ownerId === userId;
  }

  /** Public for dashboard / diagnostics (platform operator bypass). */
  async isPlatformSuperAdmin(userId: string): Promise<boolean> {
    const u = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    return u?.globalRole === GlobalRole.SUPER_ADMIN;
  }

  /**
   * RBAC matrix + gym feature gates. Owners bypass RBAC; feature flags still returned for UI.
   */
  async getEffective(actorUserId: string, gymId: string) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true, ownerId: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const [fPay, fDash] = await Promise.all([
      this.features.isEnabled(gymId, GymFeatureKey.payments),
      this.features.isEnabled(gymId, GymFeatureKey.dashboard),
    ]);

    const gymFeatures = { payments: fPay, dashboard: fDash };

    if (
      gym.ownerId === actorUserId ||
      (await this.isPlatformSuperAdmin(actorUserId))
    ) {
      const all: EffectivePermissionMatrix = {
        dashboard: true,
        payments: true,
        members: true,
        admin: true,
        leaveCreate: true,
        leaveRead: true,
        leaveUpdate: true,
        leaveDelete: true,
        leaveApprove: true,
        leaveReject: true,
        productCreate: true,
        productRead: true,
        productUpdate: true,
        productDelete: true,
      };
      return {
        gymId,
        role: 'OWNER' as const,
        permissions: all,
        gymFeatures,
        effective: all,
      };
    }

    const gu = await this.prisma.gymUser.findFirst({
      where: { gymId, userId: actorUserId, isActive: true },
      select: { id: true, role: true },
    });
    if (!gu) {
      throw new ForbiddenException('No access to this gym');
    }

    if (gu.role === GymRole.MEMBER) {
      const z: EffectivePermissionMatrix = {
        dashboard: false,
        payments: false,
        members: false,
        admin: false,
        leaveCreate: false,
        leaveRead: false,
        leaveUpdate: false,
        leaveDelete: false,
        leaveApprove: false,
        leaveReject: false,
        productCreate: false,
        productRead: true,
        productUpdate: false,
        productDelete: false,
      };
      return {
        gymId,
        role: 'MEMBER' as const,
        permissions: z,
        gymFeatures,
        effective: z,
      };
    }

    const rows = await this.prisma.gymUserPermission.findMany({
      where: { gymUserId: gu.id },
      select: { permission: { select: { code: true } } },
    });
    const codes = new Set(rows.map((r) => r.permission.code));

    const permissions: EffectivePermissionMatrix = {
      dashboard: codes.has(PERMISSION_CODES.DASHBOARD),
      payments: codes.has(PERMISSION_CODES.PAYMENTS),
      members: codes.has(PERMISSION_CODES.MEMBERS),
      admin: codes.has(PERMISSION_CODES.ADMIN),
      leaveCreate: codes.has(PERMISSION_CODES.LEAVE_CREATE),
      leaveRead: codes.has(PERMISSION_CODES.LEAVE_READ),
      leaveUpdate: codes.has(PERMISSION_CODES.LEAVE_UPDATE),
      leaveDelete: codes.has(PERMISSION_CODES.LEAVE_DELETE),
      leaveApprove: codes.has(PERMISSION_CODES.LEAVE_APPROVE),
      leaveReject: codes.has(PERMISSION_CODES.LEAVE_REJECT),
      productCreate: codes.has(PERMISSION_CODES.PRODUCT_CREATE),
      productRead: codes.has(PERMISSION_CODES.PRODUCT_READ),
      productUpdate: codes.has(PERMISSION_CODES.PRODUCT_UPDATE),
      productDelete: codes.has(PERMISSION_CODES.PRODUCT_DELETE),
    };

    const effective: EffectivePermissionMatrix = {
      dashboard: permissions.dashboard && fDash,
      payments: permissions.payments && fPay,
      members: permissions.members,
      admin: permissions.admin,
      leaveCreate: permissions.leaveCreate,
      leaveRead: permissions.leaveRead,
      leaveUpdate: permissions.leaveUpdate,
      leaveDelete: permissions.leaveDelete,
      leaveApprove: permissions.leaveApprove,
      leaveReject: permissions.leaveReject,
      productCreate: permissions.productCreate,
      productRead: permissions.productRead,
      productUpdate: permissions.productUpdate,
      productDelete: permissions.productDelete,
    };

    return {
      gymId,
      role: gu.role,
      permissions,
      gymFeatures,
      effective,
    };
  }

  /**
   * Owner/super-admin, or staff with approve/reject/delete leave permissions (gym queue).
   */
  async canViewAllLeaves(actorUserId: string, gymId: string): Promise<boolean> {
    if (
      (await this.isGymOwner(actorUserId, gymId)) ||
      (await this.isPlatformSuperAdmin(actorUserId))
    ) {
      return true;
    }
    for (const code of [
      PERMISSION_CODES.LEAVE_APPROVE,
      PERMISSION_CODES.LEAVE_REJECT,
      PERMISSION_CODES.LEAVE_DELETE,
    ] as const) {
      if (await this.staffHasEffectivePermission(actorUserId, gymId, code)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Owner/super-admin, or staff with any product write permission (catalog management UI).
   */
  async hasProductManagement(
    actorUserId: string,
    gymId: string,
  ): Promise<boolean> {
    if (
      (await this.isGymOwner(actorUserId, gymId)) ||
      (await this.isPlatformSuperAdmin(actorUserId))
    ) {
      return true;
    }
    for (const code of [
      PERMISSION_CODES.PRODUCT_CREATE,
      PERMISSION_CODES.PRODUCT_UPDATE,
      PERMISSION_CODES.PRODUCT_DELETE,
    ] as const) {
      if (await this.staffHasEffectivePermission(actorUserId, gymId, code)) {
        return true;
      }
    }
    return false;
  }

  async assertOwnerOrPermission(
    actorUserId: string,
    gymId: string,
    code: PermissionCode,
  ): Promise<void> {
    await this.assertOwnerOrPermissionMode(actorUserId, gymId, [code], 'all');
  }

  async assertOwnerOrPermissionMode(
    actorUserId: string,
    gymId: string,
    codes: PermissionCode[],
    mode: 'all' | 'any',
  ): Promise<void> {
    if (codes.length === 0) {
      return;
    }
    if (
      (await this.isGymOwner(actorUserId, gymId)) ||
      (await this.isPlatformSuperAdmin(actorUserId))
    ) {
      return;
    }
    const checks = await Promise.all(
      codes.map((c) => this.staffHasEffectivePermission(actorUserId, gymId, c)),
    );
    const ok = mode === 'all' ? checks.every(Boolean) : checks.some(Boolean);
    if (!ok) {
      throw new ForbiddenException('Insufficient permissions for this action');
    }
  }

  /** Default permission codes suggested for a gym role (TRAINER / STAFF). */
  async getRoleDefaults(gymRole: GymRole) {
    if (gymRole === GymRole.OWNER || gymRole === GymRole.MEMBER) {
      throw new BadRequestException('role must be TRAINER or STAFF');
    }
    const rows = await this.prisma.gymRolePermissionDefault.findMany({
      where: { gymRole },
      include: { permission: { select: { code: true, description: true } } },
    });
    return {
      gymRole,
      permissionCodes: rows.map((r) => r.permission.code),
      details: rows.map((r) => ({
        code: r.permission.code,
        description: r.permission.description,
      })),
    };
  }

  expandEffectivePermissions(
    base: EffectivePermissionMatrix,
  ): ExpandedEffectivePermissionMatrix {
    const dashboard = base.dashboard;
    const members = base.members;
    const payments = base.payments;
    const admin = base.admin;
    return {
      ...base,
      dashboardView: dashboard,
      dashboardNotifications: dashboard,
      dashboardPaymentsWidget: dashboard && payments,
      dashboardAnalytics: dashboard,
      clientRead: members,
      clientCreate: members,
      clientUpdate: members,
      clientDelete: members,
      clientDetailsRead: members,
      clientDetailsUpdate: members,
      clientDetailsDelete: members,
      subscriptionRead: members,
      subscriptionCreate: members,
      subscriptionRenew: members,
      subscriptionUpgrade: members,
      subscriptionFreeze: members,
      paymentRead: payments,
      paymentCreate: payments,
      paymentUpdate: payments,
      paymentDelete: payments,
      invoiceGenerate: payments,
      invoiceShare: payments,
      attendanceRead: members,
      biometricCreate: members,
      biometricDelete: members,
      biometricBlock: members,
      workoutAssign: members,
      dietAssign: members,
      progressTrack: members,
      leadRead: members,
      leadCreate: members,
      leadUpdate: members,
      leadDelete: members,
      leadConvert: members,
      planRead: members,
      planCreate: admin,
      planUpdate: admin,
      planDelete: admin,
      planClientsView: members,
      trainerRead: admin,
      trainerCreate: admin,
      trainerUpdate: admin,
      trainerDelete: admin,
      trainerCredentialsManage: admin,
      trainerPermissionsAssign: admin,
      salaryRead: admin,
      salaryCreate: admin,
      salaryUpdate: admin,
      salaryDelete: admin,
      expenseRead: payments,
      expenseCreate: payments,
      expenseUpdate: payments,
      expenseDelete: payments,
      gymRead: true,
      gymUpdate: admin,
      gymDelete: admin,
      broadcastWhatsapp: admin,
      broadcastMessage: admin,
      qrView: members || admin,
    };
  }

  private async staffHasEffectivePermission(
    actorUserId: string,
    gymId: string,
    code: PermissionCode,
  ): Promise<boolean> {
    const gu = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId: actorUserId,
        isActive: true,
        role: { in: [GymRole.STAFF, GymRole.TRAINER] },
      },
      select: { id: true },
    });
    if (!gu) {
      return false;
    }
    const row = await this.prisma.gymUserPermission.findFirst({
      where: {
        gymUserId: gu.id,
        permission: { code },
      },
    });
    if (!row) {
      return false;
    }
    if (code === PERMISSION_CODES.PAYMENTS) {
      return this.features.isEnabled(gymId, GymFeatureKey.payments);
    }
    if (code === PERMISSION_CODES.DASHBOARD) {
      return this.features.isEnabled(gymId, GymFeatureKey.dashboard);
    }
    return true;
  }
}
