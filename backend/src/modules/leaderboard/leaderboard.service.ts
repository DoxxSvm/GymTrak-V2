import { Injectable } from '@nestjs/common';
import { GymRole } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type {
  LeaderboardQueryDto,
  LeaderboardType,
} from './dto/leaderboard-query.dto';

export type LeaderboardRow = {
  rank: number;
  userId: string;
  name: string;
  profileImage: string;
  points: number;
  isTopThree: boolean;
  isCurrentUser: boolean;
};

export type LeaderboardResponse = {
  type: LeaderboardType;
  data: LeaderboardRow[];
  page: number;
  limit: number;
  total: number;
};

@Injectable()
export class LeaderboardService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async get(
    actorUserId: string,
    query: LeaderboardQueryDto,
  ): Promise<LeaderboardResponse> {
    const gymId = query.gymId.trim();
    await this.gymAccess.assertCanBrowseGymCatalog(actorUserId, gymId);

    const page = Math.max(1, parseInt(String(query.page ?? '1'), 10) || 1);
    const limit = Math.min(
      100,
      Math.max(1, parseInt(String(query.limit ?? '20'), 10) || 20),
    );

    const members = await this.prisma.gymUser.findMany({
      where: {
        gymId,
        role: GymRole.MEMBER,
        isActive: true,
        isLead: false,
      },
      select: {
        userId: true,
        user: { select: { fullName: true, avatarUrl: true } },
      },
    });

    const userIds = members.map((m) => m.userId);
    const pointsByUser = new Map<string, number>();

    if (query.type === 'attendance') {
      if (userIds.length > 0) {
        const rows = await this.prisma.attendanceRecord.groupBy({
          by: ['memberUserId'],
          where: { gymId, memberUserId: { in: userIds } },
          _count: { _all: true },
        });
        for (const r of rows) {
          pointsByUser.set(r.memberUserId, r._count._all);
        }
      }
    } else {
      if (userIds.length > 0) {
        const rows = await this.prisma.memberWorkoutPlan.groupBy({
          by: ['userId'],
          where: {
            gymId,
            userId: { in: userIds },
            completed: true,
          },
          _count: { _all: true },
        });
        for (const r of rows) {
          pointsByUser.set(r.userId, r._count._all);
        }
      }
    }

    const ranked: Omit<
      LeaderboardRow,
      'rank' | 'isTopThree' | 'isCurrentUser'
    >[] = members
      .map((m) => ({
        userId: m.userId,
        name: m.user.fullName?.trim() || '',
        profileImage: m.user.avatarUrl ?? '',
        points: pointsByUser.get(m.userId) ?? 0,
      }))
      .sort((a, b) => {
        if (b.points !== a.points) return b.points - a.points;
        return a.userId.localeCompare(b.userId);
      });

    const total = ranked.length;
    const offset = (page - 1) * limit;
    const slice = ranked.slice(offset, offset + limit);

    const data: LeaderboardRow[] = slice.map((row, i) => {
      const rank = offset + i + 1;
      return {
        ...row,
        rank,
        isTopThree: rank <= 3,
        isCurrentUser: row.userId === actorUserId,
      };
    });

    return {
      type: query.type,
      data,
      page,
      limit,
      total,
    };
  }
}
