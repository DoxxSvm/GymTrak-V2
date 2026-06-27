import { Injectable, NotFoundException, OnModuleDestroy } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  GlobalRole,
  GymRole,
  GymStatus,
  PaymentStatus,
  Prisma,
  SubscriptionStatus,
} from '@prisma/client';
import Redis from 'ioredis';
import { PrismaService } from '../prisma/prisma.service';
import { PlatformAdminService } from '../../platform/platform-admin.service';
import type { AdminActivityQueryDto } from './dto/admin-activity-query.dto';
import type { AdminGymsQueryDto } from './dto/admin-gyms-query.dto';
import type { AdminSubscriptionsQueryDto } from './dto/admin-subscriptions-query.dto';
import {
  AdminUserGymRoleFilter,
  type AdminUsersQueryDto,
} from './dto/admin-users-query.dto';
import type { UpsertPlatformGymSubscriptionDto } from '../../platform/dto/upsert-platform-gym-subscription.dto';

const ANALYTICS_CACHE_KEY = 'admin:analytics:v1';
const ANALYTICS_TTL_SECONDS = 60;

const trainerRoles: GymRole[] = [GymRole.TRAINER, GymRole.STAFF];

/**
 * Optional analytics cache: must not crash or spam logs when Redis is down.
 * ioredis emits `error` if no listener is attached — that caused ECONNREFUSED noise.
 */
function createOptionalRedis(url: string): Redis {
  const client = new Redis(url, {
    lazyConnect: true,
    enableOfflineQueue: false,
    maxRetriesPerRequest: 1,
    retryStrategy: () => null,
  });
  client.on('error', () => {
    /* cache-only; ignore unreachable Redis */
  });
  return client;
}

@Injectable()
export class AdminService implements OnModuleDestroy {
  private readonly redis: Redis | null;

  constructor(
    private readonly prisma: PrismaService,
    private readonly platformAdmin: PlatformAdminService,
    config: ConfigService,
  ) {
    const url = config.get<string>('REDIS_URL')?.trim();
    this.redis = url ? createOptionalRedis(url) : null;
  }

  async onModuleDestroy(): Promise<void> {
    if (!this.redis) {
      return;
    }
    try {
      this.redis.disconnect();
    } catch {
      /* ignore */
    }
  }

  me(userId: string) {
    return this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        id: true,
        phone: true,
        email: true,
        fullName: true,
        globalRole: true,
        status: true,
        createdAt: true,
      },
    });
  }

  async listGyms(query: AdminGymsQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    const where: Prisma.GymWhereInput = {};
    if (query.status) {
      where.status = query.status;
    }
    const q = query.q?.trim();
    if (q) {
      where.OR = [
        { name: { contains: q, mode: 'insensitive' } },
        { slug: { contains: q, mode: 'insensitive' } },
      ];
    }

    const [rows, total] = await Promise.all([
      this.prisma.gym.findMany({
        where,
        take: limit,
        skip: offset,
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          name: true,
          slug: true,
          status: true,
          createdAt: true,
          owner: {
            select: {
              id: true,
              phone: true,
              email: true,
              fullName: true,
            },
          },
        },
      }),
      this.prisma.gym.count({ where }),
    ]);

    const ids = rows.map((g) => g.id);
    const [memberAgg, trainerAgg] = await Promise.all([
      ids.length
        ? this.prisma.gymUser.groupBy({
            by: ['gymId'],
            where: { gymId: { in: ids }, role: GymRole.MEMBER },
            _count: { _all: true },
          })
        : Promise.resolve([]),
      ids.length
        ? this.prisma.gymUser.groupBy({
            by: ['gymId'],
            where: {
              gymId: { in: ids },
              role: { in: trainerRoles },
            },
            _count: { _all: true },
          })
        : Promise.resolve([]),
    ]);

    const memberMap = new Map(memberAgg.map((r) => [r.gymId, r._count._all]));
    const trainerMap = new Map(trainerAgg.map((r) => [r.gymId, r._count._all]));

    const items = rows.map((g) => ({
      ...g,
      totalMembers: memberMap.get(g.id) ?? 0,
      totalTrainers: trainerMap.get(g.id) ?? 0,
    }));

    return { items, total, limit, offset };
  }

  async getGymDetail(gymId: string) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: {
        id: true,
        name: true,
        slug: true,
        status: true,
        timezone: true,
        address: true,
        createdAt: true,
        updatedAt: true,
        owner: {
          select: {
            id: true,
            phone: true,
            email: true,
            fullName: true,
          },
        },
        subscription: {
          select: {
            id: true,
            status: true,
            renewsAt: true,
            plan: {
              select: {
                id: true,
                code: true,
                name: true,
                saasTier: true,
                interval: true,
                priceCents: true,
                currency: true,
              },
            },
          },
        },
      },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const [memberCount, trainerCount, revenueAgg] = await Promise.all([
      this.prisma.gymUser.count({
        where: { gymId, role: GymRole.MEMBER },
      }),
      this.prisma.gymUser.count({
        where: { gymId, role: { in: trainerRoles } },
      }),
      this.prisma.payment.aggregate({
        where: {
          gymId,
          status: PaymentStatus.COMPLETED,
        },
        _sum: { amountCents: true },
      }),
    ]);

    return {
      ...gym,
      counts: {
        members: memberCount,
        trainers: trainerCount,
      },
      revenue: {
        totalCompletedCents: revenueAgg._sum.amountCents ?? 0,
        currency:
          (
            await this.prisma.payment.findFirst({
              where: { gymId, status: PaymentStatus.COMPLETED },
              select: { currency: true },
              orderBy: { createdAt: 'desc' },
            })
          )?.currency ?? 'INR',
      },
    };
  }

  async blockGym(gymId: string) {
    return this.setGymBlocked(gymId, true);
  }

  async unblockGym(gymId: string) {
    return this.setGymBlocked(gymId, false);
  }

  private async setGymBlocked(gymId: string, blocked: boolean) {
    try {
      return await this.prisma.gym.update({
        where: { id: gymId },
        data: { status: blocked ? GymStatus.SUSPENDED : GymStatus.ACTIVE },
        select: {
          id: true,
          name: true,
          status: true,
          updatedAt: true,
        },
      });
    } catch {
      throw new NotFoundException('Gym not found');
    }
  }

  async listUsers(query: AdminUsersQueryDto) {
    const page = query.page ?? 1;
    const limit = query.limit ?? 20;
    const skip = (page - 1) * limit;

    const where = this.buildUserWhere(query);

    const [items, total] = await Promise.all([
      this.prisma.user.findMany({
        where,
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          phone: true,
          email: true,
          fullName: true,
          globalRole: true,
          status: true,
          createdAt: true,
          ownedGyms: { select: { id: true, name: true } },
          gymMemberships: {
            select: {
              id: true,
              gymId: true,
              role: true,
              isActive: true,
              gym: { select: { id: true, name: true, slug: true } },
            },
          },
        },
      }),
      this.prisma.user.count({ where }),
    ]);

    return {
      items,
      total,
      page,
      limit,
      totalPages: Math.ceil(total / limit) || 1,
    };
  }

  private buildUserWhere(query: AdminUsersQueryDto): Prisma.UserWhereInput {
    const gymId = query.gymId?.trim();
    const role = query.role;

    const gymScope = gymId
      ? {
          OR: [
            { ownedGyms: { some: { id: gymId } } },
            { gymMemberships: { some: { gymId } } },
          ],
        }
      : undefined;

    if (!role) {
      return gymScope ?? {};
    }

    if (role === AdminUserGymRoleFilter.OWNER) {
      const ownerFilter: Prisma.UserWhereInput = {
        OR: [
          { ownedGyms: gymId ? { some: { id: gymId } } : { some: {} } },
          {
            gymMemberships: {
              some: {
                role: GymRole.OWNER,
                ...(gymId ? { gymId } : {}),
              },
            },
          },
        ],
      };
      return ownerFilter;
    }

    if (role === AdminUserGymRoleFilter.TRAINER) {
      return {
        gymMemberships: {
          some: {
            role: { in: trainerRoles },
            ...(gymId ? { gymId } : {}),
          },
        },
      };
    }

    if (role === AdminUserGymRoleFilter.MEMBER) {
      return {
        gymMemberships: {
          some: {
            role: GymRole.MEMBER,
            ...(gymId ? { gymId } : {}),
          },
        },
      };
    }

    return gymScope ?? {};
  }

  async getUserDetail(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        phone: true,
        email: true,
        username: true,
        fullName: true,
        globalRole: true,
        status: true,
        createdAt: true,
        updatedAt: true,
        onboardingCompletedAt: true,
        ownedGyms: {
          select: {
            id: true,
            name: true,
            slug: true,
            status: true,
            createdAt: true,
          },
        },
        gymMemberships: {
          select: {
            id: true,
            gymId: true,
            role: true,
            isActive: true,
            joinedAt: true,
            gym: {
              select: {
                id: true,
                name: true,
                slug: true,
                status: true,
              },
            },
          },
        },
      },
    });
    if (!user) {
      throw new NotFoundException('User not found');
    }
    return user;
  }

  async getAnalytics() {
    const cached =
      await this.getCachedJson<Record<string, unknown>>(ANALYTICS_CACHE_KEY);
    if (cached) {
      return cached;
    }

    const [
      totalGyms,
      totalUsers,
      activeSubscriptions,
      revenueByCurrency,
      superAdmins,
    ] = await Promise.all([
      this.prisma.gym.count(),
      this.prisma.user.count({
        where: { globalRole: { not: GlobalRole.SUPER_ADMIN } },
      }),
      this.prisma.gymSubscription.count({
        where: {
          status: {
            in: [SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING],
          },
        },
      }),
      this.prisma.payment.groupBy({
        by: ['currency'],
        where: { status: PaymentStatus.COMPLETED },
        _sum: { amountCents: true },
      }),
      this.prisma.user.count({ where: { globalRole: GlobalRole.SUPER_ADMIN } }),
    ]);

    const totalRevenueByCurrency = Object.fromEntries(
      revenueByCurrency.map((r) => [r.currency, r._sum.amountCents ?? 0]),
    );

    const payload = {
      totalGyms,
      totalUsers,
      /** Platform operators excluded from `totalUsers`; listed separately. */
      superAdminUsers: superAdmins,
      totalRevenueByCurrency,
      activeSubscriptions,
    };

    await this.setCachedJson(
      ANALYTICS_CACHE_KEY,
      payload,
      ANALYTICS_TTL_SECONDS,
    );
    return payload;
  }

  async listSubscriptions(query: AdminSubscriptionsQueryDto) {
    const limit = query.limit ?? 50;
    const offset = query.offset ?? 0;
    const [items, total] = await Promise.all([
      this.prisma.gymSubscription.findMany({
        take: limit,
        skip: offset,
        orderBy: { updatedAt: 'desc' },
        select: {
          id: true,
          status: true,
          renewsAt: true,
          createdAt: true,
          updatedAt: true,
          gym: {
            select: {
              id: true,
              name: true,
              slug: true,
              status: true,
            },
          },
          plan: {
            select: {
              id: true,
              code: true,
              name: true,
              saasTier: true,
              interval: true,
              priceCents: true,
              currency: true,
            },
          },
        },
      }),
      this.prisma.gymSubscription.count(),
    ]);
    return { items, total, limit, offset };
  }

  async patchSubscription(
    gymId: string,
    dto: UpsertPlatformGymSubscriptionDto,
  ) {
    const result = await this.platformAdmin.upsertGymSubscription(gymId, dto);
    await this.invalidateAnalyticsCache();
    return result;
  }

  listSaasPlans() {
    return this.platformAdmin.listSaasPlans();
  }

  async activity(query: AdminActivityQueryDto) {
    const limit = query.limit ?? 30;
    const offset = query.offset ?? 0;
    const where: Prisma.AuditLogWhereInput = {};
    if (query.gymId?.trim()) {
      where.gymId = query.gymId.trim();
    }
    const [items, total] = await Promise.all([
      this.prisma.auditLog.findMany({
        where,
        take: limit,
        skip: offset,
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          gymId: true,
          action: true,
          entityType: true,
          entityId: true,
          createdAt: true,
          actorUserId: true,
          actor: { select: { id: true, phone: true, fullName: true } },
          gym: { select: { id: true, name: true, slug: true } },
        },
      }),
      this.prisma.auditLog.count({ where }),
    ]);
    return { items, total, limit, offset };
  }

  private async getCachedJson<T extends Record<string, unknown>>(
    key: string,
  ): Promise<T | null> {
    if (!this.redis) {
      return null;
    }
    try {
      const raw = await this.redis.get(key);
      if (!raw) {
        return null;
      }
      return JSON.parse(raw) as T;
    } catch {
      return null;
    }
  }

  private async setCachedJson(
    key: string,
    value: Record<string, unknown>,
    ttlSeconds: number,
  ): Promise<void> {
    if (!this.redis) {
      return;
    }
    try {
      await this.redis.set(key, JSON.stringify(value), 'EX', ttlSeconds);
    } catch {
      /* ignore cache failures */
    }
  }

  private async invalidateAnalyticsCache(): Promise<void> {
    if (!this.redis) {
      return;
    }
    try {
      await this.redis.del(ANALYTICS_CACHE_KEY);
    } catch {
      /* ignore */
    }
  }
}
