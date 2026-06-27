import { Injectable } from '@nestjs/common';
import { GymRole, PlanType, Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';

const DEFAULT_LIMIT = 8;
const MIN_QUERY_LEN = 2;

export type SearchMemberHit = {
  type: 'member';
  gymUserId: string;
  userId: string;
  name: string | null;
  phone: string;
  isLead: boolean;
};

export type SearchTrainerHit = {
  type: 'trainer';
  gymUserId: string;
  userId: string;
  name: string | null;
  phone: string;
};

export type SearchPlanHit = {
  type: 'plan';
  planId: string;
  name: string;
  planType: PlanType;
  priceCents: number;
  currency: string;
  isActive: boolean;
};

@Injectable()
export class SearchService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async globalSearch(
    actorUserId: string,
    gymId: string,
    rawQ: string,
    limitPerCategory?: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const q = rawQ.trim();
    const limit = Math.min(Math.max(limitPerCategory ?? DEFAULT_LIMIT, 1), 20);

    if (q.length < MIN_QUERY_LEN) {
      return {
        query: q,
        minLength: MIN_QUERY_LEN,
        members: [] as SearchMemberHit[],
        trainers: [] as SearchTrainerHit[],
        plans: [] as SearchPlanHit[],
      };
    }

    const phoneDigits = q.replace(/\D/g, '');
    const userOr: Prisma.UserWhereInput = {
      OR: [
        { fullName: { contains: q, mode: 'insensitive' as const } },
        { phone: { contains: q, mode: 'insensitive' as const } },
        ...(phoneDigits.length >= MIN_QUERY_LEN && phoneDigits !== q
          ? [{ phone: { contains: phoneDigits, mode: 'insensitive' as const } }]
          : []),
      ],
    };

    const memberSelect = {
      id: true,
      isLead: true,
      user: { select: { id: true, fullName: true, phone: true } },
    } as const;

    const [memberRows, trainerRows, planRows] = await Promise.all([
      this.prisma.gymUser.findMany({
        where: {
          gymId,
          role: GymRole.MEMBER,
          isActive: true,
          user: userOr,
        },
        orderBy: { joinedAt: 'desc' },
        take: limit,
        select: memberSelect,
      }),
      this.prisma.gymUser.findMany({
        where: {
          gymId,
          role: GymRole.TRAINER,
          isActive: true,
          user: userOr,
        },
        orderBy: { joinedAt: 'desc' },
        take: limit,
        select: memberSelect,
      }),
      this.prisma.gymPlan.findMany({
        where: {
          gymId,
          isActive: true,
          name: { contains: q, mode: 'insensitive' as const },
        },
        orderBy: { name: 'asc' },
        take: limit,
        select: {
          id: true,
          name: true,
          type: true,
          priceCents: true,
          currency: true,
          isActive: true,
        },
      }),
    ]);

    const members: SearchMemberHit[] = memberRows.map((r) => ({
      type: 'member' as const,
      gymUserId: r.id,
      userId: r.user.id,
      name: r.user.fullName,
      phone: r.user.phone,
      isLead: r.isLead,
    }));

    const trainers: SearchTrainerHit[] = trainerRows.map((r) => ({
      type: 'trainer' as const,
      gymUserId: r.id,
      userId: r.user.id,
      name: r.user.fullName,
      phone: r.user.phone,
    }));

    const plans: SearchPlanHit[] = planRows.map((r) => ({
      type: 'plan' as const,
      planId: r.id,
      name: r.name,
      planType: r.type,
      priceCents: r.priceCents,
      currency: r.currency,
      isActive: r.isActive,
    }));

    return {
      query: q,
      minLength: MIN_QUERY_LEN,
      members,
      trainers,
      plans,
    };
  }
}
