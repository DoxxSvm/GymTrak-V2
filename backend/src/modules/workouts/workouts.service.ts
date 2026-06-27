import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
  ServiceUnavailableException,
} from '@nestjs/common';
import {
  ExerciseEquipment,
  ExerciseType,
  GymRole,
  Muscle,
  Prisma,
} from '@prisma/client';
import { randomUUID } from 'crypto';
import {
  applyWorkoutDurationMinutes,
  computeWorkoutDurationMinutes,
  formatWorkoutDateLabel,
  formatWorkoutDurationLabel,
  workoutCompletionDateYmd,
} from '../../common/utils/workout-completion.util';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type { AddExerciseToWorkoutDto } from './dto/add-exercise-to-workout.dto';
import type { AddSetDto } from './dto/add-set.dto';
import type { CreateTrainerExerciseDto } from './dto/create-trainer-exercise.dto';
import type { PatchWorkoutBodyDto } from './dto/patch-workout-body.dto';
import type { CreateWorkoutDto } from './dto/create-workout.dto';
import type {
  ExerciseDetailChartMetric,
  ExerciseDetailChartRange,
  ExerciseDetailsQueryDto,
} from './dto/exercise-details-query.dto';
import type { ListTrainerExercisesQueryDto } from './dto/list-trainer-exercises-query.dto';
import type { ListTrainerWorkoutsQueryDto } from './dto/list-trainer-workouts-query.dto';
import type { ListWorkoutsQueryDto } from './dto/list-workouts-query.dto';
import type { UpdateWorkoutCompletionDto } from './dto/update-workout-completion.dto';
import type { UpdateTrainerWorkoutDto } from './dto/update-trainer-workout.dto';
import type { UpdateTrainerExerciseDto } from './dto/update-trainer-exercise.dto';
import type { UpdateSetDto } from './dto/update-set.dto';
import type { WorkoutIdOnlyDto } from './dto/workout-id-only.dto';
import type { UnifiedWorkoutHistoryQueryDto } from './dto/unified-workout-history-query.dto';
import { workoutCreatedByBucket, workoutCreatorDisplayName } from './workout-meta.constants';

@Injectable()
export class WorkoutsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) { }

  /**
   * With `?gymId`: `gymId` + `userId` = actor, gym must be manageable.
   * Without: personal catalog — `gymId` = null, `userId` = actor (like diet foods; JWT `gymId` ignored).
   */
  async createExercise(
    actorUserId: string,
    dto: CreateTrainerExerciseDto,
    gymIdFromQuery?: string,
  ) {
    const secondary = [...new Set(dto.secondary_muscles ?? [])].filter(
      (m) => m !== dto.primary_muscle,
    );

    const trimmedGym = gymIdFromQuery?.trim();
    if (trimmedGym) {
      await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
      const created = await this.prisma.exercise.create({
        data: {
          userId: actorUserId,
          gymId: trimmedGym,
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
    const created = await this.prisma.exercise.create({
      data: {
        userId: actorUserId,
        gymId: null,
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

  async listExercises(
    actorUserId: string,
    query: ListTrainerExercisesQueryDto,
  ) {
    const trimmedGym = query.gymId?.trim();
    const search = query.search?.trim();

    const base: Prisma.ExerciseWhereInput = {
      userId: actorUserId,
      isActive: true,
      ...(search
        ? { name: { contains: search, mode: 'insensitive' } }
        : undefined),
      ...(query.equipment ? { equipment: query.equipment } : undefined),
      ...(query.muscle
        ? {
          OR: [
            { primaryMuscle: query.muscle },
            { secondaryMuscles: { some: { muscle: query.muscle } } },
          ],
        }
        : undefined),
    };

    let where: Prisma.ExerciseWhereInput;
    if (trimmedGym) {
      await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
      where = { ...base, gymId: trimmedGym };
    } else {
      where = { ...base, gymId: null };
    }

    const rows = await this.prisma.exercise.findMany({
      where,
      orderBy: { name: 'asc' },
      take: 500,
      include: { secondaryMuscles: { select: { muscle: true } } },
    });

    return rows.map((r) => this.exerciseToApi(r));
  }

  /**
   * Exercise detail for the Summary + History UI: catalog metadata, PRs, chart buckets,
   * and paginated session logs from `member_workout_plans` / `workout_plan_exercises`.
   *
   * **Personal catalog** (`Exercise.gymId` null): stats are this user’s personal logs
   * (`gym_id` null on the workout). **Gym catalog**: stats are the caller’s **member**
   * sessions at that gym (`gym_user_id` = their `GymUser` when role is MEMBER); staff
   * / owners without a member row see metadata with empty stats. Optional `gymId` on
   * a gym-scoped exercise must match the row.
   */
  async getExerciseDetails(
    actorUserId: string,
    exerciseId: string,
    query: ExerciseDetailsQueryDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const exercise = await this.prisma.exercise.findFirst({
        where: { id: exerciseId },
        include: { secondaryMuscles: { select: { muscle: true } } },
      });
      if (!exercise) {
        throw new NotFoundException('Exercise not found');
      }

      const qGym = query.gymId?.trim();
      if (exercise.gymId == null) {
        if (qGym) {
          throw new BadRequestException(
            'This exercise is personal-scoped; omit gymId',
          );
        }
        if (exercise.userId !== actorUserId) {
          throw new ForbiddenException(
            'You can only access your own exercise catalog entries',
          );
        }
      } else {
        await this.gymAccess.assertCanBrowseGymCatalog(
          actorUserId,
          exercise.gymId,
        );
        if (qGym && qGym !== exercise.gymId) {
          throw new BadRequestException('gymId does not match this exercise');
        }
      }

      const chartRange = query.chart_range ?? '3m';
      const chartMetric = query.chart_metric ?? 'heaviest_weight';
      const historyPage = query.history_page ?? 1;
      const historyLimit = query.history_limit ?? 20;

      const planWhere = await this.buildExerciseDetailPlanFilter(
        actorUserId,
        exercise.gymId,
      );

      const exercisePayload = this.exerciseToApi(exercise);

      if (!planWhere) {
        return {
          exercise: exercisePayload,
          summary: this.emptyExerciseSummary(chartRange, chartMetric),
          history: {
            items: [],
            page: historyPage,
            limit: historyLimit,
            total: 0,
          },
        };
      }

      const blocks = await this.prisma.workoutPlanExercise.findMany({
        where: { exerciseId, workout: planWhere },
        select: {
          id: true,
          workout: {
            select: {
              id: true,
              title: true,
              startedAt: true,
              endedAt: true,
              createdAt: true,
            },
          },
          sets: {
            select: {
              setNumber: true,
              weight: true,
              reps: true,
              completed: true,
            },
            orderBy: { setNumber: 'asc' },
          },
        },
      });

      const flat = this.flattenExerciseDetailSets(blocks);
      const summary = this.buildExerciseSummaryStats(
        flat,
        chartRange,
        chartMetric,
      );

      const sortedBlocks = [...blocks].sort(
        (a, b) =>
          this.workoutPerformedAt(b.workout).getTime() -
          this.workoutPerformedAt(a.workout).getTime(),
      );
      const total = sortedBlocks.length;
      const start = (historyPage - 1) * historyLimit;
      const pageBlocks = sortedBlocks.slice(start, start + historyLimit);
      const historyItems = pageBlocks.map((b) => ({
        workout_id: b.workout.id,
        title: b.workout.title,
        performed_at: this.workoutPerformedAt(b.workout).toISOString(),
        sets: b.sets.map((s) => ({
          set_number: s.setNumber,
          weight_kg: s.weight,
          reps: s.reps,
          completed: s.completed,
        })),
      }));

      return {
        exercise: exercisePayload,
        summary,
        history: {
          items: historyItems,
          page: historyPage,
          limit: historyLimit,
          total,
        },
      };
    });
  }

  async updateExercise(
    actorUserId: string,
    exerciseId: string,
    dto: UpdateTrainerExerciseDto,
    gymId?: string,
  ) {
    const existing = await this.prisma.exercise.findFirst({
      where: { id: exerciseId },
    });
    if (!existing) {
      throw new NotFoundException('Exercise not found');
    }
    this.assertCatalogExerciseOwner(existing, actorUserId);
    const gq = gymId?.trim();
    if (gq) {
      if (existing.gymId !== gq) {
        throw new BadRequestException('gymId does not match this exercise');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, gq);
    } else if (existing.gymId != null) {
      throw new BadRequestException(
        'This exercise is gym-scoped; pass gymId as a query parameter',
      );
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
        await tx.exerciseSecondaryMuscle.deleteMany({
          where: { exerciseId },
        });
        if (secondary.length) {
          await tx.exerciseSecondaryMuscle.createMany({
            data: secondary.map((muscle) => ({ exerciseId, muscle })),
          });
        }
      } else if (dto.primary_muscle !== undefined) {
        await tx.exerciseSecondaryMuscle.deleteMany({
          where: { exerciseId, muscle: dto.primary_muscle },
        });
      }

      await tx.exercise.update({
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

    const updated = await this.prisma.exercise.findFirstOrThrow({
      where: {
        id: exerciseId,
        userId: actorUserId,
        gymId: existing.gymId,
      },
      include: { secondaryMuscles: { select: { muscle: true } } },
    });
    return this.exerciseToApi(updated);
  }

  async deleteExercise(
    actorUserId: string,
    exerciseId: string,
    gymIdFromQuery?: string,
  ) {
    const existing = await this.prisma.exercise.findFirst({
      where: { id: exerciseId },
      select: { id: true, gymId: true, userId: true },
    });
    if (!existing) {
      throw new NotFoundException('Exercise not found');
    }
    this.assertCatalogExerciseOwner(
      { userId: existing.userId, gymId: existing.gymId },
      actorUserId,
    );
    const gq = gymIdFromQuery?.trim();
    if (gq) {
      if (existing.gymId !== gq) {
        throw new BadRequestException('gymId does not match this exercise');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, gq);
    } else if (existing.gymId != null) {
      throw new BadRequestException(
        'This exercise is gym-scoped; pass gymId as a query parameter',
      );
    }
    try {
      const r = await this.prisma.exercise.deleteMany({
        where: {
          id: exerciseId,
          userId: actorUserId,
          gymId: existing.gymId,
        },
      });
      if (r.count === 0) {
        throw new NotFoundException('Exercise not found');
      }
    } catch (e) {
      if (
        e instanceof Prisma.PrismaClientKnownRequestError &&
        (e.code === 'P2003' || e.code === 'P2014')
      ) {
        throw new ConflictException(
          'Exercise is referenced by a workout and cannot be deleted',
        );
      }
      throw e;
    }
    return { success: true as const };
  }

  /**
   * `POST /workouts` / `POST /trainers/workouts`:
   * - Body `member_id` (GymUser id): gym log — `user_id` = JWT `sub` (trainer), `gym_user_id` = member. `gymId` query optional; omit it and gym is taken from the member row; if sent, must match that member’s gym.
   * - No `member_id`: personal log for JWT `sub` only.
   */
  async createWorkout(
    actorUserId: string,
    dto: CreateWorkoutDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(() =>
      this.createWorkoutScoped(actorUserId, dto, gymIdFromQuery),
    );
  }

  async createTrainerWorkout(
    actorUserId: string,
    dto: CreateWorkoutDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(() =>
      this.createWorkoutScoped(actorUserId, dto, gymIdFromQuery),
    );
  }

  private async createWorkoutScoped(
    actorUserId: string,
    dto: CreateWorkoutDto,
    gymIdFromQuery?: string,
  ) {
    const trimmedGym = gymIdFromQuery?.trim();
    if (dto.member_id?.trim()) {
      const memberWhere = {
        userId: dto.member_id?.trim(),
        role: GymRole.MEMBER,
        isActive: true,
        ...(trimmedGym ? { gymId: trimmedGym } : {}),
      };
      const member = await this.prisma.gymUser.findFirst({
        where: memberWhere,
        select: { id: true, gymId: true, userId: true },
      });
      if (!member) {
        throw new NotFoundException(
          trimmedGym ? 'Member not found in this gym' : 'Member not found',
        );
      }
      await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);
      const workoutId = await this.persistGymWorkout(actorUserId, member, dto);
      return this.getWorkoutDetails(actorUserId, workoutId);
    }
    if (trimmedGym) {
      throw new BadRequestException(
        'member_id is required when gymId query is set',
      );
    }
    const workoutId = await this.persistPersonalWorkout(actorUserId, dto);
    return this.getWorkoutDetails(actorUserId, workoutId);
  }

  /** `POST /workouts/start` — empty in-progress session on `member_workout_plans`. */
  /**
   * `POST /workouts/start` — body `{ workout_id }` only. (Re)opens an active session:
   * sets `started_at` to now, clears `ended_at`, sets `completed` false and `isStarted` true.
   * Resumes a paused session via `resumeUnifiedWorkout`. Otherwise clones to a new plan id
   * so the library template row is unchanged. Idempotent if already in progress.
   */
  async startUnifiedWorkout(
    actorUserId: string,
    dto: WorkoutIdOnlyDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const wid = dto.workout_id.trim();
      const plan = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: wid },
        select: {
          id: true,
          userId: true,
          gymId: true,
          gymUserId: true,
          startedAt: true,
          endedAt: true,
          completed: true,
          isStarted: true,
        },
      });
      if (!plan) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, plan);
      this.assertOptionalGymIdMatchesWorkout(plan.gymId, gymIdFromQuery);

      const inProgress =
        !plan.completed &&
        plan.endedAt == null &&
        plan.startedAt != null &&
        plan.isStarted;
      if (inProgress) {
        return this.getWorkoutDetails(actorUserId, wid);
      }

      const paused =
        !plan.completed &&
        plan.endedAt == null &&
        plan.startedAt != null &&
        !plan.isStarted;
      if (paused) {
        return this.resumeUnifiedWorkout(actorUserId, dto, gymIdFromQuery);
      }

      const now = new Date();
      const activeWorkoutId = await this.cloneMemberWorkoutPlanForNewSession(
        wid,
        now,
      );
      return this.getWorkoutDetails(actorUserId, activeWorkoutId);
    });
  }

  /** `POST /workouts/stop` — finalize session (totals from all sets, `completed=true`). */
  async stopUnifiedWorkout(
    actorUserId: string,
    dto: WorkoutIdOnlyDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const wid = dto.workout_id.trim();
      const plan = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: wid },
        select: {
          id: true,
          userId: true,
          gymId: true,
          gymUserId: true,
          startedAt: true,
          endedAt: true,
          completed: true,
        },
      });
      if (!plan) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, plan);
      this.assertOptionalGymIdMatchesWorkout(plan.gymId, gymIdFromQuery);
      if (plan.endedAt != null && plan.completed) {
        return this.getWorkoutDetails(actorUserId, wid);
      }

      const { totalVolume, totalSets } =
        await this.aggregateMemberWorkoutTotalsAllSets(wid);
      const endedAt = new Date();
      await this.prisma.memberWorkoutPlan.update({
        where: { id: wid },
        data: {
          startedAt: plan.startedAt ?? endedAt,
          endedAt,
          completed: true,
          totalVolume,
          totalSets,
          isStarted: false,
        },
      });
      return this.getWorkoutDetails(actorUserId, wid);
    });
  }

  /** `POST /workouts/pause` — pause an in-progress session (`isStarted=false`, session stays open). */
  async pauseUnifiedWorkout(
    actorUserId: string,
    dto: WorkoutIdOnlyDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const wid = dto.workout_id.trim();
      const plan = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: wid },
        select: {
          id: true,
          userId: true,
          gymId: true,
          gymUserId: true,
          startedAt: true,
          endedAt: true,
          completed: true,
          isStarted: true,
        },
      });
      if (!plan) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, plan);
      this.assertOptionalGymIdMatchesWorkout(plan.gymId, gymIdFromQuery);

      const paused =
        !plan.completed &&
        plan.endedAt == null &&
        plan.startedAt != null &&
        !plan.isStarted;
      if (paused) {
        return this.getWorkoutDetails(actorUserId, wid);
      }

      const active =
        !plan.completed &&
        plan.endedAt == null &&
        plan.startedAt != null &&
        plan.isStarted;
      if (!active) {
        throw new BadRequestException(
          'Workout is not in an active session; start it first',
        );
      }

      await this.prisma.memberWorkoutPlan.update({
        where: { id: wid },
        data: { isStarted: false },
      });
      return this.getWorkoutDetails(actorUserId, wid);
    });
  }

  /** `POST /workouts/resume` — resume a paused session (`isStarted=true`, keeps `started_at`). */
  async resumeUnifiedWorkout(
    actorUserId: string,
    dto: WorkoutIdOnlyDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const wid = dto.workout_id.trim();
      const plan = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: wid },
        select: {
          id: true,
          userId: true,
          gymId: true,
          gymUserId: true,
          startedAt: true,
          endedAt: true,
          completed: true,
          isStarted: true,
        },
      });
      if (!plan) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, plan);
      this.assertOptionalGymIdMatchesWorkout(plan.gymId, gymIdFromQuery);

      const inProgress =
        !plan.completed &&
        plan.endedAt == null &&
        plan.startedAt != null &&
        plan.isStarted;
      if (inProgress) {
        return this.getWorkoutDetails(actorUserId, wid);
      }

      const paused =
        !plan.completed &&
        plan.endedAt == null &&
        plan.startedAt != null &&
        !plan.isStarted;
      if (!paused) {
        throw new BadRequestException(
          'Workout is not paused; start the session first',
        );
      }

      await this.prisma.memberWorkoutPlan.update({
        where: { id: wid },
        data: { isStarted: true },
      });
      return this.getWorkoutDetails(actorUserId, wid);
    });
  }

  /** `POST /workouts/save` — sets `is_saved` on the plan; body `{ workout_id }` only. */
  async saveUnifiedWorkout(
    actorUserId: string,
    dto: WorkoutIdOnlyDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const wid = dto.workout_id.trim();

      const existing = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: wid },
        select: {
          id: true,
          userId: true,
          gymId: true,
          gymUserId: true,
          title: true,
          notes: true,
        },
      });
      if (!existing) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, existing);
      this.assertOptionalGymIdMatchesWorkout(existing.gymId, gymIdFromQuery);

      await this.prisma.memberWorkoutPlan.update({
        where: { id: wid },
        data: { isSaved: true },
      });

      return this.getWorkoutDetails(actorUserId, wid);
    });
  }

  /** `GET /workouts/history` — paginated `member_workout_plans` summaries. */
  async listUnifiedWorkoutHistory(
    actorUserId: string,
    query: UnifiedWorkoutHistoryQueryDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const page = query.page ?? 0;
      const limit = query.limit ?? 20;
      const skip = page * limit;

      const trimmedGym = query.gymId?.trim();
      let where: Prisma.MemberWorkoutPlanWhereInput;

      if (!trimmedGym) {
        where = { userId: actorUserId };
      } else {
        const memberSelf = await this.prisma.gymUser.findFirst({
          where: {
            gymId: trimmedGym,
            userId: actorUserId,
            role: GymRole.MEMBER,
            isActive: true,
          },
          select: { id: true },
        });
        if (memberSelf) {
          where = { gymId: trimmedGym, gymUserId: memberSelf.id };
        } else {
          await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
          where = { userId: actorUserId, gymId: trimmedGym };
        }
      }

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
      const listWhere: Prisma.MemberWorkoutPlanWhereInput = {
        ...where,
        completed:
          query.completed !== undefined ? query.completed : true,
      };

      const [rows, total] = await this.prisma.$transaction([
        this.prisma.memberWorkoutPlan.findMany({
          where: listWhere,
          orderBy: { startedAt: 'desc' },
          skip,
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
        this.prisma.memberWorkoutPlan.count({ where: listWhere }),
      ]);
      return {
        userId: actorUserId,
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
            is_saved: w.isSaved,
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
    });
  }

  async listWorkouts(actorUserId: string, query: ListWorkoutsQueryDto) {
    return this.listTrainerWorkouts(actorUserId, query);
  }

  async listTrainerWorkouts(
    actorUserId: string,
    query: ListTrainerWorkoutsQueryDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const trimmedGym = query.gymId?.trim();

      if (trimmedGym) {
        await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
      }

      const exerciseInclude = {
        exercise: {
          select: {
            id: true,
            name: true,
            equipment: true,
            primaryMuscle: true,
            exerciseType: true,
            assetUrl: true,
          },
        },
        sets: {
          orderBy: { setNumber: 'asc' as const },
          select: {
            id: true,
            setNumber: true,
            reps: true,
            weight: true,
            completed: true
          },
        },
      } satisfies Prisma.WorkoutPlanExerciseInclude;

      const baseWhere: Prisma.MemberWorkoutPlanWhereInput = trimmedGym
        ? { userId: actorUserId, gymId: trimmedGym }
        : { userId: actorUserId, gymId: null };
      const createdByFilter = this.workoutCreatedByWhere(query.created_by);
      /** Library templates only — never started; session logs live in history / direct fetch by id. */
      const templateOnlyFilter: Prisma.MemberWorkoutPlanWhereInput = {
        startedAt: null,
        completed: false,
      };
      const where: Prisma.MemberWorkoutPlanWhereInput = {
        AND: [
          baseWhere,
          ...(createdByFilter == null ? [] : [createdByFilter]),
          templateOnlyFilter,
        ],
      };

      const rows = await this.prisma.memberWorkoutPlan.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        take: 200,
        include: {
          exercises: {
            include: exerciseInclude,
            orderBy: { createdAt: 'asc' },
          },
        },
      });

      return rows.map((w) => ({
        workout_id: w.id,
        member_id: w.gymUserId,
        title: w.title,
        date: w.createdAt,
        exercise_count: w.exercises.length,
        is_saved: w.isSaved,
        created_by: workoutCreatedByBucket(w.createdByRole),
        created_by_role: w.createdByRole,
        exercises: w.exercises,
        completed: w.completed,
      }));
    });
  }

  async updateTrainerWorkout(
    actorUserId: string,
    workoutId: string,
    dto: UpdateTrainerWorkoutDto,
    gymId?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const existing = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: workoutId },
        select: {
          userId: true,
          gymId: true,
          gymUserId: true,
          title: true,
          notes: true,
        },
      });
      if (!existing) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, existing);
      this.assertOptionalGymIdMatchesWorkout(existing.gymId, gymId);

      const hasUpdate =
        dto.title !== undefined ||
        dto.notes !== undefined ||
        dto.created_by !== undefined;
      if (!hasUpdate) {
        throw new BadRequestException('No fields to update');
      }

      const title = dto.title !== undefined ? dto.title.trim() : existing.title;
      const notes =
        dto.notes !== undefined ? (dto.notes?.trim() ?? null) : existing.notes;
      const data: Prisma.MemberWorkoutPlanUpdateInput = { title, notes };
      if (dto.created_by !== undefined) {
        data.createdByRole = await this.resolveWorkoutCreatedByRole(
          actorUserId,
          existing.gymId,
          dto.created_by,
        );
      }

      const updated = await this.prisma.memberWorkoutPlan.updateMany({
        where: existing.gymId
          ? { id: workoutId, gymId: existing.gymId }
          : { id: workoutId, gymId: null, userId: actorUserId },
        data,
      });
      if (updated.count === 0) {
        throw new NotFoundException('Workout not found');
      }

      return this.getWorkoutDetails(actorUserId, workoutId);
    });
  }

  async deleteTrainerWorkout(
    actorUserId: string,
    workoutId: string,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const existing = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: workoutId },
        select: { id: true, userId: true, gymId: true, gymUserId: true },
      });
      if (!existing) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, existing);
      this.assertOptionalGymIdMatchesWorkout(existing.gymId, gymIdFromQuery);

      const r = await this.prisma.memberWorkoutPlan.deleteMany({
        where: existing.gymId
          ? { id: workoutId, gymId: existing.gymId }
          : { id: workoutId, gymId: null, userId: actorUserId },
      });
      if (r.count === 0) {
        throw new NotFoundException('Workout not found');
      }
      return { success: true as const };
    });
  }

  /**
   * `PATCH /workouts` — `title` / `notes` only, or full exercise tree when `exercises` is sent.
   */
  async patchLegacyWorkout(
    actorUserId: string,
    dto: PatchWorkoutBodyDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const wid = dto.workout_id.trim();
      const plan = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: wid },
        select: {
          id: true,
          userId: true,
          gymId: true,
          gymUserId: true,
          title: true,
          notes: true,
        },
      });
      if (!plan) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, plan);
      this.assertOptionalGymIdMatchesWorkout(plan.gymId, gymIdFromQuery);

      const exercises = dto.exercises;
      if (exercises != null && exercises.length > 0) {
        const mid = dto.member_id?.trim();
        if (plan.gymId) {
          if (mid && mid !== plan.gymUserId) {
            throw new BadRequestException(
              'member_id does not match this workout',
            );
          }
        } else if (mid) {
          throw new BadRequestException(
            'member_id must not be set for personal workouts',
          );
        }

        const title = dto.title?.trim() ?? plan.title;
        const notes =
          dto.notes !== undefined ? (dto.notes?.trim() ?? null) : plan.notes;
        const planUpdate: Prisma.MemberWorkoutPlanUpdateInput = {
          title,
          notes,
        };
        if (dto.created_by !== undefined) {
          planUpdate.createdByRole = await this.resolveWorkoutCreatedByRole(
            actorUserId,
            plan.gymId,
            dto.created_by,
          );
        }

        await this.prisma.$transaction(async (tx) => {
          await tx.workoutPlanExercise.deleteMany({
            where: { workoutId: wid },
          });
          await tx.memberWorkoutPlan.update({
            where: { id: wid },
            data: planUpdate,
          });

          for (const ex of exercises) {
            const exercise = plan.gymId
              ? await tx.exercise.findFirst({
                where: { id: ex.exercise_id, userId: actorUserId },
                select: { id: true },
              })
              : await tx.exercise.findFirst({
                where: {
                  id: ex.exercise_id,
                  userId: actorUserId,
                  gymId: null,
                },
                select: { id: true },
              });
            if (!exercise) {
              throw new BadRequestException(
                plan.gymId
                  ? `Invalid exercise_id: ${ex.exercise_id} (use exercises from this gym's catalog)`
                  : `Invalid exercise_id: ${ex.exercise_id} (use exercises from your personal catalog)`,
              );
            }

            const weId = randomUUID();
            await tx.$executeRaw(Prisma.sql`
              INSERT INTO workout_plan_exercises
                (id, workout_id, exercise_id)
              VALUES (${weId}, ${wid}, ${ex.exercise_id})
            `);

            for (const set of ex.sets) {
              await tx.$executeRaw(Prisma.sql`
                INSERT INTO workout_exercise_sets
                  (id, workout_exercise_id, set_number, reps, weight, completed, updated_at)
                VALUES
                  (${randomUUID()}, ${weId}, ${set.set_number},
                   ${set.reps ?? 0}, ${set.weight ?? 0}, false, CURRENT_TIMESTAMP)
              `);
            }
          }
        });

        const { totalVolume, totalSets } =
          await this.aggregateMemberWorkoutTotalsAllSets(wid);
        await this.prisma.memberWorkoutPlan.update({
          where: { id: wid },
          data: { totalVolume, totalSets },
        });

        return this.getWorkoutDetails(actorUserId, wid);
      }

      const patch: UpdateTrainerWorkoutDto = {
        title: dto.title,
        notes: dto.notes,
        created_by: dto.created_by,
        completed: dto.completed,
        isSaved: dto.isSaved,
      };
      return this.updateTrainerWorkout(actorUserId, wid, patch, gymIdFromQuery);
    });
  }

  private async persistGymWorkout(
    actorUserId: string,
    member: { id: string; gymId: string; userId: string },
    dto: CreateWorkoutDto,
  ): Promise<string> {
    const workoutId = randomUUID();
    const createdByRole = await this.resolveWorkoutCreatedByRole(
      actorUserId,
      member.gymId,
      dto.created_by,
    );

    await this.prisma.$transaction(async (tx) => {
      await tx.memberWorkoutPlan.create({
        data: {
          id: workoutId,
          userId: member.userId,
          gymId: member.gymId,
          gymUserId: member.id,
          title: dto.title.trim(),
          notes: dto.notes?.trim() ?? null,
          completed: false,
          createdByRole,
          createdByUserId: actorUserId,
        },
      });

      for (const ex of dto.exercises) {
        const exercise = await tx.exercise.findFirst({
          where: { id: ex.exercise_id, userId: actorUserId },
          select: { id: true },
        });
        if (!exercise) {
          throw new BadRequestException(
            `Invalid exercise_id: ${ex.exercise_id} (use exercises from this gym's catalog)`,
          );
        }

        const weId = randomUUID();
        await tx.$executeRaw(Prisma.sql`
          INSERT INTO workout_plan_exercises
            (id, workout_id, exercise_id)
          VALUES (${weId}, ${workoutId}, ${ex.exercise_id})
        `);

        for (const set of ex.sets) {
          await tx.$executeRaw(Prisma.sql`
            INSERT INTO workout_exercise_sets
              (id, workout_exercise_id, set_number, reps, weight, completed, updated_at)
            VALUES
              (${randomUUID()}, ${weId}, ${set.set_number},
               ${set.reps ?? 0}, ${set.weight ?? 0}, false, CURRENT_TIMESTAMP)
          `);
        }
      }
    });

    return workoutId;
  }

  /**
   * Copy a finished plan into a new in-progress session so history rows are preserved.
   * Exercises/sets are duplicated with `completed=false`; reps/weight targets are kept.
   */
  private async cloneMemberWorkoutPlanForNewSession(
    sourceWorkoutId: string,
    startedAt: Date,
  ): Promise<string> {
    const source = await this.prisma.memberWorkoutPlan.findFirst({
      where: { id: sourceWorkoutId },
      include: {
        exercises: {
          orderBy: { createdAt: 'asc' },
          include: {
            sets: { orderBy: { setNumber: 'asc' } },
          },
        },
      },
    });
    if (!source) {
      throw new NotFoundException('Workout not found');
    }

    const newWorkoutId = randomUUID();

    await this.prisma.$transaction(async (tx) => {
      await tx.memberWorkoutPlan.create({
        data: {
          id: newWorkoutId,
          userId: source.userId,
          gymId: source.gymId,
          gymUserId: source.gymUserId,
          title: source.title,
          notes: source.notes,
          createdByRole: source.createdByRole,
          startedAt,
          endedAt: null,
          completed: false,
          isStarted: true,
          isSaved: source.isSaved,
          totalVolume: 0,
          totalSets: 0,
        },
      });

      for (const we of source.exercises) {
        const weId = randomUUID();
        await tx.$executeRaw(Prisma.sql`
          INSERT INTO workout_plan_exercises (id, workout_id, exercise_id)
          VALUES (${weId}, ${newWorkoutId}, ${we.exerciseId})
        `);
        for (const set of we.sets) {
          await tx.$executeRaw(Prisma.sql`
            INSERT INTO workout_exercise_sets
              (id, workout_exercise_id, set_number, reps, weight, completed, updated_at)
            VALUES
              (${randomUUID()}, ${weId}, ${set.setNumber},
               ${set.reps ?? 0}, ${set.weight ?? 0}, false, CURRENT_TIMESTAMP)
          `);
        }
      }
    });

    return newWorkoutId;
  }

  private async persistPersonalWorkout(
    actorUserId: string,
    dto: CreateWorkoutDto,
  ): Promise<string> {
    const workoutId = randomUUID();
    const createdByRole = await this.resolveWorkoutCreatedByRole(
      actorUserId,
      null,
      dto.created_by,
    );

    await this.prisma.$transaction(async (tx) => {
      await tx.memberWorkoutPlan.create({
        data: {
          id: workoutId,
          userId: actorUserId,
          gymId: null,
          gymUserId: null,
          title: dto.title.trim(),
          notes: dto.notes?.trim() ?? null,
          completed: false,
          createdByRole,
          createdByUserId: actorUserId,
        },
      });

      for (const ex of dto.exercises) {
        const exercise = await tx.exercise.findFirst({
          where: {
            id: ex.exercise_id,
            userId: actorUserId,
            gymId: null,
          },
          select: { id: true },
        });
        if (!exercise) {
          throw new BadRequestException(
            `Invalid exercise_id: ${ex.exercise_id} (use exercises from your personal catalog)`,
          );
        }

        const weId = randomUUID();
        await tx.$executeRaw(Prisma.sql`
          INSERT INTO workout_plan_exercises
            (id, workout_id, exercise_id)
          VALUES (${weId}, ${workoutId}, ${ex.exercise_id})
        `);

        for (const set of ex.sets) {
          await tx.$executeRaw(Prisma.sql`
            INSERT INTO workout_exercise_sets
              (id, workout_exercise_id, set_number, reps, weight, completed, updated_at)
            VALUES
              (${randomUUID()}, ${weId}, ${set.set_number},
               ${set.reps ?? 0}, ${set.weight ?? 0}, false, CURRENT_TIMESTAMP)
          `);
        }
      }
    });

    return workoutId;
  }

  async listMemberWorkouts(
    actorUserId: string,
    gymUserId: string,
    createdBy = 'trainer',
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const member = await this.requireMemberAndAccess(actorUserId, gymUserId);
      const roleClause = this.workoutCreatedBySqlClause(createdBy);
      const rows = await this.prisma.$queryRaw<
        Array<{
          workout_id: string;
          title: string;
          date: Date;
          exercise_count: number;
          created_by_role: string;
          created_by_name: string | null;
        }>
      >(Prisma.sql`
        SELECT
          w.id AS workout_id,
          w.title,
          w.created_at AS date,
          COUNT(we.id)::int AS exercise_count,
          w."createdByRole" AS created_by_role,
          COALESCE(
            NULLIF(TRIM(creator."fullName"), ''),
            CASE w."createdByRole"
              WHEN ${GymRole.OWNER} THEN NULLIF(TRIM(owner_user."fullName"), '')
              ELSE NULL
            END
          ) AS created_by_name
        FROM member_workout_plans w
        LEFT JOIN workout_plan_exercises we ON we.workout_id = w.id
        LEFT JOIN "User" creator ON creator.id = w.created_by_user_id
        LEFT JOIN "Gym" g ON g.id = w.gym_id
        LEFT JOIN "User" owner_user ON owner_user.id = g."ownerId"
        WHERE w.gym_id = ${member.gymId}
          AND w.gym_user_id = ${gymUserId}
          AND w.started_at IS NULL
          AND w.completed = false
          ${roleClause}
        GROUP BY w.id, w."createdByRole", w.title, w.created_at, creator."fullName", owner_user."fullName"
        ORDER BY w.created_at DESC
        LIMIT 200
      `);
      return rows.map((r) => ({
        workout_id: r.workout_id,
        title: r.title,
        date: r.date,
        exercise_count: r.exercise_count,
        created_by: workoutCreatedByBucket(r.created_by_role),
        created_by_role: r.created_by_role,
        created_by_name: workoutCreatorDisplayName(
          r.created_by_name,
          r.created_by_role,
        ),
      }));
    });
  }

  async getWorkoutDetails(actorUserId: string, workoutId: string) {
    return this.withWorkoutStorageHandling(async () => {
      const summary = await this.loadMemberWorkoutCompletionContext(
        actorUserId,
        workoutId,
      );
      return this.buildMemberWorkoutDetailResponse(summary);
    });
  }

  /** Completed workout summary for the post-workout / edit screen. */
  async getWorkoutCompletionSummary(
    actorUserId: string,
    workoutId: string,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const summary = await this.loadMemberWorkoutCompletionContext(
        actorUserId,
        workoutId,
        gymIdFromQuery,
      );
      if (!summary.plan.completed) {
        throw new BadRequestException(
          'Workout is not completed yet; stop or complete the session first',
        );
      }
      return this.buildMemberWorkoutCompletionSummary(summary);
    });
  }

  async updateWorkoutCompletionSummary(
    actorUserId: string,
    workoutId: string,
    dto: UpdateWorkoutCompletionDto,
    gymIdFromQuery?: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const summary = await this.loadMemberWorkoutCompletionContext(
        actorUserId,
        workoutId,
        gymIdFromQuery,
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

      await this.prisma.memberWorkoutPlan.update({
        where: { id: workoutId },
        data: { startedAt, endedAt },
      });

      return this.getWorkoutCompletionSummary(
        actorUserId,
        workoutId,
        gymIdFromQuery,
      );
    });
  }

  private async loadMemberWorkoutCompletionContext(
    actorUserId: string,
    workoutId: string,
    gymIdFromQuery?: string,
  ): Promise<{
    plan: {
      id: string;
      title: string;
      startedAt: Date | null;
      endedAt: Date | null;
      isSaved: boolean;
      totalVolume: number;
      totalSets: number;
      completed: boolean;
      createdByRole: string;
      isStarted: boolean;
    };
    exercises: Array<{
      exercise_id: string;
      name: string;
      assetUrl: string | null;
      exerciseType: string | null;
      sets: Array<Record<string, unknown>>;
    }>;
  }> {
    const plan = await this.prisma.memberWorkoutPlan.findFirst({
      where: { id: workoutId },
      select: {
        id: true,
        userId: true,
        gymId: true,
        gymUserId: true,
        title: true,
        startedAt: true,
        endedAt: true,
        isSaved: true,
        totalVolume: true,
        totalSets: true,
        completed: true,
        createdByRole: true,
        isStarted: true,
      },
    });

    if (!plan) {
      throw new NotFoundException('Workout not found');
    }
    await this.assertMemberWorkoutRowAccess(actorUserId, {
      userId: plan.userId,
      gymId: plan.gymId,
      gymUserId: plan.gymUserId,
    });
    this.assertOptionalGymIdMatchesWorkout(plan.gymId, gymIdFromQuery);

    const exercises = await this.loadMemberWorkoutExerciseGroups(workoutId);
    return {
      plan: {
        id: plan.id,
        title: plan.title,
        startedAt: plan.startedAt,
        endedAt: plan.endedAt,
        isSaved: plan.isSaved,
        totalVolume: plan.totalVolume ?? 0,
        totalSets: plan.totalSets ?? 0,
        completed: plan.completed,
        createdByRole: plan.createdByRole,
        isStarted: plan.isStarted,
      },
      exercises,
    };
  }

  /**
   * When `gymId` is sent (query or `X-Gym-Id`), it must match the plan's gym.
   * Omitted `gymId` is allowed after `assertMemberWorkoutRowAccess`.
   */
  private assertOptionalGymIdMatchesWorkout(
    planGymId: string | null,
    gymIdFromQuery: string | undefined,
  ): void {
    const gq = gymIdFromQuery?.trim();
    if (!gq) {
      return;
    }
    if (planGymId == null) {
      throw new BadRequestException(
        'gymId must not be set for personal workouts',
      );
    }
    if (planGymId !== gq) {
      throw new BadRequestException('gymId does not match this workout');
    }
  }

  private async loadMemberWorkoutExerciseGroups(workoutId: string): Promise<
    Array<{
      exercise_id: string;
      name: string;
      assetUrl: string | null;
      exerciseType: string | null;
      sets: Array<Record<string, unknown>>;
    }>
  > {
    const exercises = await this.prisma.$queryRaw<
      Array<{
        workout_exercise_id: string;
        exercise_id: string;
        name: string;
        set_id: string | null;
        set_number: number | null;
        reps: number | null;
        weight: number | null;
        completed: boolean | null;
        assetUrl: string | null;
        exerciseType: string | null;
      }>
    >(Prisma.sql`
      SELECT
        we.id AS workout_exercise_id,
        e.id AS exercise_id,
        e.name,
        s.id AS set_id,
        s.set_number,
        s.reps,
        s.weight,
        s.completed,
        e."assetUrl" AS "assetUrl",
        e."exerciseType" AS "exerciseType"
      FROM workout_plan_exercises we
      JOIN exercises e
        ON e.id = we.exercise_id
      LEFT JOIN workout_exercise_sets s ON s.workout_exercise_id = we.id
      WHERE we.workout_id = ${workoutId}
      ORDER BY e.name ASC, s.set_number ASC
    `);

    const grouped = new Map<
      string,
      {
        exercise_id: string;
        name: string;
        assetUrl: string | null;
        exerciseType: string | null;
        sets: Array<Record<string, unknown>>;
      }
    >();
    for (const r of exercises) {
      if (!grouped.has(r.workout_exercise_id)) {
        grouped.set(r.workout_exercise_id, {
          exercise_id: r.exercise_id,
          name: r.name,
          assetUrl: r.assetUrl,
          exerciseType: r.exerciseType,
          sets: [],
        });
      }
      if (r.set_id) {
        grouped.get(r.workout_exercise_id)!.sets.push({
          id: r.set_id,
          set_number: r.set_number,
          reps: r.reps,
          weight: r.weight,
          completed: r.completed,
        });
      }
    }
    return [...grouped.values()];
  }

  private buildMemberWorkoutDetailResponse(summary: {
    plan: {
      id: string;
      title: string;
      startedAt: Date | null;
      endedAt: Date | null;
      isSaved: boolean;
      totalVolume: number;
      totalSets: number;
      createdByRole: string;
      isStarted: boolean;
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
      created_by: workoutCreatedByBucket(summary.plan.createdByRole),
      created_by_role: summary.plan.createdByRole,
      duration: formatWorkoutDurationLabel(durationMin),
      volume: `${summary.plan.totalVolume ?? 0} kg`,
      sets: summary.plan.totalSets ?? 0,
      is_saved: summary.plan.isSaved,
      is_started: summary.plan.isStarted,
      exercises: summary.exercises,
    };
  }

  private buildMemberWorkoutCompletionSummary(summary: {
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

  async addExerciseToWorkout(
    actorUserId: string,
    workoutId: string,
    dto: AddExerciseToWorkoutDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const plan = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: workoutId },
        select: { userId: true, gymId: true, gymUserId: true },
      });
      if (!plan) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertMemberWorkoutRowAccess(actorUserId, plan);

      const e = plan.gymId
        ? await this.prisma.exercise.findFirst({
          where: { id: dto.exercise_id, gymId: plan.gymId },
          select: { id: true },
        })
        : await this.prisma.exercise.findFirst({
          where: {
            id: dto.exercise_id,
            userId: plan.userId,
            gymId: null,
          },
          select: { id: true },
        });
      if (!e) {
        throw new NotFoundException('Exercise not found');
      }
      await this.prisma.$executeRaw(Prisma.sql`
        INSERT INTO workout_plan_exercises (id, workout_id, exercise_id)
        VALUES (${randomUUID()}, ${workoutId}, ${dto.exercise_id})
      `);
      return this.getWorkoutDetails(actorUserId, workoutId);
    });
  }

  async updateSet(actorUserId: string, setId: string, dto: UpdateSetDto) {
    return this.withWorkoutStorageHandling(async () => {
      const w = await this.prisma.$queryRaw<Array<{ workout_id: string }>>(
        Prisma.sql`
        SELECT we.workout_id
        FROM workout_exercise_sets s
        JOIN workout_plan_exercises we ON we.id = s.workout_exercise_id
        WHERE s.id = ${setId}
        LIMIT 1
      `,
      );
      const workoutId = w[0]?.workout_id;
      if (!workoutId) {
        throw new NotFoundException('Set not found');
      }
      await this.assertWorkoutAccess(actorUserId, workoutId);
      await this.prisma.$executeRaw(Prisma.sql`
        UPDATE workout_exercise_sets
        SET
          reps = COALESCE(${dto.reps ?? null}, reps),
          weight = COALESCE(${dto.weight ?? null}, weight),
          completed = COALESCE(${dto.completed ?? null}, completed),
          updated_at = CURRENT_TIMESTAMP
        WHERE id = ${setId}
      `);
      return { success: true as const };
    });
  }

  async addSet(actorUserId: string, dto: AddSetDto) {
    return this.withWorkoutStorageHandling(async () => {
      const w = await this.prisma.$queryRaw<Array<{ workout_id: string }>>(
        Prisma.sql`
        SELECT workout_id FROM workout_plan_exercises WHERE id = ${dto.workout_exercise_id} LIMIT 1
      `,
      );
      const workoutId = w[0]?.workout_id;
      if (!workoutId) {
        throw new NotFoundException('Workout exercise not found');
      }
      await this.assertWorkoutAccess(actorUserId, workoutId);
      await this.prisma.$executeRaw(Prisma.sql`
        INSERT INTO workout_exercise_sets
          (id, workout_exercise_id, set_number, reps, weight, completed, updated_at)
        VALUES
          (${randomUUID()}, ${dto.workout_exercise_id}, ${dto.set_number},
           ${dto.reps ?? 0}, ${dto.weight ?? 0}, false, CURRENT_TIMESTAMP)
      `);
      return { success: true as const };
    });
  }

  async deleteExerciseFromWorkout(
    actorUserId: string,
    workoutId: string,
    exerciseId: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      await this.assertWorkoutAccess(actorUserId, workoutId);
      await this.prisma.$executeRaw(Prisma.sql`
        DELETE FROM workout_plan_exercises
        WHERE workout_id = ${workoutId} AND exercise_id = ${exerciseId}
      `);
      return { success: true as const };
    });
  }

  async completeWorkout(actorUserId: string, workoutId: string) {
    return this.withWorkoutStorageHandling(async () => {
      await this.assertWorkoutAccess(actorUserId, workoutId);
      const existing = await this.prisma.memberWorkoutPlan.findFirst({
        where: { id: workoutId },
        select: { completed: true },
      });
      if (!existing) {
        throw new NotFoundException('Workout not found');
      }
      if (existing.completed) {
        return this.getWorkoutCompletionSummary(actorUserId, workoutId);
      }
      const agg = await this.prisma.$queryRaw<
        Array<{ total_volume: number; total_sets: number }>
      >(Prisma.sql`
        SELECT
          COALESCE(SUM(weight * reps), 0)::int AS total_volume,
          COUNT(*)::int AS total_sets
        FROM workout_exercise_sets s
        JOIN workout_plan_exercises we ON we.id = s.workout_exercise_id
        WHERE we.workout_id = ${workoutId} AND s.completed = true
      `);
      const volume = agg[0]?.total_volume ?? 0;
      const sets = agg[0]?.total_sets ?? 0;
      await this.prisma.$executeRaw(Prisma.sql`
        UPDATE member_workout_plans
        SET
          completed = true,
          started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
          ended_at = CURRENT_TIMESTAMP,
          total_volume = ${volume},
          total_sets = ${sets},
          updated_at = CURRENT_TIMESTAMP
        WHERE id = ${workoutId}
      `);
      return this.getWorkoutCompletionSummary(actorUserId, workoutId);
    });
  }

  private async aggregateMemberWorkoutTotalsAllSets(
    workoutId: string,
  ): Promise<{ totalVolume: number; totalSets: number }> {
    const agg = await this.prisma.$queryRaw<
      Array<{ total_volume: number; total_sets: number }>
    >(Prisma.sql`
      SELECT
        COALESCE(SUM(s.weight * s.reps), 0)::int AS total_volume,
        COUNT(s.id)::int AS total_sets
      FROM workout_exercise_sets s
      JOIN workout_plan_exercises we ON we.id = s.workout_exercise_id
      WHERE we.workout_id = ${workoutId}
    `);
    return {
      totalVolume: agg[0]?.total_volume ?? 0,
      totalSets: agg[0]?.total_sets ?? 0,
    };
  }

  private assertCatalogExerciseOwner(
    row: { userId: string; gymId?: string | null },
    actorUserId: string,
  ): void {
    if (row.userId !== actorUserId) {
      throw new ForbiddenException(
        'You can only access your own exercise catalog entries',
      );
    }
  }

  private exerciseToApi(r: {
    id: string;
    name: string;
    assetUrl: string | null;
    equipment: ExerciseEquipment;
    primaryMuscle: Muscle;
    exerciseType: ExerciseType;
    isActive: boolean;
    createdAt: Date;
    secondaryMuscles: { muscle: Muscle }[];
  }) {
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

  private workoutPerformedAt(w: {
    startedAt: Date | null;
    endedAt: Date | null;
    createdAt: Date;
  }): Date {
    return w.endedAt ?? w.startedAt ?? w.createdAt;
  }

  private async buildExerciseDetailPlanFilter(
    actorUserId: string,
    exerciseGymId: string | null,
  ): Promise<Prisma.MemberWorkoutPlanWhereInput | null> {
    if (exerciseGymId == null) {
      return { userId: actorUserId, gymId: null };
    }
    const member = await this.prisma.gymUser.findFirst({
      where: {
        gymId: exerciseGymId,
        userId: actorUserId,
        isActive: true,
        role: GymRole.MEMBER,
      },
      select: { id: true },
    });
    if (!member) {
      return null;
    }
    return { gymId: exerciseGymId, gymUserId: member.id };
  }

  private flattenExerciseDetailSets(
    blocks: Array<{
      workout: {
        id: string;
        title: string;
        startedAt: Date | null;
        endedAt: Date | null;
        createdAt: Date;
      };
      sets: Array<{
        setNumber: number;
        weight: number;
        reps: number;
        completed: boolean;
      }>;
    }>,
  ): Array<{
    weight: number;
    reps: number;
    completed: boolean;
    setNumber: number;
    performedAt: Date;
    workoutId: string;
  }> {
    const out: Array<{
      weight: number;
      reps: number;
      completed: boolean;
      setNumber: number;
      performedAt: Date;
      workoutId: string;
    }> = [];
    for (const b of blocks) {
      const performedAt = this.workoutPerformedAt(b.workout);
      for (const s of b.sets) {
        out.push({
          weight: s.weight,
          reps: s.reps,
          completed: s.completed,
          setNumber: s.setNumber,
          performedAt,
          workoutId: b.workout.id,
        });
      }
    }
    return out;
  }

  private emptyExerciseSummary(
    chartRange: ExerciseDetailChartRange,
    chartMetric: ExerciseDetailChartMetric,
  ) {
    return {
      last_performance: null,
      chart_range: chartRange,
      chart_metric: chartMetric,
      unit: 'kg' as const,
      chart: this.exerciseDetailMonthBuckets(chartRange).map((m) => ({
        period_start: m.periodStartIso,
        label: m.label,
        value: null as number | null,
      })),
      personal_records: {
        heaviest_weight_kg: null,
        best_one_rep_max_kg: null,
        best_set_volume_kg: null,
        best_set_volume_label: null,
        best_session_volume_kg: null,
      },
    };
  }

  private exerciseDetailMonthBuckets(range: ExerciseDetailChartRange): Array<{
    year: number;
    monthIndex0: number;
    label: string;
    periodStartIso: string;
  }> {
    const n =
      range === '3m' ? 3 : range === '6m' ? 6 : range === '12m' ? 12 : 3;
    const now = new Date();
    const y = now.getUTCFullYear();
    const m = now.getUTCMonth();
    const buckets: Array<{
      year: number;
      monthIndex0: number;
      label: string;
      periodStartIso: string;
    }> = [];
    for (let i = n - 1; i >= 0; i--) {
      const d = new Date(Date.UTC(y, m - i, 1));
      const year = d.getUTCFullYear();
      const monthIndex0 = d.getUTCMonth();
      const label = d.toLocaleString('en-US', {
        month: 'short',
        timeZone: 'UTC',
      });
      const periodStartIso = `${year}-${String(monthIndex0 + 1).padStart(2, '0')}-01`;
      buckets.push({ year, monthIndex0, label, periodStartIso });
    }
    return buckets;
  }

  private epleyOneRmKg(weight: number, reps: number): number | null {
    if (weight <= 0 || reps <= 0) {
      return null;
    }
    return weight * (1 + reps / 30);
  }

  private setMetricValue(
    s: { weight: number; reps: number },
    metric: ExerciseDetailChartMetric,
  ): number | null {
    if (metric === 'heaviest_weight') {
      return s.weight > 0 ? s.weight : null;
    }
    if (metric === 'one_rep_max') {
      return this.epleyOneRmKg(s.weight, s.reps);
    }
    if (metric === 'best_set_volume') {
      return s.weight > 0 && s.reps > 0 ? s.weight * s.reps : null;
    }
    return null;
  }

  private buildExerciseSummaryStats(
    flat: Array<{
      weight: number;
      reps: number;
      completed: boolean;
      setNumber: number;
      performedAt: Date;
      workoutId: string;
    }>,
    chartRange: ExerciseDetailChartRange,
    chartMetric: ExerciseDetailChartMetric,
  ) {
    const qualifying = flat.filter((s) => s.weight > 0 || s.reps > 0);

    let heaviestWeightKg: number | null = null;
    let bestOneRm: number | null = null;
    let bestSetVolKg: number | null = null;
    let bestSetVolLabel: string | null = null;
    const volByWorkout = new Map<string, number>();

    for (const s of qualifying) {
      if (s.weight > 0) {
        heaviestWeightKg =
          heaviestWeightKg == null
            ? s.weight
            : Math.max(heaviestWeightKg, s.weight);
      }
      const orm = this.epleyOneRmKg(s.weight, s.reps);
      if (orm != null) {
        bestOneRm = bestOneRm == null ? orm : Math.max(bestOneRm, orm);
      }
      if (s.weight > 0 && s.reps > 0) {
        const v = s.weight * s.reps;
        if (bestSetVolKg == null || v > bestSetVolKg) {
          bestSetVolKg = v;
          bestSetVolLabel = `${s.weight}kg x ${s.reps}`;
        }
      }
      if (s.weight > 0 && s.reps > 0) {
        volByWorkout.set(
          s.workoutId,
          (volByWorkout.get(s.workoutId) ?? 0) + s.weight * s.reps,
        );
      }
    }

    let bestSessionVolumeKg: number | null = null;
    for (const v of volByWorkout.values()) {
      bestSessionVolumeKg =
        bestSessionVolumeKg == null ? v : Math.max(bestSessionVolumeKg, v);
    }

    const buckets = this.exerciseDetailMonthBuckets(chartRange);
    const chart = buckets.map((b) => {
      let best: number | null = null;
      for (const s of qualifying) {
        if (
          s.performedAt.getUTCFullYear() !== b.year ||
          s.performedAt.getUTCMonth() !== b.monthIndex0
        ) {
          continue;
        }
        const val = this.setMetricValue(s, chartMetric);
        if (val == null) {
          continue;
        }
        best = best == null ? val : Math.max(best, val);
      }
      return {
        period_start: b.periodStartIso,
        label: b.label,
        value: best,
      };
    });

    const lastPerf = this.buildLastPerformance(qualifying, chartMetric);

    return {
      last_performance: lastPerf,
      chart_range: chartRange,
      chart_metric: chartMetric,
      unit: 'kg' as const,
      chart,
      personal_records: {
        heaviest_weight_kg: heaviestWeightKg,
        best_one_rep_max_kg:
          bestOneRm == null ? null : Math.round(bestOneRm * 100) / 100,
        best_set_volume_kg: bestSetVolKg,
        best_set_volume_label: bestSetVolLabel,
        best_session_volume_kg: bestSessionVolumeKg,
      },
    };
  }

  private buildLastPerformance(
    qualifying: Array<{
      weight: number;
      reps: number;
      performedAt: Date;
      workoutId: string;
    }>,
    chartMetric: ExerciseDetailChartMetric,
  ): Record<string, unknown> | null {
    if (qualifying.length === 0) {
      return null;
    }
    const byWorkout = new Map<
      string,
      { at: number; sets: typeof qualifying }
    >();
    for (const s of qualifying) {
      const t = s.performedAt.getTime();
      const cur = byWorkout.get(s.workoutId);
      if (!cur) {
        byWorkout.set(s.workoutId, { at: t, sets: [s] });
      } else {
        cur.sets.push(s);
        cur.at = Math.max(cur.at, t);
      }
    }
    let latestWid: string | null = null;
    let latestT = -1;
    for (const [wid, { at }] of byWorkout) {
      if (at > latestT) {
        latestT = at;
        latestWid = wid;
      }
    }
    if (!latestWid) {
      return null;
    }
    const sessionSets = byWorkout.get(latestWid)!.sets;
    const day = new Date(latestT);
    const dateStr = `${day.getUTCFullYear()}-${String(day.getUTCMonth() + 1).padStart(2, '0')}-${String(day.getUTCDate()).padStart(2, '0')}`;

    if (chartMetric === 'heaviest_weight') {
      let maxW = 0;
      for (const s of sessionSets) {
        if (s.weight > maxW) {
          maxW = s.weight;
        }
      }
      if (maxW <= 0) {
        return null;
      }
      return {
        date: dateStr,
        metric: chartMetric,
        heaviest_weight_kg: maxW,
      };
    }

    if (chartMetric === 'one_rep_max') {
      let best: number | null = null;
      for (const s of sessionSets) {
        const o = this.epleyOneRmKg(s.weight, s.reps);
        if (o != null) {
          best = best == null ? o : Math.max(best, o);
        }
      }
      if (best == null) {
        return null;
      }
      return {
        date: dateStr,
        metric: chartMetric,
        estimated_one_rep_max_kg: Math.round(best * 100) / 100,
      };
    }

    let bestVol = 0;
    let label: string | null = null;
    for (const s of sessionSets) {
      if (s.weight > 0 && s.reps > 0) {
        const v = s.weight * s.reps;
        if (v > bestVol) {
          bestVol = v;
          label = `${s.weight}kg x ${s.reps}`;
        }
      }
    }
    if (bestVol <= 0 || !label) {
      return null;
    }
    return {
      date: dateStr,
      metric: chartMetric,
      best_set_volume_kg: bestVol,
      best_set_volume_label: label,
    };
  }

  /** Read/update access: personal workout (owner), gym workout (member subject or gym managers). */
  private async assertMemberWorkoutRowAccess(
    actorUserId: string,
    row: {
      userId: string;
      gymId: string | null;
      gymUserId: string | null;
    },
  ): Promise<void> {
    if (!row.gymId) {
      if (row.userId !== actorUserId) {
        throw new ForbiddenException('No access to this workout');
      }
      return;
    }
    const memberSelf =
      row.gymUserId != null
        ? await this.prisma.gymUser.findFirst({
          where: { id: row.gymUserId, userId: actorUserId },
          select: { id: true },
        })
        : null;
    if (memberSelf) {
      return;
    }
    await this.gymAccess.assertCanManageGym(actorUserId, row.gymId);
  }

  private async requireMemberAndAccess(
    actorUserId: string,
    gymUserId?: string,
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: gymUserId },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);
    return member;
  }

  private async assertWorkoutAccess(actorUserId: string, workoutId: string) {
    const plan = await this.prisma.memberWorkoutPlan.findFirst({
      where: { id: workoutId },
      select: { userId: true, gymId: true, gymUserId: true },
    });
    if (!plan) {
      throw new NotFoundException('Workout not found');
    }
    await this.assertMemberWorkoutRowAccess(actorUserId, plan);
  }

  private async withWorkoutStorageHandling<T>(
    operation: () => Promise<T>,
  ): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      this.rethrowKnownWorkoutStorageError(error);
      throw error;
    }
  }

  private rethrowKnownWorkoutStorageError(error: unknown): never | void {
    if (!(error instanceof Prisma.PrismaClientKnownRequestError)) {
      return;
    }

    const meta = error.meta as
      | { code?: unknown; message?: unknown }
      | undefined;
    const sqlState = typeof meta?.code === 'string' ? meta.code : '';
    const details =
      typeof meta?.message === 'string' ? meta.message : error.message;

    if (
      sqlState === '42P01' ||
      /relation\s+".*"\s+does not exist/i.test(details)
    ) {
      throw new ServiceUnavailableException(
        'Workout storage is not initialized. Please run database migrations and try again.',
      );
    }
  }

  private async resolveWorkoutCreatedByRole(
    actorUserId: string,
    gymId: string | null,
    createdBy?: string,
  ): Promise<GymRole> {
    const resolved = await this.gymAccess.resolveActorGymRole(
      actorUserId,
      gymId,
    );
    const bucket = createdBy?.trim().toLowerCase();
    if (!bucket) {
      return resolved;
    }
    if (bucket === 'member') {
      return GymRole.MEMBER;
    }
    if (bucket === 'trainer') {
      if (
        resolved === GymRole.OWNER ||
        resolved === GymRole.TRAINER ||
        resolved === GymRole.STAFF
      ) {
        return resolved;
      }
      return GymRole.TRAINER;
    }
    return resolved;
  }

  private workoutCreatedByWhere(
    createdBy?: string,
  ): Prisma.MemberWorkoutPlanWhereInput | null {
    const v = createdBy?.trim().toLowerCase();
    if (!v || v === 'all') {
      return null;
    }
    if (v === 'member') {
      return { createdByRole: GymRole.MEMBER };
    }
    if (v === 'trainer') {
      return {
        createdByRole: {
          in: [GymRole.OWNER, GymRole.TRAINER, GymRole.STAFF],
        },
      };
    }
    return null;
  }

  private workoutCreatedBySqlClause(createdBy?: string): Prisma.Sql {
    const v = createdBy?.trim().toLowerCase();
    if (!v || v === 'all') {
      return Prisma.empty;
    }
    if (v === 'member') {
      return Prisma.sql`AND w."createdByRole" = ${GymRole.MEMBER}`;
    }
    if (v === 'trainer') {
      return Prisma.sql`AND w."createdByRole" IN (${GymRole.OWNER}, ${GymRole.TRAINER}, ${GymRole.STAFF})`;
    }
    return Prisma.empty;
  }
}
