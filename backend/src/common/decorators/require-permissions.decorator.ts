import { applyDecorators, SetMetadata } from '@nestjs/common';
import type { PermissionCode } from '../permissions/permission-codes';

export const PERMISSIONS_KEY = 'rbac:permissions';

export type PermissionMode = 'all' | 'any';

export const PERMISSION_MODE_KEY = 'rbac:permission_mode';

/** Require gym-scoped permissions (owner / super-admin bypass). Default: all listed codes. */
export const RequirePermissions = (
  ...codes: PermissionCode[]
): MethodDecorator & ClassDecorator => SetMetadata(PERMISSIONS_KEY, codes);

/** Require at least one of the listed permissions. */
export const RequireAnyPermission = (
  ...codes: PermissionCode[]
): MethodDecorator & ClassDecorator =>
  applyDecorators(
    SetMetadata(PERMISSIONS_KEY, codes),
    SetMetadata(PERMISSION_MODE_KEY, 'any'),
  );
