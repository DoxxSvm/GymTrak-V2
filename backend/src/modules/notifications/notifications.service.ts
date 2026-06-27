import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import type { Prisma } from '@prisma/client';
import {
  GlobalRole,
  GymRole,
  NotificationEntityType,
  NotificationType,
} from '@prisma/client';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PrismaService } from '../prisma/prisma.service';
import { FirebasePushService } from './firebase-push.service';
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
  /** Member the notification is about (e.g. plan extended for this person). */
  member: {
    gymUserId: string;
    name: string | null;
  } | null;
  /** Mobile routing hint from `metadata.deepLink.screen` (e.g. `trainer-salary`, `staff-salary`). */
  screen: string | null;
};

@Injectable()
export class NotificationsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gateway: NotificationsGateway,
    private readonly firebasePush: FirebasePushService,
  ) {}

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
      select: {
        ownerId: true,
        owner: {
          select: { fcmDeviceToken: true },
        },
      },
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

    const item = this.mapFeedItem(row, this.memberFromStoredMetadata(row.metadata));
    this.gateway.emitToUser(gym.ownerId, item);

    const fcmToken = gym.owner.fcmDeviceToken;
    if (fcmToken && this.firebasePush.isEnabled) {
      void this.deliverFcmToOwner(fcmToken, gym.ownerId, row.id, input);
    }

    return row;
  }

  /** Stores or clears the caller’s FCM token (one active token per user — last write wins). */
  async registerDeviceToken(userId: string, token: string | undefined) {
    const trimmed = token?.trim();
    await this.prisma.user.update({
      where: { id: userId },
      data: {
        fcmDeviceToken: trimmed || null,
        fcmTokenUpdatedAt: trimmed ? new Date() : null,
      },
    });
    return { ok: true as const };
  }

  private async deliverFcmToOwner(
    token: string,
    ownerUserId: string,
    notificationId: string,
    input: {
      gymId: string;
      type: NotificationType;
      title: string;
      body: string;
      entityType?: NotificationEntityType;
      entityId?: string;
    },
  ) {
    const data: Record<string, string> = {
      notificationId,
      gymId: input.gymId,
      type: input.type,
    };
    if (input.entityType && input.entityId) {
      data.entityType = input.entityType;
      data.entityId = input.entityId;
    }

    const result = await this.firebasePush.sendToDevice(token, {
      title: input.title,
      body: input.body,
      data,
    });

    if (result.ok === false && result.shouldClearToken) {
      await this.prisma.user.updateMany({
        where: { id: ownerUserId, fcmDeviceToken: token },
        data: { fcmDeviceToken: null, fcmTokenUpdatedAt: null },
      });
    }
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
  },
    member?: { gymUserId: string; name: string | null } | null,
    screen?: string | null,
  ): NotificationFeedItem {
    const resolvedScreen =
      screen ?? this.screenFromMetadata(n.metadata) ?? null;
    return {
      id: n.id,
      gymId: n.gymId,
      title: n.title,
      body: n.body,
      type: n.type,
      readAt: n.readAt,
      createdAt: n.createdAt,
      metadata: this.metadataWithScreen(n.metadata, resolvedScreen),
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
      member: member ?? null,
      screen: resolvedScreen,
    };
  }

  private screenFromMetadata(metadata: Prisma.JsonValue | null): string | null {
    if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) {
      return null;
    }
    const m = metadata as Record<string, unknown>;
    if (typeof m.screen === 'string' && m.screen.trim()) {
      return m.screen.trim();
    }
    const deepLink = m.deepLink;
    if (
      deepLink &&
      typeof deepLink === 'object' &&
      !Array.isArray(deepLink) &&
      typeof (deepLink as Record<string, unknown>).screen === 'string'
    ) {
      return ((deepLink as Record<string, unknown>).screen as string).trim();
    }
    return null;
  }

  private metadataWithScreen(
    metadata: Prisma.JsonValue | null,
    screen: string | null,
  ): Prisma.JsonValue | null {
    if (!screen) {
      return metadata;
    }
    const base =
      metadata && typeof metadata === 'object' && !Array.isArray(metadata)
        ? { ...(metadata as Record<string, unknown>) }
        : {};
    const existingDeepLink = base.deepLink;
    const deepLink =
      existingDeepLink &&
      typeof existingDeepLink === 'object' &&
      !Array.isArray(existingDeepLink)
        ? { ...(existingDeepLink as Record<string, unknown>) }
        : { params: {} };
    return {
      ...base,
      screen,
      deepLink: { ...deepLink, screen },
    } as Prisma.JsonValue;
  }

  private salaryScreenForPayeeRole(role: GymRole): string {
    return role === GymRole.STAFF ? 'staff-salary' : 'trainer-salary';
  }

  private async resolveScreensForFeed(
    rows: Array<{
      id: string;
      metadata: Prisma.JsonValue | null;
    }>,
  ): Promise<Map<string, string>> {
    const out = new Map<string, string>();
    const gymUserIds = new Set<string>();
    const pending: Array<{ notifId: string; gymUserId: string }> = [];

    for (const n of rows) {
      const existing = this.screenFromMetadata(n.metadata);
      if (
        existing === 'staff-salary' ||
        existing === 'trainer-salary'
      ) {
        if (existing === 'trainer-salary') {
          const hint = this.metadataMemberHint(n.metadata);
          const payeeRole =
            this.metadataPayeeRole(n.metadata) ?? null;
          if (payeeRole === 'STAFF' || payeeRole === GymRole.STAFF) {
            out.set(n.id, 'staff-salary');
            continue;
          }
          if (hint.gymUserId) {
            pending.push({ notifId: n.id, gymUserId: hint.gymUserId });
            gymUserIds.add(hint.gymUserId);
            continue;
          }
        }
        out.set(n.id, existing);
        continue;
      }

      const hint = this.metadataMemberHint(n.metadata);
      const salaryPaymentId = this.metadataSalaryPaymentId(n.metadata);
      if (salaryPaymentId && hint.gymUserId) {
        pending.push({ notifId: n.id, gymUserId: hint.gymUserId });
        gymUserIds.add(hint.gymUserId);
      }
    }

    if (gymUserIds.size) {
      const payees = await this.prisma.gymUser.findMany({
        where: { id: { in: [...gymUserIds] } },
        select: { id: true, role: true },
      });
      const roleById = new Map(payees.map((p) => [p.id, p.role]));
      for (const { notifId, gymUserId } of pending) {
        const role = roleById.get(gymUserId);
        if (role === GymRole.TRAINER || role === GymRole.STAFF) {
          out.set(notifId, this.salaryScreenForPayeeRole(role));
        }
      }
    }

    return out;
  }

  private metadataPayeeRole(
    metadata: Prisma.JsonValue | null,
  ): string | null {
    if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) {
      return null;
    }
    const role = (metadata as Record<string, unknown>).payeeRole;
    return typeof role === 'string' ? role : null;
  }

  private metadataSalaryPaymentId(
    metadata: Prisma.JsonValue | null,
  ): string | null {
    if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) {
      return null;
    }
    const id = (metadata as Record<string, unknown>).salaryPaymentId;
    return typeof id === 'string' ? id : null;
  }

  private userDisplayName(user: {
    fullName: string | null;
    username: string | null;
    phone: string | null;
  }): string | null {
    return (
      user.fullName?.trim() ||
      user.username?.trim() ||
      user.phone?.trim() ||
      null
    );
  }

  private memberFromStoredMetadata(
    metadata: Prisma.JsonValue | null,
  ): { gymUserId: string; name: string | null } | null {
    const hint = this.metadataMemberHint(metadata);
    if (!hint.gymUserId) {
      return null;
    }
    return {
      gymUserId: hint.gymUserId,
      name: hint.memberName ?? null,
    };
  }

  private metadataMemberHint(metadata: Prisma.JsonValue | null): {
    gymUserId?: string;
    memberName?: string;
  } {
    if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) {
      return {};
    }
    const m = metadata as Record<string, unknown>;
    const gymUserId =
      typeof m.gymUserId === 'string'
        ? m.gymUserId
        : typeof m.deepLink === 'object' &&
            m.deepLink !== null &&
            !Array.isArray(m.deepLink)
          ? (() => {
              const params = (m.deepLink as Record<string, unknown>).params;
              return typeof params === 'object' &&
                params !== null &&
                !Array.isArray(params) &&
                typeof (params as Record<string, unknown>).gymUserId === 'string'
                ? ((params as Record<string, unknown>).gymUserId as string)
                : undefined;
            })()
          : undefined;
    const memberName =
      typeof m.memberName === 'string' ? m.memberName.trim() : undefined;
    return { gymUserId, memberName };
  }

  private async resolveMembersForFeed(
    rows: Array<{
      id: string;
      entityType: NotificationEntityType | null;
      entityId: string | null;
      metadata: Prisma.JsonValue | null;
    }>,
  ): Promise<Map<string, { gymUserId: string; name: string | null }>> {
    const out = new Map<string, { gymUserId: string; name: string | null }>();
    const gymUserIdsToLoad = new Set<string>();
    const subscriptionIds: string[] = [];

    for (const n of rows) {
      const hint = this.metadataMemberHint(n.metadata);
      if (hint.memberName && !hint.gymUserId) {
        out.set(n.id, { gymUserId: '', name: hint.memberName });
        continue;
      }
      if (hint.gymUserId && hint.memberName) {
        out.set(n.id, { gymUserId: hint.gymUserId, name: hint.memberName });
        continue;
      }
      if (hint.gymUserId) {
        gymUserIdsToLoad.add(hint.gymUserId);
        out.set(n.id, { gymUserId: hint.gymUserId, name: null });
        continue;
      }
      if (
        (n.entityType === NotificationEntityType.MEMBER ||
          n.entityType === NotificationEntityType.TRAINER) &&
        n.entityId
      ) {
        gymUserIdsToLoad.add(n.entityId);
        out.set(n.id, { gymUserId: n.entityId, name: null });
      } else if (
        n.entityType === NotificationEntityType.MEMBER_SUBSCRIPTION &&
        n.entityId
      ) {
        subscriptionIds.push(n.entityId);
      }
    }

    if (subscriptionIds.length) {
      const subs = await this.prisma.memberSubscription.findMany({
        where: { id: { in: subscriptionIds } },
        select: {
          id: true,
          gymUserId: true,
          gymUser: {
            select: {
              user: {
                select: { fullName: true, username: true, phone: true },
              },
            },
          },
        },
      });
      const subById = new Map(subs.map((s) => [s.id, s]));
      for (const n of rows) {
        if (n.entityType !== NotificationEntityType.MEMBER_SUBSCRIPTION) {
          continue;
        }
        const sub = n.entityId ? subById.get(n.entityId) : undefined;
        if (!sub) {
          continue;
        }
        const name = this.userDisplayName(sub.gymUser.user);
        out.set(n.id, { gymUserId: sub.gymUserId, name });
        gymUserIdsToLoad.delete(sub.gymUserId);
      }
    }

    if (gymUserIdsToLoad.size) {
      const members = await this.prisma.gymUser.findMany({
        where: { id: { in: [...gymUserIdsToLoad] } },
        select: {
          id: true,
          user: { select: { fullName: true, username: true, phone: true } },
        },
      });
      const nameByGymUserId = new Map(
        members.map((m) => [m.id, this.userDisplayName(m.user)]),
      );
      for (const [notifId, entry] of out) {
        if (entry.name) {
          continue;
        }
        const name = nameByGymUserId.get(entry.gymUserId) ?? null;
        out.set(notifId, { gymUserId: entry.gymUserId, name });
      }
    }

    return out;
  }

  async feed(
    recipientUserId: string,
    limit = 20,
    cursor?: string,
    gymId?: string,
  ) {
    const take = Math.min(Math.max(limit, 1), 50);

    const rows = await this.prisma.notification.findMany({
      where: {
        recipientUserId,
        ...(gymId ? { gymId } : {}),
      },
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

    const [memberByNotifId, screenByNotifId] = await Promise.all([
      this.resolveMembersForFeed(page),
      this.resolveScreensForFeed(page),
    ]);

    return {
      items: page.map((n) =>
        this.mapFeedItem(
          n,
          memberByNotifId.get(n.id) ?? null,
          screenByNotifId.get(n.id),
        ),
      ),
      data: page.map((n) => {
        const member = memberByNotifId.get(n.id) ?? null;
        const screen = screenByNotifId.get(n.id);
        const it = this.mapFeedItem(n, member, screen);
        return {
          id: it.id,
          title: it.title,
          description: it.body,
          member_name: member?.name ?? null,
          screen: it.screen,
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

  async markRead(
    recipientUserId: string,
    notificationId: string,
    gymId?: string,
  ) {
    await this.prisma.notification.updateMany({
      where: {
        id: notificationId,
        recipientUserId,
        ...(gymId ? { gymId } : {}),
      },
      data: { readAt: new Date() },
    });
    return { ok: true as const };
  }

  /**
   * Sends a synthetic notification over WebSocket and FCM (if configured and token present).
   * Callers may only target their own phone unless they are {@link GlobalRole.SUPER_ADMIN}.
   */
  async sendTestNotification(caller: JwtUser, targetPhone: string) {
    const phone = targetPhone.trim();
    const user = await this.prisma.user.findUnique({
      where: { phone },
      select: { id: true, fcmDeviceToken: true },
    });
    if (!user) {
      throw new NotFoundException('User not found');
    }
    if (caller.sub !== user.id && caller.globalRole !== GlobalRole.SUPER_ADMIN) {
      throw new ForbiddenException(
        'You can only send test notifications to your own phone',
      );
    }

    const title = 'Test notification';
    const body =
      'This is a test. If you see this, WebSocket / push delivery is working.';
    const testId = `test-${Date.now()}`;
    const wsPayload: NotificationFeedItem = {
      id: testId,
      gymId: '',
      title,
      body,
      type: NotificationType.INFO,
      readAt: null,
      createdAt: new Date(),
      metadata: { test: true },
      entity: null,
      actor: null,
      member: null,
      screen: null,
    };
    this.gateway.emitToUser(user.id, wsPayload);

    const token = user.fcmDeviceToken?.trim();
    if (!token) {
      return {
        ok: true as const,
        websocket: true as const,
        fcm: { attempted: false as const, reason: 'no_device_token' as const },
      };
    }
    if (!this.firebasePush.isEnabled) {
      return {
        ok: true as const,
        websocket: true as const,
        fcm: {
          attempted: false as const,
          reason: 'fcm_not_configured' as const,
        },
      };
    }

    const result = await this.firebasePush.sendToDevice(token, {
      title,
      body,
      data: {
        notificationId: testId,
        gymId: '',
        type: NotificationType.INFO,
        test: 'true',
      },
    });

    if (result.ok === false && result.shouldClearToken) {
      await this.prisma.user.updateMany({
        where: { id: user.id, fcmDeviceToken: token },
        data: { fcmDeviceToken: null, fcmTokenUpdatedAt: null },
      });
    }

    return {
      ok: true as const,
      websocket: true as const,
      fcm: {
        attempted: true as const,
        delivered: result.ok,
        ...(result.ok === false && !result.shouldClearToken
          ? { error: 'send_failed' as const }
          : {}),
        ...(result.ok === false && result.shouldClearToken
          ? { error: 'invalid_token_cleared' as const }
          : {}),
      },
    };
  }
}
