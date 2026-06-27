import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  GymStatus,
  GymTrakSaasTier,
  Prisma,
  SubscriptionStatus,
} from '@prisma/client';
import { PrismaService } from '../modules/prisma/prisma.service';
import type { PlatformGymsQueryDto } from './dto/platform-gyms-query.dto';
import type { UpsertPlatformGymSubscriptionDto } from './dto/upsert-platform-gym-subscription.dto';

const gymListSelect = {
  id: true,
  name: true,
  slug: true,
  status: true,
  timezone: true,
  createdAt: true,
  owner: { select: { id: true, phone: true, fullName: true } },
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
} satisfies Prisma.GymSelect;

function monthlyEquivalentCents(priceCents: number, interval: string): number {
  const i = interval.toLowerCase();
  if (i === 'yearly' || i === 'annual' || i === 'year') {
    return Math.round(priceCents / 12);
  }
  return priceCents;
}

@Injectable()
export class PlatformAdminService {
  constructor(private readonly prisma: PrismaService) {}

  async listGyms(query: PlatformGymsQueryDto) {
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

    const [items, total] = await Promise.all([
      this.prisma.gym.findMany({
        where,
        take: limit,
        skip: offset,
        orderBy: { createdAt: 'desc' },
        select: gymListSelect,
      }),
      this.prisma.gym.count({ where }),
    ]);

    return { items, total, limit, offset };
  }

  async getGym(gymId: string) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: gymListSelect,
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    return gym;
  }

  async setGymStatus(gymId: string, status: GymStatus) {
    if (status !== GymStatus.ACTIVE && status !== GymStatus.SUSPENDED) {
      throw new BadRequestException('Only ACTIVE or SUSPENDED is allowed');
    }
    try {
      return await this.prisma.gym.update({
        where: { id: gymId },
        data: { status },
        select: gymListSelect,
      });
    } catch {
      throw new NotFoundException('Gym not found');
    }
  }

  async getGymSubscription(gymId: string) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    return this.prisma.gymSubscription.findUnique({
      where: { gymId },
      include: {
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
    });
  }

  async upsertGymSubscription(
    gymId: string,
    dto: UpsertPlatformGymSubscriptionDto,
  ) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const plan = await this.prisma.subscriptionPlan.findUnique({
      where: { id: dto.planId },
      select: { id: true, saasTier: true },
    });
    if (!plan) {
      throw new NotFoundException('Subscription plan not found');
    }
    if (plan.saasTier === null) {
      throw new BadRequestException(
        'Plan must be a GymTrak SaaS plan (has a saasTier)',
      );
    }

    const data: Prisma.GymSubscriptionUpdateInput = {
      plan: { connect: { id: dto.planId } },
    };
    if (dto.status !== undefined) {
      data.status = dto.status;
    }
    if (dto.renewsAt !== undefined) {
      data.renewsAt = dto.renewsAt === null ? null : new Date(dto.renewsAt);
    }

    return this.prisma.gymSubscription.upsert({
      where: { gymId },
      create: {
        gymId,
        planId: dto.planId,
        status: dto.status ?? SubscriptionStatus.TRIALING,
        renewsAt:
          dto.renewsAt === undefined || dto.renewsAt === null
            ? null
            : new Date(dto.renewsAt),
      },
      update: data,
      include: {
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
    });
  }

  async listSaasPlans() {
    return this.prisma.subscriptionPlan.findMany({
      where: {
        saasTier: {
          in: [
            GymTrakSaasTier.BASIC,
            GymTrakSaasTier.PLUS,
            GymTrakSaasTier.PREMIUM,
          ],
        },
        isActive: true,
      },
      orderBy: [{ saasTier: 'asc' }, { name: 'asc' }],
      select: {
        id: true,
        name: true,
        code: true,
        interval: true,
        priceCents: true,
        currency: true,
        saasTier: true,
      },
    });
  }

  async revenueOverview() {
    const [gymByStatus, subByStatus, subsForMrr] = await Promise.all([
      this.prisma.gym.groupBy({
        by: ['status'],
        _count: { id: true },
      }),
      this.prisma.gymSubscription.groupBy({
        by: ['status'],
        _count: { id: true },
      }),
      this.prisma.gymSubscription.findMany({
        where: {
          status: {
            in: [SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING],
          },
          gym: { status: GymStatus.ACTIVE },
        },
        include: {
          plan: {
            select: {
              interval: true,
              priceCents: true,
              currency: true,
              saasTier: true,
            },
          },
        },
      }),
    ]);

    const mrrByCurrency = new Map<string, number>();
    const tierCounts: Record<string, number> = {
      BASIC: 0,
      PLUS: 0,
      PREMIUM: 0,
      UNKNOWN: 0,
    };

    for (const row of subsForMrr) {
      const p = row.plan;
      const m = monthlyEquivalentCents(p.priceCents, p.interval);
      const cur = p.currency ?? 'USD';
      mrrByCurrency.set(cur, (mrrByCurrency.get(cur) ?? 0) + m);

      const t = p.saasTier;
      if (t === 'BASIC' || t === 'PLUS' || t === 'PREMIUM') {
        tierCounts[t] += 1;
      } else {
        tierCounts.UNKNOWN += 1;
      }
    }

    return {
      gymsByStatus: Object.fromEntries(
        gymByStatus.map((r) => [r.status, r._count.id]),
      ),
      subscriptionsByStatus: Object.fromEntries(
        subByStatus.map((r) => [r.status, r._count.id]),
      ),
      /** Estimated platform MRR from SaaS rows (active gym + active/trial subscription). */
      estimatedMrrByCurrency: Object.fromEntries(mrrByCurrency),
      activeSaasSubscriptionsByTier: tierCounts,
      totals: {
        gyms: gymByStatus.reduce((s, r) => s + r._count.id, 0),
        gymSubscriptions: subByStatus.reduce((s, r) => s + r._count.id, 0),
      },
    };
  }
}
