import { Injectable, Logger } from '@nestjs/common';
import { AuditAction, AuditEntityType, Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { auditContextStorage } from './audit-context';

export type AuditLogInput = {
  gymId: string;
  actorUserId: string;
  action: AuditAction;
  entityType?: AuditEntityType;
  entityId?: string;
  metadata?: Prisma.InputJsonValue;
};

@Injectable()
export class AuditService {
  private readonly logger = new Logger(AuditService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  /**
   * Persists an immutable audit row. Uses request context (IP, UA, request id)
   * when {@link AuditContextInterceptor} is active.
   */
  async log(input: AuditLogInput): Promise<void> {
    const ctx = auditContextStorage.getStore();
    try {
      await this.prisma.auditLog.create({
        data: {
          gymId: input.gymId,
          actorUserId: input.actorUserId,
          action: input.action,
          entityType: input.entityType,
          entityId: input.entityId,
          metadata: input.metadata,
          ipAddress: ctx?.ip ? ctx.ip.slice(0, 64) : null,
          userAgent: ctx?.userAgent ? ctx.userAgent.slice(0, 512) : null,
          requestId: ctx?.requestId,
        },
      });
    } catch (e) {
      this.logger.error(
        `Audit log failed: ${(e as Error).message}`,
        (e as Error).stack,
      );
      throw e;
    }
  }

  async listForGymWithAccess(
    actorUserId: string,
    gymId: string,
    limit: number,
    cursor?: string,
    action?: AuditAction,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    return this.listForGym(gymId, { limit, cursor, action });
  }

  async listForGym(
    gymId: string,
    options: {
      limit: number;
      cursor?: string;
      action?: AuditAction;
    },
  ) {
    const take = Math.min(Math.max(options.limit, 1), 100);
    const rows = await this.prisma.auditLog.findMany({
      where: {
        gymId,
        ...(options.action ? { action: options.action } : {}),
      },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      take: take + 1,
      ...(options.cursor
        ? {
            skip: 1,
            cursor: { id: options.cursor },
          }
        : {}),
      select: {
        id: true,
        gymId: true,
        actorUserId: true,
        action: true,
        entityType: true,
        entityId: true,
        metadata: true,
        ipAddress: true,
        userAgent: true,
        requestId: true,
        createdAt: true,
        actor: {
          select: {
            id: true,
            fullName: true,
            phone: true,
            username: true,
          },
        },
      },
    });

    let nextCursor: string | null = null;
    let page = rows;
    if (rows.length > take) {
      nextCursor = rows[take - 1].id;
      page = rows.slice(0, take);
    }

    return {
      items: page.map((r) => ({
        id: r.id,
        gymId: r.gymId,
        action: r.action,
        entityType: r.entityType,
        entityId: r.entityId,
        metadata: r.metadata,
        ipAddress: r.ipAddress,
        userAgent: r.userAgent,
        requestId: r.requestId,
        createdAt: r.createdAt,
        actor: {
          id: r.actor.id,
          name: r.actor.fullName ?? r.actor.username ?? r.actor.phone,
          phone: r.actor.phone,
        },
      })),
      nextCursor,
    };
  }
}
