import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
} from '@nestjs/common';
import { GlobalRole } from '@prisma/client';
import type { JwtUser } from '../../modules/auth/types/jwt-user.type';

/**
 * Allows only {@link GlobalRole.SUPER_ADMIN}. Requires JWT auth (global {@link JwtAuthGuard}).
 */
@Injectable()
export class AdminGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<{ user?: JwtUser }>();
    const user = request.user;
    if (!user || user.globalRole !== GlobalRole.SUPER_ADMIN) {
      throw new ForbiddenException();
    }
    return true;
  }
}
