import { SetMetadata } from '@nestjs/common';
import { GlobalRole } from '@prisma/client';

export const ROLES_KEY = 'roles';

/** Requires JWT user to have one of these global roles (Super Admin, etc.). */
export const RequireGlobalRoles = (...roles: GlobalRole[]) =>
  SetMetadata(ROLES_KEY, roles);
