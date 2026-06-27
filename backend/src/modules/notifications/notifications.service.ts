import { ForbiddenException, Injectable } from '@nestjs/common';
import type { Prisma } from '@prisma/client';
import { NotificationEntityType, NotificationType } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { NotificationsGateway } from './notifications.gateway';

export type NotificationFeedItem = {
  id: string;
  gymId: string;
  title: string;
  body: string;
  type: NotificationType;
  readAt: Date | null;
  createdAt: Date;
  metadata: Prisma.JsonValue | null;
  entity: { type: NotificationEntityType; id: string } | null;
  actor: {
    id: string;
    name: string | null;
    phone: string | null;
  } | null;
};

@Injectable()
export class NotificationsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gateway: NotificationsGateway,
  ) {}

  private async assertGymOwner(gymId: string, ownerId: string) {
    const gym = await this.prisma.gym.findFirst({
      where: { id: gymId, ownerId },
      select: { id: true },
    });
    if (!gym) {
      throw new ForbiddenException('Invalid gym');
    }
  }

  /**
   * Persists a notification for the gym owner and pushes it over WebSocket.
   * Used by domain event listeners (decoupled from feature modules).
   */
  async createForGymOwner(input: {
    gymId: string;
    type: NotificationType;
    title: string;
    body: string;
    entityType?: NotificationEntityType;
    entityId?: string;
    actorUserId?: string;
    metadata?: Prisma.InputJsonValue;
    dedupeKey?: string;
  }) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: input.gymId },
      select: { ownerId: true },
    });
    if (!gym) {
      return null;
    }

    if (input.dedupeKey) {
      const existing = await this.prisma.notification.findUnique({
        where: { dedupeKey: input.dedupeKey },
      });
      if (existing) {
        return existing;
      }
    }

    const row = await this.prisma.notification.create({
      data: {
        gymId: input.gymId,
        recipientUserId: gym.ownerId,
        title: input.title,
        body: input.body,
        type: input.type,
        entityType: input.entityType,
        entityId: input.entityId,
        actorUserId: input.actorUserId,
        metadata: input.metadata,
        dedupeKey: input.dedupeKey,
      },
      select: {
        id: true,
        gymId: true,
        title: true,
        body: true,
        type: true,
        entityType: true,
        entityId: true,
        readAt: true,
        createdAt: true,
        metadata: true,
        actorUserId: true,
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

    const item = this.mapFeedItem(row);
    this.gateway.emitToUser(gym.ownerId, item);
    return row;
  }

  private mapFeedItem(n: {
    id: string;
    gymId: string;
    title: string;
    body: string;
    type: NotificationType;
    entityType: NotificationEntityType | null;
    entityId: string | null;
    readAt: Date | null;
    createdAt: Date;
    metadata: Prisma.JsonValue | null;
    actorUserId: string | null;
    actor: {
      id: string;
      fullName: string | null;
      phone: string | null;
      username: string | null;
    } | null;
  }): NotificationFeedItem {
    return {
      id: n.id,
      gymId: n.gymId,
      title: n.title,
      body: n.body,
      type: n.type,
      readAt: n.readAt,
      createdAt: n.createdAt,
      metadata: n.metadata,
      entity:
        n.entityType && n.entityId
          ? { type: n.entityType, id: n.entityId }
          : null,
      actor: n.actor
        ? {
            id: n.actor.id,
            name: n.actor.fullName ?? n.actor.username ?? n.actor.phone,
            phone: n.actor.phone,
          }
        : n.actorUserId
          ? {
              id: n.actorUserId,
              name: null as string | null,
              phone: null as string | null,
            }
          : null,
    };
  }

  async feed(ownerId: string, gymId: string, limit = 20, cursor?: string) {
    await this.assertGymOwner(gymId, ownerId);
    const take = Math.min(Math.max(limit, 1), 50);

    const rows = await this.prisma.notification.findMany({
      where: { gymId, recipientUserId: ownerId },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      take: take + 1,
      ...(cursor
        ? {
            skip: 1,
            cursor: { id: cursor },
          }
        : {}),
      select: {
        id: true,
        gymId: true,
        title: true,
        body: true,
        type: true,
        entityType: true,
        entityId: true,
        readAt: true,
        createdAt: true,
        metadata: true,
        actorUserId: true,
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
      items: page.map((n) => this.mapFeedItem(n)),
      data: page.map((n) => {
        const it = this.mapFeedItem(n);
        return {
          id: it.id,
          title: it.title,
          description: it.body,
          type:
            it.type === 'PAYMENT'
              ? 'payment'
              : it.type === 'MEMBER'
                ? 'member'
                : 'system',
          created_at: it.createdAt,
          read: it.readAt != null,
        };
      }),
      nextCursor,
    };
  }

  async markRead(ownerId: string, gymId: string, notificationId: string) {
    await this.assertGymOwner(gymId, ownerId);
    await this.prisma.notification.updateMany({
      where: {
        id: notificationId,
        gymId,
        recipientUserId: ownerId,
      },
      data: { readAt: new Date() },
    });
    return { ok: true as const };
  }
}
