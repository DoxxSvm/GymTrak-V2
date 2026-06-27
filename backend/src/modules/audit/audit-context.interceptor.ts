import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import type { Request } from 'express';
import { Observable } from 'rxjs';
import { auditContextStorage } from './audit-context';

/**
 * Binds request IP / User-Agent / request id into AsyncLocalStorage for
 * {@link AuditService} (enterprise "who did what from where").
 */
@Injectable()
export class AuditContextInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const req = context.switchToHttp().getRequest<Request>();
    const xf = req.headers['x-forwarded-for'];
    const forwarded =
      typeof xf === 'string'
        ? xf.split(',')[0]?.trim()
        : Array.isArray(xf)
          ? xf[0]?.trim()
          : '';
    const ip =
      forwarded ||
      req.ip ||
      (req.socket?.remoteAddress as string | undefined) ||
      undefined;
    const ua = req.headers['user-agent'];
    const userAgent = typeof ua === 'string' ? ua : undefined;
    const requestId = randomUUID();

    return auditContextStorage.run({ ip, userAgent, requestId }, () =>
      next.handle(),
    );
  }
}
