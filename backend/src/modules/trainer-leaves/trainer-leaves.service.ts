import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  GymRole,
  Prisma,
  TrainerLeaveStatus,
  TrainerLeaveType,
} from '@prisma/client';
// TEMP: trainer-leaves — re-enable before production
// import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { GymAccessService } from '../../common/services/gym-access.service';
import { monthUtcRange } from '../../common/utils/month-utc-range';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import type { CreateTrainerLeaveDto } from './dto/create-trainer-leave.dto';
import type { ListLeavesQueryDto } from './dto/list-leaves-query.dto';
import type { RejectLeaveDto } from './dto/reject-leave.dto';
import type { UpdateTrainerLeaveDto } from './dto/update-trainer-leave.dto';

function parseYmdUtcStart(ymd: string): Date {
  const [y, m, d] = ymd.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d, 0, 0, 0, 0));
}

function parseYmdUtcEndInclusive(ymd: string): Date {
  const [y, m, d] = ymd.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d, 23, 59, 59, 999));
}

function inclusiveUtcDayCount(start: Date, end: Date): number {
  const sd = Date.UTC(
    start.getUTCFullYear(),
    start.getUTCMonth(),
    start.getUTCDate(),
  );
  const ed = Date.UTC(
    end.getUTCFullYear(),
    end.getUTCMonth(),
    end.getUTCDate(),
  );
  return Math.floor((ed - sd) / 86400000) + 1;
}

function leaveTypeApi(t: TrainerLeaveType): string {
  return t.toLowerCase();
}

function leaveStatusApi(s: TrainerLeaveStatus): string {
  return s.toLowerCase();
}

@Injectable()
export class TrainerLeavesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly perm: PermissionEngineService,
    private readonly config: ConfigService,
  ) {}

  private annualAllowanceDays(): number {
    const raw = this.config.get<string>('TRAINER_ANNUAL_LEAVE_DAYS');
    const n = parseInt(raw ?? '12', 10);
    return Number.isFinite(n) && n >= 0 ? n : 12;
  }

  private ok<T>(data: T, message?: string) {
    return message
      ? { success: true as const, message, data }
      : { success: true as const, data };
  }

  async create(actorUserId: string, dto: CreateTrainerLeaveDto) {

    const viewAll = await this.perm.canViewAllLeaves(actorUserId, dto.gymId);
    let trainerGymUserId: string;

    if (viewAll) {
      if (!dto.trainerId?.trim()) {
        throw new BadRequestException(
          'trainerId is required when creating leave for another trainer',
        );
      }
      const tr = await this.prisma.gymUser.findFirst({
        where: {
          id: dto.trainerId.trim(),
          gymId: dto.gymId,
          role: GymRole.TRAINER,
          isActive: true,
        },
        select: { id: true },
      });
      if (!tr) {
        throw new NotFoundException('Trainer not found at this gym');
      }
      trainerGymUserId = tr.id;
    } else {
      const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
        actorUserId,
        dto.gymId,
      );
      const tid = dto.trainerId?.trim();
      if (tid && tid !== gymUserId && tid !== actorUserId) {
        throw new ForbiddenException('You can only create leave for yourself');
      }
      trainerGymUserId = gymUserId;
    }

    const startsAt = parseYmdUtcStart(dto.startDate);
    const endsAt = parseYmdUtcEndInclusive(dto.endDate);

    if (!(endsAt >= startsAt)) {
      throw new BadRequestException('endDate must be on or after startDate');
    }

    const row = await this.prisma.trainerLeave.create({
      data: {
        gymId: dto.gymId,
        trainerGymUserId,
        leaveType: dto.leaveType,
        startsAt,
        endsAt,
        reason: dto.reason?.trim() || null,
        status: TrainerLeaveStatus.PENDING,
      },
      select: { id: true, status: true },
    });

    return this.ok(
      {
        id: row.id,
        status: leaveStatusApi(row.status),
      },
      'Leave applied successfully',
    );
  }

  async list(actorUserId: string, query: ListLeavesQueryDto) {
    const gymId = query.gymId;
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_READ,
    // );

    const viewAll = await this.perm.canViewAllLeaves(actorUserId, gymId);
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;

    const where: Prisma.TrainerLeaveWhereInput = { gymId };

    // if (!viewAll) {
    //   console.log('1111111111111111111111111111');
    //   const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
    //     actorUserId,
    //     gymId,
    //   );
    //   where.trainerGymUserId = gymUserId;
    // } else

    if (query.trainerId?.trim()) {
      where.trainerGymUserId = query.trainerId.trim();
    }

    if (query.status) {
      where.status = query.status;
    }

    if (query.month?.trim()) {
      const [y, m] = query.month.split('-').map(Number);
      if (!y || !m) {
        throw new BadRequestException('Invalid month (use YYYY-MM)');
      }
      const { start, endExclusive } = monthUtcRange(y, m);
      where.startsAt = { lt: endExclusive };
      where.endsAt = { gte: start };
    } else if (query.dateFrom || query.dateTo) {
      if (!query.dateFrom?.trim() || !query.dateTo?.trim()) {
        throw new BadRequestException(
          'Both dateFrom and dateTo are required (YYYY-MM-DD)',
        );
      }
      const dStart = parseYmdUtcStart(query.dateFrom.trim());
      const dEnd = parseYmdUtcEndInclusive(query.dateTo.trim());
      if (dEnd < dStart) {
        throw new BadRequestException('dateFrom must be on or before dateTo');
      }
      where.startsAt = { lte: dEnd };
      where.endsAt = { gte: dStart };
    }

    if (query.q?.trim()) {
      const q = query.q.trim();
      where.trainerGymUser = {
        user: {
          OR: [
            { fullName: { contains: q, mode: 'insensitive' } },
            { phone: { contains: q } },
          ],
        },
      };
    }

    const [total, rows] = await Promise.all([
      this.prisma.trainerLeave.count({ where }),
      this.prisma.trainerLeave.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        take: limit,
        skip: offset,
        include: {
          trainerGymUser: {
            select: {
              id: true,
              user: { select: { fullName: true } },
            },
          },
        },
      }),
    ]);

    const data = rows.map((r) => this.serializeListItem(r));
    return {
      success: true as const,
      data,
      total,
      limit,
      offset,
    };
  }

  async getOne(actorUserId: string, gymId: string, leaveId: string) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_READ,
    // );

    const row = await this.prisma.trainerLeave.findFirst({
      where: { id: leaveId, gymId },
      include: {
        trainerGymUser: {
          select: {
            id: true,
            user: { select: { fullName: true, phone: true } },
          },
        },
      },
    });
    if (!row) {
      throw new NotFoundException('Leave not found');
    }

    const viewAll = await this.perm.canViewAllLeaves(actorUserId, gymId);
    if (!viewAll) {
      const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
        actorUserId,
        gymId,
      );
      if (row.trainerGymUserId !== gymUserId) {
        throw new ForbiddenException('You can only view your own leave');
      }
    }

    return this.ok(this.serializeDetail(row));
  }

  async update(
    actorUserId: string,
    gymId: string,
    leaveId: string,
    dto: UpdateTrainerLeaveDto,
  ) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_UPDATE,
    // );

    const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
      actorUserId,
      gymId,
    );

    const row = await this.prisma.trainerLeave.findFirst({
      where: { id: leaveId, gymId, trainerGymUserId: gymUserId },
    });
    if (!row) {
      throw new NotFoundException('Leave not found');
    }
    if (row.status !== TrainerLeaveStatus.PENDING) {
      throw new BadRequestException('Only pending leave can be updated');
    }

    const data: Prisma.TrainerLeaveUpdateInput = {};
    if (dto.leaveType != null) {
      data.leaveType = dto.leaveType;
    }
    if (dto.reason !== undefined) {
      data.reason = dto.reason?.trim() || null;
    }

    let startsAt = row.startsAt;
    let endsAt = row.endsAt;
    if (dto.startDate != null) {
      startsAt = parseYmdUtcStart(dto.startDate);
    }
    if (dto.endDate != null) {
      endsAt = parseYmdUtcEndInclusive(dto.endDate);
    }
    if (dto.startDate != null || dto.endDate != null) {
      if (!(endsAt >= startsAt)) {
        throw new BadRequestException('endDate must be on or after startDate');
      }
      data.startsAt = startsAt;
      data.endsAt = endsAt;
    }

    const updated = await this.prisma.trainerLeave.update({
      where: { id: leaveId },
      data,
      include: {
        trainerGymUser: {
          select: {
            id: true,
            user: { select: { fullName: true, phone: true } },
          },
        },
      },
    });

    return this.ok(this.serializeDetail(updated));
  }

  async approve(actorUserId: string, gymId: string, leaveId: string) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_APPROVE,
    // );

    const row = await this.prisma.trainerLeave.findFirst({
      where: { id: leaveId, gymId },
    });
    if (!row) {
      throw new NotFoundException('Leave not found');
    }
    if (row.status !== TrainerLeaveStatus.PENDING) {
      throw new BadRequestException('Leave is no longer pending');
    }

    await this.prisma.trainerLeave.update({
      where: { id: leaveId },
      data: {
        status: TrainerLeaveStatus.APPROVED,
        decidedAt: new Date(),
        decidedByUserId: actorUserId,
        rejectionReason: null,
      },
    });

    return { success: true as const, message: 'Leave approved' };
  }

  async reject(
    actorUserId: string,
    gymId: string,
    leaveId: string,
    dto: RejectLeaveDto,
  ) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_REJECT,
    // );

    const row = await this.prisma.trainerLeave.findFirst({
      where: { id: leaveId, gymId },
    });
    if (!row) {
      throw new NotFoundException('Leave not found');
    }
    if (row.status !== TrainerLeaveStatus.PENDING) {
      throw new BadRequestException('Leave is no longer pending');
    }

    await this.prisma.trainerLeave.update({
      where: { id: leaveId },
      data: {
        status: TrainerLeaveStatus.REJECTED,
        decidedAt: new Date(),
        decidedByUserId: actorUserId,
        rejectionReason: dto.reason.trim(),
      },
    });

    return { success: true as const, message: 'Leave rejected' };
  }

  async remove(actorUserId: string, gymId: string, leaveId: string) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_DELETE,
    // );

    const row = await this.prisma.trainerLeave.findFirst({
      where: { id: leaveId, gymId },
    });
    if (!row) {
      throw new NotFoundException('Leave not found');
    }

    await this.prisma.trainerLeave.delete({ where: { id: leaveId } });
    return { success: true as const, message: 'Leave deleted' };
  }

  async cancelMine(actorUserId: string, gymId: string, leaveId: string) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_UPDATE,
    // );

    const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
      actorUserId,
      gymId,
    );
    const row = await this.prisma.trainerLeave.findFirst({
      where: { id: leaveId, gymId, trainerGymUserId: gymUserId },
    });
    if (!row) {
      throw new NotFoundException('Leave not found');
    }
    if (row.status !== TrainerLeaveStatus.PENDING) {
      throw new BadRequestException('Only pending requests can be cancelled');
    }

    await this.prisma.trainerLeave.update({
      where: { id: leaveId },
      data: { status: TrainerLeaveStatus.CANCELLED },
    });

    return this.ok({ id: leaveId, status: 'cancelled' }, 'Leave cancelled');
  }

  async balance(actorUserId: string, gymId: string, trainerId?: string) {
    // await this.perm.assertOwnerOrPermission(
    //   actorUserId,
    //   gymId,
    //   PERMISSION_CODES.LEAVE_READ,
    // );

    const viewAll = await this.perm.canViewAllLeaves(actorUserId, gymId);
    let targetTrainerGymUserId: string;

    if (viewAll) {
      if (!trainerId?.trim()) {
        throw new BadRequestException(
          'trainerId is required for staff/owner balance lookup',
        );
      }
      const tr = await this.prisma.gymUser.findFirst({
        where: {
          id: trainerId.trim(),
          gymId,
          role: GymRole.TRAINER,
          isActive: true,
        },
        select: { id: true },
      });
      if (!tr) {
        throw new NotFoundException('Trainer not found at this gym');
      }
      targetTrainerGymUserId = tr.id;
    } else {
      const { gymUserId } = await this.gymAccess.assertTrainerAtGym(
        actorUserId,
        gymId,
      );
      if (trainerId?.trim() && trainerId.trim() !== gymUserId) {
        throw new ForbiddenException('You can only view your own balance');
      }
      targetTrainerGymUserId = gymUserId;
    }

    const annualAllowance = this.annualAllowanceDays();
    const y = new Date().getUTCFullYear();
    const yearStart = new Date(Date.UTC(y, 0, 1));
    const yearEndExclusive = new Date(Date.UTC(y + 1, 0, 1));

    const approved = await this.prisma.trainerLeave.findMany({
      where: {
        gymId,
        trainerGymUserId: targetTrainerGymUserId,
        status: TrainerLeaveStatus.APPROVED,
        startsAt: { lt: yearEndExclusive },
        endsAt: { gte: yearStart },
      },
      select: { startsAt: true, endsAt: true },
    });

    let usedDays = 0;
    for (const l of approved) {
      usedDays += inclusiveUtcDayCount(l.startsAt, l.endsAt);
    }

    const remaining = Math.max(0, annualAllowance - usedDays);

    return this.ok({
      remaining_balance: remaining,
      annual_allowance: annualAllowance,
      used_days: usedDays,
      year: y,
    });
  }

  private serializeListItem(
    row: Prisma.TrainerLeaveGetPayload<{
      include: {
        trainerGymUser: {
          select: { id: true; user: { select: { fullName: true } } };
        };
      };
    }>,
  ) {
    return {
      id: row.id,
      trainer_id: row.trainerGymUserId,
      trainer_name: row.trainerGymUser.user.fullName?.trim() || 'Trainer',
      leave_type: leaveTypeApi(row.leaveType),
      start_date: row.startsAt.toISOString().slice(0, 10),
      end_date: row.endsAt.toISOString().slice(0, 10),
      days: inclusiveUtcDayCount(row.startsAt, row.endsAt),
      status: leaveStatusApi(row.status),
    };
  }

  private serializeDetail(
    row: Prisma.TrainerLeaveGetPayload<{
      include: {
        trainerGymUser: {
          select: {
            id: true;
            user: { select: { fullName: true; phone: true } };
          };
        };
      };
    }>,
  ) {
    return {
      id: row.id,
      trainer_id: row.trainerGymUserId,
      trainer_name: row.trainerGymUser.user.fullName?.trim() || 'Trainer',
      leave_type: leaveTypeApi(row.leaveType),
      start_date: row.startsAt.toISOString().slice(0, 10),
      end_date: row.endsAt.toISOString().slice(0, 10),
      days: inclusiveUtcDayCount(row.startsAt, row.endsAt),
      reason: row.reason,
      status: leaveStatusApi(row.status),
      rejection_reason: row.rejectionReason,
      created_at: row.createdAt.toISOString(),
      decided_at: row.decidedAt?.toISOString() ?? null,
    };
  }
}
