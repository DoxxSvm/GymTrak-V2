import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  BatchPlanGender,
  GymRole,
  MemberSubscriptionStatus,
  PlanType,
  Prisma,
} from '@prisma/client';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import type { CreateGymPlanDto } from './dto/create-gym-plan.dto';
import {
  normalizeCompatPlanType,
  type CreatePlanCompatDto,
} from './dto/create-plan-compat.dto';
import type { UpdatePlanCompatDto } from './dto/update-plan-compat.dto';
import type { UpdateGymPlanDto } from './dto/update-gym-plan.dto';
import type { AssignMemberPlanBodyDto } from './dto/assign-member-plan.dto';
import type {
  FreezeMemberPlanDto,
  UnfreezeMemberPlanDto,
} from './dto/freeze-member-plan.dto';

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
  ) { }

  async list(
    actorUserId: string,
    gymId: string,
    type: PlanType | undefined,
    limit: number,
    offset: number,
    includeInactive = false,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const where: Prisma.GymPlanWhereInput = {
      gymId,
      ...(type ? { type } : {}),
      ...(!includeInactive ? { isActive: true } : {}),
    };
    const [total, items, subscriptionCount] = await Promise.all([
      this.prisma.gymPlan.count({ where }),
      this.prisma.gymPlan.findMany({
        where,
        orderBy: [{ name: 'asc' }, { id: 'asc' }],
        take: limit,
        skip: offset,
        include: planInclude,
      }),
      this.prisma.memberSubscription.findMany({
        where: {
          gymUser: {
            gymId,
            role: GymRole.MEMBER,
          },
          status: MemberSubscriptionStatus.ACTIVE,
        },
        distinct: ['gymUserId'],
        select: {
          gymUserId: true,
        },
      })
    ]);

    return {
      total,
      limit,
      offset,
      items: items.map((p) => this.serializePlan(p)),
      subscriptionCount: subscriptionCount.length,
    };
  }

  async createCompat(actorUserId: string, dto: CreatePlanCompatDto) {
    const mapped = this.mapCompatToCreate(dto);
    return this.create(actorUserId, dto.gymId, mapped);
  }

  async getOne(actorUserId: string, gymId: string, planId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.gymPlan.findFirst({
      where: { id: planId, gymId, isActive: true },
      include: planInclude,
    });
    if (!row) {
      throw new NotFoundException('Plan not found');
    }
    return this.serializePlan(row);
  }

  async create(actorUserId: string, gymId: string, dto: CreateGymPlanDto) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const currency = dto.currency?.trim() || 'INR';
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
    if (!existing.isActive && dto.isActive !== true) {
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
          ? { currency: dto.currency.trim() || 'INR' }
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
        (nextType === PlanType.PT_PLAN || nextType === PlanType.BATCH_PLAN) &&
        (dto.trainerGymUserId !== undefined || typeChanged)
      ) {
        if (trainerId) {
          data.trainerGymUser = { connect: { id: trainerId } };
        }
      } else if (
        typeChanged &&
        nextType !== PlanType.PT_PLAN &&
        nextType !== PlanType.BATCH_PLAN
      ) {
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
    if (!row.isActive) {
      return { ok: true };
    }

    const assignedCount = await this.prisma.memberSubscription.count({
      where: {
        gymPlanId: planId,
        status: {
          in: [
            MemberSubscriptionStatus.ACTIVE,
            MemberSubscriptionStatus.SCHEDULED,
            MemberSubscriptionStatus.FROZEN,
          ],
        },
      },
    });
    if (assignedCount > 0) {
      throw new ConflictException(
        `Cannot delete this plan — ${assignedCount} member${assignedCount > 1 ? 's' : ''} currently ${assignedCount > 1 ? 'have' : 'has'} an active or scheduled subscription on it. Remove or change their subscriptions first.`,
      );
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
    body: AssignMemberPlanBodyDto,
  ) {
    return this.subscriptions.assignGymPlanToMember(actorUserId, body);
  }

  async freezeMemberPlan(actorUserId: string, body: FreezeMemberPlanDto) {
    return this.subscriptions.freeze(
      actorUserId,
      body.gymId,
      body.member_subscription_id,
      {
        freeze_start_date: body.freeze_start_date,
        duration_days: body.duration_days,
        freeze_fee: body.freeze_fee,
        reason: body.reason,
      },
    );
  }

  async unfreezeMemberPlan(actorUserId: string, body: UnfreezeMemberPlanDto) {
    return this.subscriptions.unfreeze(
      actorUserId,
      body.gymId,
      body.member_subscription_id,
    );
  }

  validateCompatPayload(body: object) {
    const rec = body as Record<string, unknown>;
    const planType = normalizeCompatPlanType(rec.planType ?? rec.plan_type);
    const missing: string[] = [];
    if (!planType) {
      missing.push('planType');
    }
    const trainerRaw = rec.trainerId ?? rec.trainer_id;
    const trainerId =
      typeof trainerRaw === 'string' ? trainerRaw.trim() : undefined;

    if (planType === PlanType.PT_PLAN && !trainerId) {
      missing.push('trainerId');
    }
    if (planType === PlanType.BATCH_PLAN) {
      if (!trainerId) {
        missing.push('trainerId');
      }
      const bd = rec.batch_details as Record<string, unknown> | undefined;
      if (
        !bd ||
        !Array.isArray(bd.working_days) ||
        bd.working_days.length === 0
      ) {
        missing.push('batch_details.working_days');
      }
      const shifts = bd?.shifts;
      const hasShifts = Array.isArray(shifts) && shifts.length > 0;
      const legacy =
        typeof bd?.start_time === 'string' &&
        bd.start_time.trim().length > 0 &&
        typeof bd?.end_time === 'string' &&
        bd.end_time.trim().length > 0;
      if (!hasShifts && !legacy) {
        missing.push('batch_details.shifts');
      }
      if (!bd?.gender || !String(bd.gender).trim()) {
        missing.push('batch_details.gender');
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
        ...(dto.trainerGymUserId
          ? {
            trainerGymUser: {
              connect: { id: dto.trainerGymUserId },
            },
          }
          : {}),
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
      return {};
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
        throw new BadRequestException('batchDaysOfWeek must be 0–6');
      }
      if (!trainerGymUserId?.trim()) {
        throw new BadRequestException('Batch plan requires trainer');
      }
      await this.assertTrainer(gymId, trainerGymUserId.trim());
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
    const type = dto.planType;
    const batchDetails =
      type === PlanType.BATCH_PLAN ? dto.batch_details : undefined;
    const shiftRows =
      batchDetails != null
        ? this.resolveCompatBatchShifts(batchDetails).map((s, i) => ({
          startTime: this.normalizeCompatTime(s.start_time),
          endTime: this.normalizeCompatTime(s.end_time),
          sortOrder: i,
        }))
        : undefined;

    return {
      type,
      name: dto.planName.trim(),
      durationDays: dto.durationDays,
      priceCents: dto.price,
      trainerGymUserId: dto.trainerId,
      shifts: shiftRows,
      batchDaysOfWeek:
        batchDetails != null
          ? batchDetails.working_days.map((d) => this.mapCompatDayToken(d))
          : undefined,
      batchGender:
        batchDetails != null
          ? this.mapCompatGender(String(batchDetails.gender))
          : undefined,
      currency: 'INR',
      metadata: undefined,
    };
  }

  private mapCompatToUpdate(dto: UpdatePlanCompatDto): UpdateGymPlanDto {
    const out: UpdateGymPlanDto = {};
    if (dto.planType !== undefined) {
      out.type = dto.planType;
    }
    if (dto.planName !== undefined) {
      out.name = dto.planName.trim();
    }
    if (dto.durationDays !== undefined) {
      out.durationDays = dto.durationDays;
    }
    if (dto.price !== undefined) {
      out.priceCents = dto.price;
    }
    if (dto.trainerId !== undefined) {
      out.trainerGymUserId = dto.trainerId;
    }
    if (dto.batch_details) {
      const shifts = this.resolveCompatBatchShifts(dto.batch_details).map(
        (s, i) => ({
          startTime: this.normalizeCompatTime(s.start_time),
          endTime: this.normalizeCompatTime(s.end_time),
          sortOrder: i,
        }),
      );
      out.shifts = shifts;
      out.batchDaysOfWeek = dto.batch_details.working_days.map((d) =>
        this.mapCompatDayToken(d),
      );
      out.batchGender = this.mapCompatGender(String(dto.batch_details.gender));
    }
    out.currency = 'INR';
    return out;
  }

  private resolveCompatBatchShifts(batch: {
    shifts?: { start_time: string; end_time: string }[];
    start_time?: string;
    end_time?: string;
  }): { start_time: string; end_time: string }[] {
    if (batch.shifts?.length) {
      return batch.shifts;
    }
    if (batch.start_time?.trim() && batch.end_time?.trim()) {
      return [{ start_time: batch.start_time, end_time: batch.end_time }];
    }
    throw new BadRequestException(
      'batch_details requires shifts[] (start_time/end_time each) or legacy start_time and end_time',
    );
  }

  private normalizeCompatTime(raw: string): string {
    const s = raw.trim();
    const ampm = /^(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(AM|PM)$/i.exec(s);
    if (ampm) {
      let h = parseInt(ampm[1]!, 10);
      const m = ampm[2]!.padStart(2, '0');
      const mer = ampm[4]!.toUpperCase();
      if (mer === 'PM' && h !== 12) {
        h += 12;
      }
      if (mer === 'AM' && h === 12) {
        h = 0;
      }
      if (h > 23 || parseInt(m, 10) > 59) {
        throw new BadRequestException(`Invalid time "${raw}"`);
      }
      return `${h}:${m}`;
    }
    const twenty = /^(\d{1,2}):(\d{2})$/.exec(s);
    if (twenty) {
      const h = parseInt(twenty[1]!, 10);
      const m = twenty[2]!.padStart(2, '0');
      if (h > 23 || parseInt(m, 10) > 59) {
        throw new BadRequestException(`Invalid time "${raw}"`);
      }
      return `${h}:${m}`;
    }
    throw new BadRequestException(
      `Invalid time "${raw}"; use HH:mm or h:mm AM/PM`,
    );
  }

  /**
   * Compat APIs accept Monday-indexed days: 0=Mon, 1=Tue, …, 6=Sun.
   * Converts to DB format (0=Sun, 1=Mon, …, 6=Sat).
   */
  private mapCompatDayToken(d: string | number): number {
    if (typeof d === 'number') {
      if (!Number.isInteger(d) || d < 0 || d > 6) {
        throw new BadRequestException(
          `Invalid day index ${d}; use 0–6 (Mon–Sun)`,
        );
      }
      // Monday-indexed → DB (Sunday-indexed): (input + 1) % 7
      return (d + 1) % 7;
    }
    const s = String(d).trim();
    if (/^\d+$/.test(s)) {
      const n = parseInt(s, 10);
      if (n < 0 || n > 6) {
        throw new BadRequestException(
          `Invalid day index ${n}; use 0–6 (Mon–Sun)`,
        );
      }
      return (n + 1) % 7;
    }
    const key = s.toLowerCase();
    const map: Record<string, number> = {
      sun: 0,
      sunday: 0,
      s_sun: 0,
      mon: 1,
      monday: 1,
      m: 1,
      tue: 2,
      tues: 2,
      tuesday: 2,
      t: 2,
      wed: 3,
      wednesday: 3,
      w: 3,
      thu: 4,
      thur: 4,
      thurs: 4,
      thursday: 4,
      fri: 5,
      friday: 5,
      f: 5,
      sat: 6,
      saturday: 6,
    };
    if (key in map) {
      return map[key]!;
    }
    throw new BadRequestException(`Invalid working day "${d}"`);
  }

  private mapCompatGender(raw: string): BatchPlanGender {
    const g = raw.trim().toLowerCase();
    if (g === 'male' || g === 'm') {
      return BatchPlanGender.MALE;
    }
    if (g === 'female' || g === 'f') {
      return BatchPlanGender.FEMALE;
    }
    if (g === 'unisex' || g === 'any') {
      return BatchPlanGender.ANY;
    }
    if (g === 'mixed') {
      return BatchPlanGender.MIXED;
    }
    const upper = raw.trim().toUpperCase();
    if (upper === 'MALE') {
      return BatchPlanGender.MALE;
    }
    if (upper === 'FEMALE') {
      return BatchPlanGender.FEMALE;
    }
    if (upper === 'ANY' || upper === 'UNISEX') {
      return BatchPlanGender.ANY;
    }
    if (upper === 'MIXED') {
      return BatchPlanGender.MIXED;
    }
    throw new BadRequestException(
      `Invalid batch gender "${raw}"; use male, female, unisex, or mixed`,
    );
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
            daysOfWeek: row.batchDaysOfWeek.map(
              (d) => (d + 6) % 7,
            ),
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
