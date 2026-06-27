import {
  ExecutionContext,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Reflector } from '@nestjs/core';
import { AuthGuard } from '@nestjs/passport';
import type { Request } from 'express';
import { isObservable, lastValueFrom } from 'rxjs';
import type { JwtUser } from '../../modules/auth/types/jwt-user.type';
import { PrismaService } from '../../modules/prisma/prisma.service';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';
import { REQUIRE_BEARER_AUTH_KEY } from '../decorators/require-bearer-auth.decorator';

/**
 * Global auth: non-{@link Public} routes need a valid JWT in `Authorization: Bearer …`,
 * except some owner signup POSTs ({@link isSignupFlowPost}) which may infer user from
 * `AUTH_DEV_USER_ID` or the first ACTIVE user when Bearer is omitted.
 * Routes marked {@link RequireBearerAuth} always require `Authorization: Bearer …` (no dev fallback).
 * **`POST …/user/select-role` always requires Bearer** (temp signup or access token) so
 * `request.user` matches the JWT instead of a dev fallback user.
 * See also {@link isJwtAuthDisabled}.
 */
@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  private readonly logger = new Logger(JwtAuthGuard.name);

  constructor(
    private readonly reflector: Reflector,
    private readonly config: ConfigService,
    private readonly prisma: PrismaService,
  ) {
    super();
  }

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) {
      return true;
    }

    const requireBearerAuth = this.reflector.getAllAndOverride<boolean>(
      REQUIRE_BEARER_AUTH_KEY,
      [context.getHandler(), context.getClass()],
    );

    const request = context.switchToHttp().getRequest<Request>();
    const authHeader = request.headers?.authorization;
    const hasBearer =
      typeof authHeader === 'string' && /^Bearer\s+\S+/i.test(authHeader);

    if (this.isPostOwnerAppSelectRole(request) && !hasBearer) {
      throw new UnauthorizedException(
        'Authorization: Bearer <tempToken|access_token> is required for POST /user/select-role.',
      );
    }

    if (hasBearer) {
      return this.runJwtValidation(context);
    }

    if (requireBearerAuth) {
      throw new UnauthorizedException(
        'Authorization: Bearer <access_token> is required.',
      );
    }

    /** Owner signup/onboarding: no Bearer; user from AUTH_DEV_USER_ID or first ACTIVE user. */
    if (this.isSignupFlowPost(request)) {
      if (
        await this.attachUserWithoutBearer(request, { forSignupFlow: true })
      ) {
        return true;
      }
    }

    if (this.isJwtAuthDisabled()) {
      const ok = await this.attachUserWithoutBearer(request);
      if (ok) {
        return true;
      }
      this.logger.warn(
        'JWT auth is disabled for this environment but no ACTIVE user could be resolved; set AUTH_DEV_USER_ID or use Bearer token.',
      );
      throw new UnauthorizedException(
        'No user context: set AUTH_DEV_USER_ID, seed an ACTIVE user, or send Authorization: Bearer <access_token>.',
      );
    }

    return this.runJwtValidation(context);
  }

  /** POST /api/v1/gym (owner app) — not POST /gyms (see {@link isPostGymsCollectionCreate}). */
  private isPostOwnerAppGymEndpoint(req: Request): boolean {
    if (req.method !== 'POST') {
      return false;
    }
    const path = (req.originalUrl || req.url || '').split('?')[0];
    if (path.includes('/gyms/') || path.endsWith('/gyms')) {
      return false;
    }
    return /\/gym\/?$/.test(path);
  }

  /** POST /api/v1/gyms — create gym (`GymsController.create`), not nested routes like `/gyms/:id/config`. */
  private isPostGymsCollectionCreate(req: Request): boolean {
    if (req.method !== 'POST') {
      return false;
    }
    const path = (req.originalUrl || req.url || '').split('?')[0];
    return /\/gyms\/?$/.test(path);
  }

  /** `POST …/user/select-role` (global prefix may prepend `/api/v1`). */
  private isPostOwnerAppSelectRole(req: Request): boolean {
    if (req.method !== 'POST') {
      return false;
    }
    const path = (req.originalUrl || req.url || '').split('?')[0];
    return /\/user\/select-role\/?$/.test(path);
  }

  /** Owner app signup: select role, set gym, or create gym — temp JWT only on these paths; see guard body for Bearer rules. */
  private isSignupFlowPost(req: Request): boolean {
    if (req.method !== 'POST') {
      return false;
    }
    const path = (req.originalUrl || req.url || '').split('?')[0];
    if (/\/user\/select-role\/?$/.test(path)) {
      return true;
    }
    return (
      this.isPostOwnerAppGymEndpoint(req) ||
      this.isPostGymsCollectionCreate(req)
    );
  }

  /** When true, missing Bearer is allowed and user is inferred (dev / explicit opt-out). */
  private isJwtAuthDisabled(): boolean {
    const raw = this.config.get<boolean | string | undefined>(
      'DISABLE_JWT_AUTH',
    );
    if (raw === false || raw === 'false' || raw === '0') {
      return false;
    }
    if (raw === true || raw === 'true' || raw === '1') {
      return true;
    }
    return this.config.get<string>('NODE_ENV') === 'development';
  }

  private async attachUserWithoutBearer(
    request: Request,
    options?: { forSignupFlow?: boolean },
  ): Promise<boolean> {
    const devUserId = this.config.get<string>('AUTH_DEV_USER_ID')?.trim();
    let row = devUserId
      ? await this.prisma.user.findUnique({
          where: { id: devUserId },
          select: { id: true, phone: true, globalRole: true, status: true },
        })
      : null;

    const isDev = this.config.get<string>('NODE_ENV') === 'development';
    const allowFirstActiveUser = isDev || options?.forSignupFlow === true;
    if ((!row || row.status !== 'ACTIVE') && allowFirstActiveUser) {
      row = await this.prisma.user.findFirst({
        where: { status: 'ACTIVE' },
        orderBy: { createdAt: 'asc' },
        select: { id: true, phone: true, globalRole: true, status: true },
      });
    }

    if (!row || row.status !== 'ACTIVE') {
      return false;
    }

    const user: JwtUser = {
      sub: row.id,
      phone: row.phone,
      globalRole: row.globalRole,
    };
    request.user = user;
    return true;
  }

  handleRequest<TUser = JwtUser>(
    err: unknown,
    user: TUser | false,
    info: unknown,
    context: ExecutionContext,
    // Passport passes status; we only use err/user/info/context.
    // eslint-disable-next-line @typescript-eslint/no-unused-vars -- signature parity with AuthGuard
    status?: unknown,
  ): TUser {
    if (err || !user) {
      throw err instanceof Error ? err : new UnauthorizedException();
    }
    const jwtUser = user as unknown as JwtUser;
    const req = context.switchToHttp().getRequest<Request>();
    if (jwtUser.isTemp === true && !this.isSignupFlowPost(req)) {
      throw new UnauthorizedException(
        'Temporary signup token is only valid for POST /user/select-role, POST /gym, and POST /gyms',
      );
    }
    return user;
  }

  private runJwtValidation(context: ExecutionContext): Promise<boolean> {
    const result = super.canActivate(context);
    if (isObservable(result)) {
      return lastValueFrom(result);
    }
    if (result instanceof Promise) {
      return result;
    }
    return Promise.resolve(result);
  }
}
