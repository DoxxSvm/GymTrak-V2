import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { randomUUID } from 'crypto';
import { PrismaService } from '../prisma/prisma.service';
import {
  applyWorkoutDurationMinutes,
  computeWorkoutDurationMinutes,
  formatWorkoutDateLabel,
  formatWorkoutDurationLabel,
  workoutCompletionDateYmd,
} from '../../common/utils/workout-completion.util';
import type { UpdateWorkoutCompletionDto } from '../workouts/dto/update-workout-completion.dto';
import { WorkoutsService } from '../workouts/workouts.service';
import type { CreateMemberPersonalWorkoutDto } from './dto/create-member-personal-workout.dto';
import type { CreateTrainerExerciseDto } from '../workouts/dto/create-trainer-exercise.dto';
import type { UpdateTrainerExerciseDto } from '../workouts/dto/update-trainer-exercise.dto';
import type { UpdateTrainerWorkoutDto } from '../workouts/dto/update-trainer-workout.dto';
import type { StartMemberPersonalWorkoutDto } from './dto/start-member-personal-workout.dto';
import type { MemberPersonalWorkoutHistoryQueryDto } from './dto/member-personal-workout-history-query.dto';

const personalExerciseInclude = {
  secondaryMuscles: { select: { muscle: true } },
} satisfies Prisma.MemberPersonalExerciseInclude;

type PersonalExerciseRow = Prisma.MemberPersonalExerciseGetPayload<{
  include: typeof personalExerciseInclude;
}>;

@Injectable()
export class MemberPersonalCatalogService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly workouts: WorkoutsService,
  ) {}

  async createExercise(userId: string, dto: CreateTrainerExerciseDto) {
    const secondary = [...new Set(dto.secondary_muscles ?? [])].filter(
      (m) => m !== dto.primary_muscle,
    );
    const created = await this.prisma.memberPersonalExercise.create({
      data: {
        userId,
        name: dto.name.trim(),
        assetUrl: dto.asset_url?.trim() ?? null,
        equipment: dto.equipment,
        primaryMuscle: dto.primary_muscle,
        exerciseType: dto.exercise_type,
        isActive: dto.is_active ?? true,
        secondaryMuscles: {
          create: secondary.map((muscle) => ({ muscle })),
        },
      },
      select: { id: true, name: true },
    });
    return { id: created.id, name: created.name };
  }

  async listExercises(userId: string, search?: string) {
    const q = search?.trim();
    const where: Prisma.MemberPersonalExerciseWhereInput = {
      userId,
      isActive: true,
      ...(q ? { name: { contains: q, mode: 'insensitive' } } : {}),
    };
    const rows = await this.prisma.memberPersonalExercise.findMany({
      where,
      orderBy: { name: 'asc' },
      take: 500,
      include: personalExerciseInclude,
    });
    return rows.map((r) => this.exerciseToApi(r));
  }

  async getExercise(userId: string, exerciseId: string) {
    const row = await this.prisma.memberPersonalExercise.findFirst({
      where: { id: exerciseId, userId },
      include: personalExerciseInclude,
    });
    if (!row) {
      throw new NotFoundException('Exercise not found');
    }
    return this.exerciseToApi(row);
  }

  async updateExercise(
    userId: string,
    exerciseId: string,
    dto: UpdateTrainerExerciseDto,
  ) {
    const existing = await this.prisma.memberPersonalExercise.findFirst({
      where: { id: exerciseId, userId },
    });
    if (!existing) {
      throw new NotFoundException('Exercise not found');
    }
    const hasUpdate =
      dto.name !== undefined ||
      dto.asset_url !== undefined ||
      dto.equipment !== undefined ||
      dto.primary_muscle !== undefined ||
      dto.secondary_muscles !== undefined ||
      dto.exercise_type !== undefined ||
      dto.is_active !== undefined;
    if (!hasUpdate) {
      throw new BadRequestException('No fields to update');
    }
    const primaryAfter = dto.primary_muscle ?? existing.primaryMuscle;

    await this.prisma.$transaction(async (tx) => {
      if (dto.secondary_muscles !== undefined) {
        const secondary = [...new Set(dto.secondary_muscles)].filter(
          (m) => m !== primaryAfter,
        );
        await tx.memberPersonalExerciseSecondaryMuscle.deleteMany({
          where: { exerciseId },
        });
        if (secondary.length) {
          await tx.memberPersonalExerciseSecondaryMuscle.createMany({
            data: secondary.map((muscle) => ({ exerciseId, muscle })),
          });
        }
      } else if (dto.primary_muscle !== undefined) {
        await tx.memberPersonalExerciseSecondaryMuscle.deleteMany({
          where: { exerciseId, muscle: dto.primary_muscle },
        });
      }

      await tx.memberPersonalExercise.update({
        where: { id: exerciseId },
        data: {
          ...(dto.name !== undefined ? { name: dto.name.trim() } : {}),
          ...(dto.asset_url !== undefined
            ? { assetUrl: dto.asset_url?.trim() ?? null }
            : {}),
          ...(dto.equipment !== undefined ? { equipment: dto.equipment } : {}),
          ...(dto.primary_muscle !== undefined
            ? { primaryMuscle: dto.primary_muscle }
            : {}),
          ...(dto.exercise_type !== undefined
            ? { exerciseType: dto.exercise_type }
            : {}),
          ...(dto.is_active !== undefined ? { isActive: dto.is_active } : {}),
        },
      });
    });

    const updated = await this.prisma.memberPersonalExercise.findFirstOrThrow({
      where: { id: exerciseId, userId },
      include: personalExerciseInclude,
    });
    return this.exerciseToApi(updated);
  }

  async deleteExercise(userId: string, exerciseId: string) {
    const found = await this.prisma.memberPersonalExercise.findFirst({
      where: { id: exerciseId, userId },
      select: { id: true },
    });
    if (!found) {
      throw new NotFoundException('Exercise not found');
    }
    try {
      await this.prisma.memberPersonalExercise.delete({
        where: { id: exerciseId },
      });
    } catch (e) {
      if (
        e instanceof Prisma.PrismaClientKnownRequestError &&
        e.code === 'P2003'
      ) {
        throw new ConflictException(
          'Exercise is referenced by a workout and cannot be deleted',
        );
      }
      throw e;
    }
    return { success: true as const };
  }

  async createWorkout(userId: string, dto: CreateMemberPersonalWorkoutDto) {
    const workoutId = randomUUID();
    const startedAt = new Date();

    await this.prisma.$transaction(async (tx) => {
      await tx.memberPersonalWorkoutPlan.create({
        data: {
          id: workoutId,
          userId,
          title: dto.title.trim(),
          notes: dto.notes?.trim() ?? null,
          startedAt,
        },
      });

      for (const ex of dto.exercises) {
        const pex = await tx.memberPersonalExercise.findFirst({
          where: { id: ex.exercise_id, userId },
          select: { id: true },
        });
        if (!pex) {
          throw new BadRequestException(
            `Invalid exercise_id: ${ex.exercise_id}`,
          );
        }
        const weId = randomUUID();
        await tx.memberPersonalWorkoutPlanExercise.create({
          data: {
            id: weId,
            workoutId,
            exerciseId: ex.exercise_id,
            sets: {
              create: ex.sets.map((set) => ({
                id: randomUUID(),
                setNumber: set.set_number,
                reps: set.reps ?? 0,
                weight: set.weight ?? 0,
              })),
            },
          },
        });
      }
    });

    return this.getWorkoutDetails(userId, workoutId);
  }

  /**
   * Start an empty live session (no exercises yet). Client adds exercises or uses create with exercises later.
   */
  async startWorkoutSession(
    userId: string,
    dto: StartMemberPersonalWorkoutDto,
  ) {
    const startedAt = new Date();
    const title = (dto.title?.trim() || 'Workout') as string;
    const created = await this.prisma.memberPersonalWorkoutPlan.create({
      data: {
        userId,
        title,
        notes: dto.notes?.trim() ?? null,
        startedAt,
        completed: false,
      },
      select: { id: true },
    });
    return this.getWorkoutDetails(userId, created.id);
  }

  /**
   * End session: set `endedAt`, `completed`, and recompute volume/sets from logged sets.
   */
  async stopWorkoutSession(userId: string, workoutId: string) {
    const plan = await this.prisma.memberPersonalWorkoutPlan.findFirst({
      where: { id: workoutId, userId },
    });
    if (!plan) {
      throw new NotFoundException('Workout not found');
    }

    if (plan.endedAt != null && plan.completed) {
      return this.getWorkoutDetails(userId, workoutId);
    }

    const exercises =
      await this.prisma.memberPersonalWorkoutPlanExercise.findMany({
        where: { workoutId },
        include: {
          sets: { select: { reps: true, weight: true } },
        },
      });

    let totalVolume = 0;
    let totalSets = 0;
    for (const we of exercises) {
      for (const s of we.sets) {
        totalVolume += (s.reps ?? 0) * (s.weight ?? 0);
        totalSets += 1;
      }
    }

    const endedAt = new Date();
    const startedAt = plan.startedAt ?? endedAt;

    await this.prisma.memberPersonalWorkoutPlan.update({
      where: { id: workoutId },
      data: {
        startedAt: plan.startedAt ?? startedAt,
        endedAt,
        completed: true,
        totalVolume,
        totalSets,
        duration: formatWorkoutDurationLabel(
          computeWorkoutDurationMinutes(plan.startedAt ?? startedAt, endedAt),
        ),
      },
    });

    return this.getWorkoutCompletionSummary(userId, workoutId);
  }

  /**
   * Paginated history for `userId` (JWT `sub`) with optional date and completion filters.
   */
  async listWorkoutHistory(
    userId: string,
    query: MemberPersonalWorkoutHistoryQueryDto,
  ) {
    const page = query.page ?? 1;
    const limit = query.limit ?? 20;
    const offset = (page - 1) * limit;

    const where: Prisma.MemberPersonalWorkoutPlanWhereInput = {
      userId,
    };

    if (query.from != null && query.to != null) {
      where.startedAt = {
        gte: new Date(`${query.from}T00:00:00.000Z`),
        lte: new Date(`${query.to}T23:59:59.999Z`),
      };
    } else if (query.from != null) {
      where.startedAt = { gte: new Date(`${query.from}T00:00:00.000Z`) };
    } else if (query.to != null) {
      where.startedAt = { lte: new Date(`${query.to}T23:59:59.999Z`) };
    }

    if (query.completed !== undefined) {
      where.completed = query.completed;
    }

    const [rows, total] = await this.prisma.$transaction([
      this.prisma.memberPersonalWorkoutPlan.findMany({
        where,
        orderBy: { startedAt: 'desc' },
        skip: offset,
        take: limit,
        include: {
          _count: { select: { exercises: true } },
          exercises: {
            orderBy: { createdAt: 'asc' },
            include: {
              exercise: {
                select: { id: true, name: true, assetUrl: true },
              },
            },
          },
        },
      }),
      this.prisma.memberPersonalWorkoutPlan.count({ where }),
    ]);

    return {
      userId,
      page,
      limit,
      total,
      items: rows.map((w) => {
        const durationMin =
          w.startedAt && w.endedAt
            ? Math.max(
                0,
                Math.round(
                  (w.endedAt.getTime() - w.startedAt.getTime()) / 60000,
                ),
              )
            : 0;
        return {
          workout_id: w.id,
          user_id: w.userId,
          title: w.title,
          started_at: w.startedAt?.toISOString() ?? null,
          ended_at: w.endedAt?.toISOString() ?? null,
          completed: w.completed,
          total_volume: w.totalVolume,
          total_sets: w.totalSets,
          duration: w.startedAt && w.endedAt ? `${durationMin} min` : null,
          exercise_count: w._count.exercises,
          exercises: w.exercises.map((we) => ({
            exercise_id: we.exercise.id,
            name: we.exercise.name,
            asset_url: we.exercise.assetUrl,
          })),
        };
      }),
    };
  }

  async listWorkouts(userId: string) {
    const rows = await this.prisma.memberPersonalWorkoutPlan.findMany({
      where: { userId },
      orderBy: { startedAt: 'desc' },
      take: 200,
      include: {
        _count: { select: { exercises: true } },
      },
    });
    return rows.map((w) => ({
      workout_id: w.id,
      title: w.title,
      date: w.startedAt,
      exercise_count: w._count.exercises,
    }));
  }

  async getWorkoutDetails(userId: string, workoutId: string) {
    const summary = await this.loadPersonalWorkoutCompletionContext(
      userId,
      workoutId,
    );
    return this.buildPersonalWorkoutDetailResponse(summary);
  }

  async getWorkoutCompletionSummary(
    userId: string,
    workoutId: string,
    gymId?: string,
  ) {
    if (await this.isPersonalWorkout(userId, workoutId)) {
      return this.getPersonalWorkoutCompletionSummary(userId, workoutId);
    }
    return this.workouts.getWorkoutCompletionSummary(
      userId,
      workoutId,
      gymId,
    );
  }

  async updateWorkoutCompletionSummary(
    userId: string,
    workoutId: string,
    dto: UpdateWorkoutCompletionDto,
    gymId?: string,
  ) {
    if (await this.isPersonalWorkout(userId, workoutId)) {
      return this.updatePersonalWorkoutCompletionSummary(userId, workoutId, dto);
    }
    return this.workouts.updateWorkoutCompletionSummary(
      userId,
      workoutId,
      dto,
      gymId,
    );
  }

  private async isPersonalWorkout(
    userId: string,
    workoutId: string,
  ): Promise<boolean> {
    const row = await this.prisma.memberPersonalWorkoutPlan.findFirst({
      where: { id: workoutId, userId },
      select: { id: true },
    });
    return row != null;
  }

  private async getPersonalWorkoutCompletionSummary(
    userId: string,
    workoutId: string,
  ) {
    const summary = await this.loadPersonalWorkoutCompletionContext(
      userId,
      workoutId,
    );
    if (!summary.plan.completed) {
      throw new BadRequestException(
        'Workout is not completed yet; stop the session first',
      );
    }
    return this.buildPersonalWorkoutCompletionSummary(summary);
  }

  private async updatePersonalWorkoutCompletionSummary(
    userId: string,
    workoutId: string,
    dto: UpdateWorkoutCompletionDto,
  ) {
    const summary = await this.loadPersonalWorkoutCompletionContext(
      userId,
      workoutId,
    );
    if (!summary.plan.completed) {
      throw new BadRequestException(
        'Only completed workouts can be edited from the completion screen',
      );
    }

    const { startedAt, endedAt } = applyWorkoutDurationMinutes(
      summary.plan.startedAt,
      summary.plan.endedAt,
      dto.duration_minutes,
    );

    await this.prisma.memberPersonalWorkoutPlan.update({
      where: { id: workoutId },
      data: {
        startedAt,
        endedAt,
        duration: formatWorkoutDurationLabel(dto.duration_minutes),
      },
    });

    return this.getWorkoutCompletionSummary(userId, workoutId);
  }

  private async loadPersonalWorkoutCompletionContext(
    userId: string,
    workoutId: string,
  ) {
    const row = await this.prisma.memberPersonalWorkoutPlan.findFirst({
      where: { id: workoutId, userId },
      select: {
        id: true,
        title: true,
        startedAt: true,
        endedAt: true,
        totalVolume: true,
        totalSets: true,
        completed: true,
      },
    });
    if (!row) {
      throw new NotFoundException('Workout not found');
    }

    const exercises =
      await this.prisma.memberPersonalWorkoutPlanExercise.findMany({
        where: { workoutId },
        include: {
          exercise: { select: { id: true, name: true } },
          sets: {
            orderBy: { setNumber: 'asc' },
            select: {
              id: true,
              setNumber: true,
              reps: true,
              weight: true,
              completed: true,
            },
          },
        },
      });

    const grouped = exercises.map((we) => ({
      exercise_id: we.exercise.id,
      name: we.exercise.name,
      sets: we.sets.map((s) => ({
        id: s.id,
        set_number: s.setNumber,
        reps: s.reps,
        weight: s.weight,
        completed: s.completed,
      })),
    }));

    return {
      plan: row,
      exercises: grouped,
    };
  }

  private buildPersonalWorkoutDetailResponse(summary: {
    plan: {
      id: string;
      title: string;
      startedAt: Date | null;
      endedAt: Date | null;
      totalVolume: number;
      totalSets: number;
    };
    exercises: Array<Record<string, unknown>>;
  }) {
    const durationMin = computeWorkoutDurationMinutes(
      summary.plan.startedAt,
      summary.plan.endedAt,
    );
    return {
      workout_id: summary.plan.id,
      title: summary.plan.title,
      duration: formatWorkoutDurationLabel(durationMin),
      volume: `${summary.plan.totalVolume ?? 0} kg`,
      sets: summary.plan.totalSets ?? 0,
      exercises: summary.exercises,
    };
  }

  private buildPersonalWorkoutCompletionSummary(summary: {
    plan: {
      id: string;
      title: string;
      startedAt: Date | null;
      endedAt: Date | null;
      totalVolume: number;
      totalSets: number;
      completed: boolean;
    };
    exercises: Array<Record<string, unknown>>;
  }) {
    const durationMin = computeWorkoutDurationMinutes(
      summary.plan.startedAt,
      summary.plan.endedAt,
    );
    const completionAt =
      summary.plan.endedAt ?? summary.plan.startedAt ?? new Date();
    return {
      workout_id: summary.plan.id,
      title: summary.plan.title,
      completed: summary.plan.completed,
      date: workoutCompletionDateYmd(completionAt),
      date_label: formatWorkoutDateLabel(completionAt),
      started_at: summary.plan.startedAt?.toISOString() ?? null,
      ended_at: summary.plan.endedAt?.toISOString() ?? null,
      duration_minutes: durationMin,
      duration: formatWorkoutDurationLabel(durationMin),
      volume_kg: summary.plan.totalVolume ?? 0,
      volume: `${summary.plan.totalVolume ?? 0} kg`,
      sets: summary.plan.totalSets ?? 0,
      exercises: summary.exercises,
    };
  }

  async updateWorkout(
    userId: string,
    workoutId: string,
    dto: UpdateTrainerWorkoutDto,
  ) {
    const existing = await this.prisma.memberPersonalWorkoutPlan.findFirst({
      where: { id: workoutId, userId },
      select: { title: true, notes: true },
    });
    if (!existing) {
      throw new NotFoundException('Workout not found');
    }
    const hasUpdate = dto.title !== undefined || dto.notes !== undefined;
    if (!hasUpdate) {
      throw new BadRequestException('No fields to update');
    }
    const title = dto.title !== undefined ? dto.title.trim() : existing.title;
    const notes =
      dto.notes !== undefined ? (dto.notes?.trim() ?? null) : existing.notes;

    await this.prisma.memberPersonalWorkoutPlan.update({
      where: { id: workoutId },
      data: { title, notes },
    });

    return this.getWorkoutDetails(userId, workoutId);
  }

  async deleteWorkout(userId: string, workoutId: string) {
    const res = await this.prisma.memberPersonalWorkoutPlan.deleteMany({
      where: { id: workoutId, userId },
    });
    if (res.count === 0) {
      throw new NotFoundException('Workout not found');
    }
    return { success: true as const };
  }

  private exerciseToApi(r: PersonalExerciseRow) {
    return {
      id: r.id,
      name: r.name,
      asset_url: r.assetUrl,
      equipment: r.equipment,
      primary_muscle: r.primaryMuscle,
      secondary_muscles: r.secondaryMuscles.map((s) => s.muscle),
      exercise_type: r.exerciseType,
      is_active: r.isActive,
      created_at: r.createdAt.toISOString(),
    };
  }
}
