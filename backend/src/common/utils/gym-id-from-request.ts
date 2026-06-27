import { BadRequestException } from '@nestjs/common';
import { Request } from 'express';

/** Resolves gym scope from header (preferred), query, or JSON body. */
export function getGymIdFromRequest(req: Request): string {
  const raw = req.headers['x-gym-id'];
  const header =
    typeof raw === 'string'
      ? raw.trim()
      : Array.isArray(raw)
        ? raw[0]?.trim()
        : '';
  const q = req.query['gymId'] ?? req.query['gym_id'];
  const qs = typeof q === 'string' ? q : Array.isArray(q) ? q[0] : undefined;
  const body = req.body as { gymId?: string; gym_id?: string } | undefined;
  const b = body?.gymId ?? body?.gym_id;
  const gymId = header || qs || b;
  if (!gymId || typeof gymId !== 'string') {
    throw new BadRequestException(
      'gymId is required (query, JSON body, or X-Gym-Id header)',
    );
  }
  return gymId;
}
