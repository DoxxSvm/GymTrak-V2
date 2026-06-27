import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import {
  PERMISSION_MODE_KEY,
  PERMISSIONS_KEY,
  type PermissionMode,
} from '../decorators/require-permissions.decorator';
import { getGymIdFromRequest } from '../utils/gym-id-from-request';
import type { JwtUser } from '../../modules/auth/types/jwt-user.type';
import type { PermissionCode } from '../permissions/permission-codes';
import { PermissionEngineService } from '../../modules/rbac/permission-engine.service';

/**
 * Enforces `RequirePermissions` / `RequireAnyPermission` using gym context.
 * Owner and platform super-admin bypass checks.
 */
@Injectable()
export class PermissionsGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly engine: PermissionEngineService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const codes = this.reflector.getAllAndOverride<
      PermissionCode[] | undefined
    >(PERMISSIONS_KEY, [context.getHandler(), context.getClass()]);
    if (!codes?.length) {
      return true;
    }
    const mode =
      this.reflector.getAllAndOverride<PermissionMode | undefined>(
        PERMISSION_MODE_KEY,
        [context.getHandler(), context.getClass()],
      ) ?? 'all';
    const req = context.switchToHttp().getRequest();
    const user = req.user as JwtUser | undefined;
    if (!user?.sub) {
      return false;
    }
    /** Owner home dashboard: JWT only; gym list + access enforced in DashboardService */
    const mobileView = req.query?.['mobileView'];
    if (mobileView === 'owner') {
      return true;
    }
    const gymId = getGymIdFromRequest(req);
    await this.engine.assertOwnerOrPermissionMode(user.sub, gymId, codes, mode);
    return true;
  }
}
