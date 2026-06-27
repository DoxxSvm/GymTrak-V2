import {
  BadRequestException,
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Request } from 'express';
import { Observable } from 'rxjs';

/**
 * Merges `X-Gym-Id` into `req.query.gymId` so clients can send one global header.
 * If both header and query/body `gymId` are present, they must match (strict isolation).
 */
@Injectable()
export class GymContextInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const req = context.switchToHttp().getRequest<Request>();
    const raw = req.headers['x-gym-id'];
    const header =
      typeof raw === 'string'
        ? raw.trim()
        : Array.isArray(raw)
          ? raw[0]?.trim()
          : '';
    if (!header) {
      return next.handle();
    }

    const q = req.query['gymId'];
    const qs = typeof q === 'string' ? q : Array.isArray(q) ? q[0] : undefined;
    if (qs && qs !== header) {
      throw new BadRequestException(
        'gymId query does not match X-Gym-Id header',
      );
    }
    if (!qs) {
      (req.query as Record<string, string>)['gymId'] = header;
    }

    const body = req.body;
    if (body && typeof body === 'object' && !Array.isArray(body)) {
      const gid = (body as { gymId?: string }).gymId;
      if (gid !== undefined && gid !== header) {
        throw new BadRequestException(
          'gymId body field does not match X-Gym-Id header',
        );
      }
    }

    return next.handle();
  }
}
