import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { BatchPlanGender, GymRole, PlanType, Prisma } from '@prisma/client';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import type { CreateGymPlanDto } from './dto/create-gym-plan.dto';
import type { CreatePlanCompatDto } from './dto/create-plan-compat.dto';
import type { UpdatePlanCompatDto } from './dto/update-plan-compat.dto';
import type { UpdateGymPlanDto } from './dto/update-gym-plan.dto';

const planInclude = {
  shifts: { orderBy: { sortOrder: 'asc' as const } },
  trainerGymUser: {
    select: {
      id: true,
      user: {
        select: { id: true, fullName: true, phone: true },
      },
    },
  },
} satisfies Prisma.GymPlanInclude;

export type GymPlanRow = Prisma.GymPlanGetPayload<{
  include: typeof planInclude;
}>;

@Injectable()
export class PlansService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly permissions: PermissionEngineService,
    private readonly subscriptions: SubscriptionsService,
  ) {}

  async list(
    actorUserId: string,
    gymId: string,
    type: PlanType | undefined,
    limit: number,
    offset: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const where: Prisma.GymPlanWhereInput = {
      gymId,
      ...(type ? { type } : {}),
    };
    const [total, items] = await Promise.all([
      this.prisma.gymPlan.count({ where }),
      this.prisma.gymPlan.findMany({
        where,
        orderBy: [{ name: 'asc' }, { id: 'asc' }],
        take: limit,
        skip: offset,
        include: planInclude,
      }),
    ]);
    return {
      total,
      limit,
      offset,
      items: items.map((p) => this.serializePlan(p)),
    };
  }

  async createCompat(actorUserId: string, dto: CreatePlanCompatDto) {
    const mapped = this.mapCompatToCreate(dto);
    return this.create(actorUserId, dto.gymId, mapped);
  }

  async getOne(actorUserId: string, gymId: string, planId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.gymPlan.findFirst({
      where: { id: planId, gymId },
      include: planInclude,
    });
    if (!row) {
      throw new NotFoundException('Plan not found');
    }
    return this.serializePlan(row);
  }

  async create(actorUserId: string, gymId: string, dto: CreateGymPlanDto) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const currency = dto.currency?.trim() || 'USD';
    await this.assertTypePayload(
      gymId,
      dto.type,
      dto.trainerGymUserId,
      dto.shifts,
      dto.batchDaysOfWeek,
      dto.batchGender,
    );

    const data = this.buildCreateData(gymId, dto, currency);

    const row = await this.prisma.gymPlan.create({
      data,
      include: planInclude,
    });
    return this.serializePlan(row);
  }

  async update(
    actorUserId: string,
    gymId: string,
    planId: string,
    dto: UpdateGymPlanDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const existing = await this.prisma.gymPlan.findFirst({
      where: { id: planId, gymId },
      include: { shifts: true },
    });
    if (!existing) {
      throw new NotFoundException('Plan not found');
    }

    const nextType = dto.type ?? existing.type;
    const trainerId =
      dto.trainerGymUserId !== undefined
        ? dto.trainerGymUserId
        : existing.trainerGymUserId;
    const shiftPayload =
      dto.shifts !== undefined
        ? dto.shifts
        : existing.shifts.map((s) => ({
            startTime: s.startTime,
            endTime: s.endTime,
            sortOrder: s.sortOrder,
          }));
    const days =
      dto.batchDaysOfWeek !== undefined
        ? dto.batchDaysOfWeek
        : [...existing.batchDaysOfWeek];
    const gender =
      dto.batchGender !== undefined ? dto.batchGender : existing.batchGender;

    await this.assertTypePayload(
      gymId,
      nextType,
      trainerId ?? undefined,
      nextType === PlanType.BATCH_PLAN ? shiftPayload : undefined,
      nextType === PlanType.BATCH_PLAN ? days : undefined,
      nextType === PlanType.BATCH_PLAN ? (gender ?? undefined) : undefined,
    );

    const typeChanged = dto.type !== undefined && dto.type !== existing.type;

    const row = await this.prisma.$transaction(async (tx) => {
      if (dto.shifts !== undefined) {
        await tx.gymPlanShift.deleteMany({ where: { planId } });
      }

      const data: Prisma.GymPlanUpdateInput = {
        ...(dto.name !== undefined ? { name: dto.name.trim() } : {}),
        ...(dto.type !== undefined ? { type: dto.type } : {}),
        ...(dto.durationDays !== undefined
          ? { durationDays: dto.durationDays }
          : {}),
        ...(dto.priceCents !== undefined ? { priceCents: dto.priceCents } : {}),
        ...(dto.currency !== undefined
          ? { currency: dto.currency.trim() || 'USD' }
          : {}),
        ...(dto.isActive !== undefined ? { isActive: dto.isActive } : {}),
        ...(dto.metadata !== undefined
          ? { metadata: dto.metadata as Prisma.InputJsonValue }
          : {}),
      };

      if (typeChanged) {
        Object.assign(data, this.resetForTypeSwitch(nextType));
      }

      if (
        nextType === PlanType.PT_PLAN &&
        (dto.trainerGymUserId !== undefined || typeChanged)
      ) {
        if (trainerId) {
          data.trainerGymUser = { connect: { id: trainerId } };
        }
      } else if (typeChanged && nextType !== PlanType.PT_PLAN) {
        data.trainerGymUser = { disconnect: true };
      }

      if (nextType === PlanType.BATCH_PLAN) {
        if (dto.batchDaysOfWeek !== undefined) {
          data.batchDaysOfWeek = { set: dto.batchDaysOfWeek };
        } else if (typeChanged) {
          data.batchDaysOfWeek = { set: days };
        }
        if (dto.batchGender !== undefined) {
          data.batchGender = dto.batchGender;
        } else if (typeChanged && gender != null) {
          data.batchGender = gender;
        }
      } else if (typeChanged) {
        data.batchDaysOfWeek = { set: [] };
        data.batchGender = null;
      }

      if (dto.shifts !== undefined) {
        data.shifts = {
          create: dto.shifts.map((s, i) => ({
            startTime: s.startTime,
            endTime: s.endTime,
            sortOrder: s.sortOrder ?? i,
          })),
        };
      } else if (typeChanged && nextType === PlanType.BATCH_PLAN) {
        data.shifts = {
          create: shiftPayload.map((s, i) => ({
            startTime: s.startTime,
            endTime: s.endTime,
            sortOrder: s.sortOrder ?? i,
          })),
        };
      }

      return tx.gymPlan.update({
        where: { id: planId },
        data,
        include: planInclude,
      });
    });

    return this.serializePlan(row);
  }

  async updateCompat(
    actorUserId: string,
    gymId: string,
    planId: string,
    dto: UpdatePlanCompatDto,
  ) {
    const mapped = this.mapCompatToUpdate(dto);
    return this.update(actorUserId, gymId, planId, mapped);
  }

  async softDelete(actorUserId: string, gymId: string, planId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.gymPlan.findFirst({
      where: { id: planId, gymId },
    });
    if (!row) {
      throw new NotFoundException('Plan not found');
    }
    await this.prisma.gymPlan.update({
      where: { id: planId },
      data: { isActive: false },
    });
    return { ok: true };
  }

  async listEnrolled(
    actorUserId: string,
    gymId: string,
    planId: string,
    limit: number,
    offset: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const plan = await this.prisma.gymPlan.findFirst({
      where: { id: planId, gymId },
      select: { id: true, name: true, type: true },
    });
    if (!plan) {
      throw new NotFoundException('Plan not found');
    }

    const where: Prisma.MemberSubscriptionWhereInput = {
      gymPlanId: planId,
      gymUser: { gymId, role: GymRole.MEMBER },
    };

    const [total, rows] = await Promise.all([
      this.prisma.memberSubscription.count({ where }),
      this.prisma.memberSubscription.findMany({
        where,
        orderBy: { startsAt: 'desc' },
        take: limit,
        skip: offset,
        select: {
          id: true,
          status: true,
          startsAt: true,
          endsAt: true,
          priceCents: true,
          paidCents: true,
          currency: true,
          gymUser: {
            select: {
              id: true,
              user: {
                select: {
                  id: true,
                  fullName: true,
                  phone: true,
                  email: true,
                },
              },
            },
          },
        },
      }),
    ]);

    return {
      plan,
      total,
      limit,
      offset,
      items: rows.map((s) => ({
        id: s.id,
        status: s.status,
        startsAt: s.startsAt,
        endsAt: s.endsAt,
        priceCents: s.priceCents,
        paidCents: s.paidCents,
        currency: s.currency,
        member: {
          gymUserId: s.gymUser.id,
          userId: s.gymUser.user.id,
          name: s.gymUser.user.fullName,
          phone: s.gymUser.user.phone,
          email: s.gymUser.user.email,
        },
      })),
    };
  }

  async assignMemberPlan(
    actorUserId: string,
    body: {
      member_id: string;
      plan_id: string;
      start_date: string;
      discount?: number;
    },
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: body.member_id },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.permissions.assertOwnerOrPermission(
      actorUserId,
      member.gymId,
      PERMISSION_CODES.MEMBERS,
    );
    const plan = await this.prisma.gymPlan.findFirst({
      where: { id: body.plan_id, gymId: member.gymId, isActive: true },
      select: {
        id: true,
        durationDays: true,
        priceCents: true,
        currency: true,
      },
    });
    if (!plan) {
      throw new NotFoundException('Plan not found');
    }
    const startsAt = new Date(body.start_date);
    if (Number.isNaN(startsAt.getTime())) {
      throw new BadRequestException('Invalid start_date');
    }
    const subscriptionId = await this.subscriptions.createMemberSubscription(
      actorUserId,
      member.gymId,
      member.id,
      {
        gymPlanId: plan.id,
        startsAt: startsAt.toISOString(),
        endsAt: this.addPlanDuration(startsAt, plan.durationDays).toISOString(),
        priceCents: Math.max(0, plan.priceCents - (body.discount ?? 0)),
        currency: plan.currency,
      },
    );

    return { success: true as const, subscription_id: subscriptionId };
  }

  validateCompatPayload(dto: {
    plan_type: string;
    trainer_id?: string;
    batch_details?: {
      working_days?: string[];
      start_time?: string;
      end_time?: string;
      gender?: string;
    };
  }) {
    const missing: string[] = [];
    if (dto.plan_type === 'pt' && !dto.trainer_id) {
      missing.push('trainer_id');
    }
    if (dto.plan_type === 'batch') {
      if (!dto.trainer_id) {
        missing.push('trainer_id');
      }
      if (!dto.batch_details?.working_days?.length) {
        missing.push('batch_details.working_days');
      }
      if (!dto.batch_details?.start_time) {
        missing.push('batch_details.start_time');
      }
      if (!dto.batch_details?.end_time) {
        missing.push('batch_details.end_time');
      }
    }
    return { valid: missing.length === 0, missing_fields: missing };
  }

  /**
   * `GymPlan` has no `batchStartTime` / `batchEndTime` columns; optional batch window
   * from the API is stored on `metadata` alongside any client `metadata` object.
   */
  private mergeGymPlanMetadata(
    dto: CreateGymPlanDto,
  ): Prisma.InputJsonValue | undefined {
    const raw: Record<string, unknown> =
      dto.metadata != null &&
      typeof dto.metadata === 'object' &&
      !Array.isArray(dto.metadata)
        ? { ...(dto.metadata as Record<string, unknown>) }
        : {};
    if (dto.batchStartTime != null) {
      raw.batchStartTime = dto.batchStartTime;
    }
    if (dto.batchEndTime != null) {
      raw.batchEndTime = dto.batchEndTime;
    }
    return Object.keys(raw).length > 0
      ? (raw as Prisma.InputJsonValue)
      : undefined;
  }

  private buildCreateData(
    gymId: string,
    dto: CreateGymPlanDto,
    currency: string,
  ): Prisma.GymPlanCreateInput {
    const meta = this.mergeGymPlanMetadata(dto);
    const base: Prisma.GymPlanCreateInput = {
      gym: { connect: { id: gymId } },
      type: dto.type,
      name: dto.name.trim(),
      durationDays: dto.durationDays,
      priceCents: dto.priceCents,
      currency,
      isActive: true,
      ...(meta != null ? { metadata: meta } : {}),
    };

    if (dto.type === PlanType.PT_PLAN) {
      return {
        ...base,
        trainerGymUser: {
          connect: { id: dto.trainerGymUserId! },
        },
      };
    }

    if (dto.type === PlanType.BATCH_PLAN) {
      return {
        ...base,
        batchDaysOfWeek: dto.batchDaysOfWeek!,
        batchGender: dto.batchGender!,
        shifts: {
          create: dto.shifts!.map((s, i) => ({
            startTime: s.startTime,
            endTime: s.endTime,
            sortOrder: s.sortOrder ?? i,
          })),
        },
      };
    }

    return {
      ...base,
      trainerGymUser: undefined,
      batchDaysOfWeek: [],
      batchGender: null,
    };
  }

  /** Clear fields that do not apply after a type change */
  private resetForTypeSwitch(type: PlanType): Prisma.GymPlanUpdateInput {
    if (type === PlanType.PT_PLAN) {
      return {
        batchDaysOfWeek: { set: [] },
        batchGender: null,
        shifts: { deleteMany: {} },
      };
    }
    if (type === PlanType.BATCH_PLAN) {
      return { trainerGymUser: { disconnect: true } };
    }
    return {
      trainerGymUser: { disconnect: true },
      batchDaysOfWeek: { set: [] },
      batchGender: null,
      shifts: { deleteMany: {} },
    };
  }

  private async assertTypePayload(
    gymId: string,
    type: PlanType,
    trainerGymUserId: string | undefined,
    shifts:
      | { startTime: string; endTime: string; sortOrder?: number }[]
      | undefined,
    batchDaysOfWeek: number[] | undefined,
    batchGender: BatchPlanGender | undefined,
  ) {
    if (type === PlanType.PT_PLAN) {
      if (!trainerGymUserId?.trim()) {
        throw new BadRequestException('PT plan requires trainerGymUserId');
      }
      await this.assertTrainer(gymId, trainerGymUserId.trim());
      return;
    }
    if (type === PlanType.BATCH_PLAN) {
      if (!shifts?.length) {
        throw new BadRequestException('Batch plan requires at least one shift');
      }
      if (!batchDaysOfWeek?.length) {
        throw new BadRequestException('Batch plan requires batchDaysOfWeek');
      }
      if (batchGender == null) {
        throw new BadRequestException('Batch plan requires batchGender');
      }
      if (batchDaysOfWeek.some((d) => d < 0 || d > 6)) {
        throw new BadRequestException('batchDaysOfWeek must be 0–6 (Sun–Sat)');
      }
      return;
    }
    if (trainerGymUserId) {
      throw new BadRequestException(
        'trainerGymUserId is only valid for PT_PLAN',
      );
    }
    if (shifts?.length) {
      throw new BadRequestException('shifts are only valid for BATCH_PLAN');
    }
    if (batchDaysOfWeek?.length) {
      throw new BadRequestException(
        'batchDaysOfWeek is only valid for BATCH_PLAN',
      );
    }
    if (batchGender != null) {
      throw new BadRequestException('batchGender is only valid for BATCH_PLAN');
    }
  }

  private async assertTrainer(gymId: string, gymUserId: string) {
    const t = await this.prisma.gymUser.findFirst({
      where: {
        id: gymUserId,
        gymId,
        role: GymRole.TRAINER,
        isActive: true,
      },
      select: { id: true },
    });
    if (!t) {
      throw new BadRequestException('Invalid or inactive trainer for this gym');
    }
  }

  private mapCompatToCreate(dto: CreatePlanCompatDto): CreateGymPlanDto {
    return {
      type: this.compatType(dto.plan_type),
      name: dto.plan_name,
      durationDays: dto.duration_days,
      priceCents: dto.price * 100,
      trainerGymUserId: dto.trainer_id,
      shifts:
        dto.plan_type === 'batch' && dto.batch_details
          ? [
              {
                startTime: dto.batch_details.start_time,
                endTime: dto.batch_details.end_time,
                sortOrder: 0,
              },
            ]
          : undefined,
      batchDaysOfWeek:
        dto.plan_type === 'batch' && dto.batch_details
          ? dto.batch_details.working_days.map((d) => this.dayToInt(d))
          : undefined,
      batchGender:
        dto.plan_type === 'batch' && dto.batch_details
          ? dto.batch_details.gender === 'unisex'
            ? BatchPlanGender.ANY
            : dto.batch_details.gender === 'male'
              ? BatchPlanGender.MALE
              : BatchPlanGender.FEMALE
          : undefined,
      currency: 'USD',
      metadata: undefined,
    };
  }

  private mapCompatToUpdate(dto: UpdatePlanCompatDto): UpdateGymPlanDto {
    return {
      type: dto.plan_type ? this.compatType(dto.plan_type) : undefined,
      name: dto.plan_name,
      durationDays: dto.duration_days,
      priceCents: dto.price != null ? dto.price * 100 : undefined,
      trainerGymUserId: dto.trainer_id,
      shifts:
        dto.batch_details?.start_time && dto.batch_details?.end_time
          ? [
              {
                startTime: dto.batch_details.start_time,
                endTime: dto.batch_details.end_time,
                sortOrder: 0,
              },
            ]
          : undefined,
      batchDaysOfWeek: dto.batch_details?.working_days?.map((d) =>
        this.dayToInt(d),
      ),
      batchGender:
        dto.batch_details?.gender === undefined
          ? undefined
          : dto.batch_details.gender === 'unisex'
            ? BatchPlanGender.ANY
            : dto.batch_details.gender === 'male'
              ? BatchPlanGender.MALE
              : BatchPlanGender.FEMALE,
      currency: 'USD',
      isActive: undefined,
      metadata: undefined,
    };
  }

  private compatType(v: 'gym' | 'pt' | 'batch' | 'trial'): PlanType {
    switch (v) {
      case 'gym':
        return PlanType.GYM_MEMBERSHIP;
      case 'pt':
        return PlanType.PT_PLAN;
      case 'batch':
        return PlanType.BATCH_PLAN;
      case 'trial':
      default:
        return PlanType.FREE_TRIAL;
    }
  }

  private dayToInt(d: string): number {
    switch (d.toLowerCase()) {
      case 'sun':
        return 0;
      case 'mon':
        return 1;
      case 'tue':
        return 2;
      case 'wed':
        return 3;
      case 'thu':
        return 4;
      case 'fri':
        return 5;
      case 'sat':
        return 6;
      default:
        return 1;
    }
  }

  private addPlanDuration(startsAt: Date, durationDays: number): Date {
    const endsAt = new Date(startsAt);
    endsAt.setUTCDate(endsAt.getUTCDate() + durationDays);
    return endsAt;
  }

  private serializePlan(row: GymPlanRow) {
    return {
      id: row.id,
      gymId: row.gymId,
      type: row.type,
      name: row.name,
      durationDays: row.durationDays,
      priceCents: row.priceCents,
      currency: row.currency,
      isActive: row.isActive,
      metadata: row.metadata,
      trainer: row.trainerGymUser
        ? {
            gymUserId: row.trainerGymUser.id,
            name: row.trainerGymUser.user.fullName,
            phone: row.trainerGymUser.user.phone,
            userId: row.trainerGymUser.user.id,
          }
        : null,
      batch:
        row.type === PlanType.BATCH_PLAN
          ? {
              daysOfWeek: row.batchDaysOfWeek,
              gender: row.batchGender,
              shifts: row.shifts.map((s) => ({
                id: s.id,
                startTime: s.startTime,
                endTime: s.endTime,
                sortOrder: s.sortOrder,
              })),
            }
          : null,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
    };
  }
}
