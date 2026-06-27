import {
  BadRequestException,
  ConflictException,
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
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type { AddExerciseToWorkoutDto } from './dto/add-exercise-to-workout.dto';
import type { AddSetDto } from './dto/add-set.dto';
import type { CreateTrainerExerciseDto } from './dto/create-trainer-exercise.dto';
import type { CreateWorkoutDto } from './dto/create-workout.dto';
import type { ListTrainerExercisesQueryDto } from './dto/list-trainer-exercises-query.dto';
import type { ListTrainerWorkoutsQueryDto } from './dto/list-trainer-workouts-query.dto';
import type { UpdateTrainerWorkoutDto } from './dto/update-trainer-workout.dto';
import type { UpdateTrainerExerciseDto } from './dto/update-trainer-exercise.dto';
import type { UpdateSetDto } from './dto/update-set.dto';

@Injectable()
export class WorkoutsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async createExercise(
    actorUserId: string,
    gymId: string,
    dto: CreateTrainerExerciseDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const secondary = [
      ...new Set(dto.secondary_muscles ?? []),
    ].filter((m) => m !== dto.primary_muscle);

    const created = await this.prisma.exercise.create({
      data: {
        gymId,
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
    await this.gymAccess.assertCanManageGym(actorUserId, query.gymId);
    const search = query.search?.trim();
    const where: Prisma.ExerciseWhereInput = {
      gymId: query.gymId,
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

    const rows = await this.prisma.exercise.findMany({
      where,
      orderBy: { name: 'asc' },
      take: 500,
      include: { secondaryMuscles: { select: { muscle: true } } },
    });

    return rows.map((r) => this.exerciseToApi(r));
  }

  async updateExercise(
    actorUserId: string,
    gymId: string,
    exerciseId: string,
    dto: UpdateTrainerExerciseDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const existing = await this.prisma.exercise.findFirst({
      where: { id: exerciseId, gymId },
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
      where: { id: exerciseId, gymId },
      include: { secondaryMuscles: { select: { muscle: true } } },
    });
    return this.exerciseToApi(updated);
  }

  async deleteExercise(
    actorUserId: string,
    gymId: string,
    exerciseId: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const found = await this.prisma.exercise.findFirst({
      where: { id: exerciseId, gymId },
      select: { id: true },
    });
    if (!found) {
      throw new NotFoundException('Exercise not found');
    }
    try {
      await this.prisma.exercise.delete({ where: { id: exerciseId } });
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

  async createWorkout(actorUserId: string, dto: CreateWorkoutDto) {
    return this.withWorkoutStorageHandling(async () => {
      const member = await this.requireMemberAndAccess(
        actorUserId,
        dto.member_id,
      );
      const workoutId = await this.persistNewWorkout(member, dto);
      return this.getWorkoutDetails(actorUserId, workoutId);
    });
  }

  /** `POST /trainers/workouts` — member must belong to `gymId`. */
  async createTrainerWorkout(
    actorUserId: string,
    gymId: string,
    dto: CreateWorkoutDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      await this.gymAccess.assertCanManageGym(actorUserId, gymId);
      const member = await this.prisma.gymUser.findFirst({
        where: {
          id: dto.member_id,
          gymId,
          role: GymRole.MEMBER,
          isActive: true,
        },
        select: { id: true, gymId: true },
      });
      if (!member) {
        throw new NotFoundException('Member not found in this gym');
      }
      const workoutId = await this.persistNewWorkout(member, dto);
      return this.getWorkoutDetails(actorUserId, workoutId);
    });
  }

  async listTrainerWorkouts(
    actorUserId: string,
    query: ListTrainerWorkoutsQueryDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      await this.gymAccess.assertCanManageGym(actorUserId, query.gymId);
      if (query.member_id) {
        const m = await this.prisma.gymUser.findFirst({
          where: {
            id: query.member_id,
            gymId: query.gymId,
            role: GymRole.MEMBER,
          },
          select: { id: true },
        });
        if (!m) {
          throw new NotFoundException('Member not found in this gym');
        }
      }

      const memberFilter = query.member_id
        ? Prisma.sql`AND w.gym_user_id = ${query.member_id}`
        : Prisma.empty;

      const rows = await this.prisma.$queryRaw<
        Array<{
          workout_id: string;
          member_id: string;
          title: string;
          date: Date;
          exercise_count: number;
        }>
      >(Prisma.sql`
        SELECT
          w.id AS workout_id,
          w.gym_user_id AS member_id,
          w.title,
          w.started_at AS date,
          COUNT(we.id)::int AS exercise_count
        FROM member_workout_plans w
        LEFT JOIN workout_plan_exercises we ON we.workout_id = w.id
        WHERE w.gym_id = ${query.gymId}
        ${memberFilter}
        GROUP BY w.id, w.gym_user_id, w.title, w.started_at
        ORDER BY w.started_at DESC
        LIMIT 200
      `);
      return rows;
    });
  }

  async updateTrainerWorkout(
    actorUserId: string,
    gymId: string,
    workoutId: string,
    dto: UpdateTrainerWorkoutDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      await this.gymAccess.assertCanManageGym(actorUserId, gymId);
      const hasUpdate = dto.title !== undefined || dto.notes !== undefined;
      if (!hasUpdate) {
        throw new BadRequestException('No fields to update');
      }

      const current = await this.prisma.$queryRaw<
        Array<{ title: string; notes: string | null }>
      >(Prisma.sql`
        SELECT title, notes FROM member_workout_plans
        WHERE id = ${workoutId} AND gym_id = ${gymId}
        LIMIT 1
      `);
      const row = current[0];
      if (!row) {
        throw new NotFoundException('Workout not found');
      }

      const title =
        dto.title !== undefined ? dto.title.trim() : row.title;
      const notes =
        dto.notes !== undefined ? (dto.notes?.trim() ?? null) : row.notes;

      await this.prisma.$executeRaw(Prisma.sql`
        UPDATE member_workout_plans
        SET
          title = ${title},
          notes = ${notes},
          updated_at = CURRENT_TIMESTAMP
        WHERE id = ${workoutId} AND gym_id = ${gymId}
      `);

      return this.getWorkoutDetails(actorUserId, workoutId);
    });
  }

  async deleteTrainerWorkout(
    actorUserId: string,
    gymId: string,
    workoutId: string,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      await this.gymAccess.assertCanManageGym(actorUserId, gymId);
      const deleted = await this.prisma.$executeRaw(Prisma.sql`
        DELETE FROM member_workout_plans
        WHERE id = ${workoutId} AND gym_id = ${gymId}
      `);
      const n = Number(deleted);
      if (n === 0) {
        throw new NotFoundException('Workout not found');
      }
      return { success: true as const };
    });
  }

  private async persistNewWorkout(
    member: { id: string; gymId: string },
    dto: CreateWorkoutDto,
  ): Promise<string> {
    const workoutId = randomUUID();
    const startedAt = new Date();

    await this.prisma.$transaction(async (tx) => {
      await tx.$executeRaw(Prisma.sql`
        INSERT INTO member_workout_plans
          (id, gym_id, gym_user_id, title, notes, started_at, completed)
        VALUES
          (${workoutId}, ${member.gymId}, ${member.id}, ${dto.title.trim()},
           ${dto.notes?.trim() ?? null}, ${startedAt}, false)
      `);

      for (const ex of dto.exercises) {
        const exercise = await tx.exercise.findFirst({
          where: { id: ex.exercise_id, gymId: member.gymId },
          select: { id: true },
        });
        if (!exercise) {
          throw new BadRequestException(
            `Invalid exercise_id: ${ex.exercise_id}`,
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
              (id, workout_exercise_id, set_number, reps, weight, completed)
            VALUES
              (${randomUUID()}, ${weId}, ${set.set_number},
               ${set.reps ?? 0}, ${set.weight ?? 0}, false)
          `);
        }
      }
    });

    return workoutId;
  }

  async listMemberWorkouts(actorUserId: string, gymUserId: string) {
    return this.withWorkoutStorageHandling(async () => {
      const member = await this.requireMemberAndAccess(actorUserId, gymUserId);
      const rows = await this.prisma.$queryRaw<
        Array<{
          workout_id: string;
          title: string;
          date: Date;
          exercise_count: number;
        }>
      >(Prisma.sql`
        SELECT
          w.id AS workout_id,
          w.title,
          w.started_at AS date,
          COUNT(we.id)::int AS exercise_count
        FROM member_workout_plans w
        LEFT JOIN workout_plan_exercises we ON we.workout_id = w.id
        WHERE w.gym_id = ${member.gymId} AND w.gym_user_id = ${gymUserId}
        GROUP BY w.id
        ORDER BY w.started_at DESC
        LIMIT 200
      `);
      return rows;
    });
  }

  async getWorkoutDetails(actorUserId: string, workoutId: string) {
    return this.withWorkoutStorageHandling(async () => {
      const workout = await this.prisma.$queryRaw<
        Array<{
          id: string;
          gym_id: string;
          title: string;
          started_at: Date | null;
          ended_at: Date | null;
          total_volume: number | null;
          total_sets: number | null;
        }>
      >(Prisma.sql`
        SELECT id, gym_id, title, started_at, ended_at, total_volume, total_sets
        FROM member_workout_plans
        WHERE id = ${workoutId}
        LIMIT 1
      `);
      const row = workout[0];
      if (!row) {
        throw new NotFoundException('Workout not found');
      }
      await this.assertCanAccessGym(actorUserId, row.gym_id);

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
          s.completed
        FROM workout_plan_exercises we
        JOIN exercises e ON e.id = we.exercise_id AND e."gymId" = ${row.gym_id}
        LEFT JOIN workout_exercise_sets s ON s.workout_exercise_id = we.id
        WHERE we.workout_id = ${workoutId}
        ORDER BY e.name ASC, s.set_number ASC
      `);

      const grouped = new Map<
        string,
        {
          exercise_id: string;
          name: string;
          sets: Array<Record<string, unknown>>;
        }
      >();
      for (const r of exercises) {
        if (!grouped.has(r.workout_exercise_id)) {
          grouped.set(r.workout_exercise_id, {
            exercise_id: r.exercise_id,
            name: r.name,
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

      const durationMin =
        row.started_at && row.ended_at
          ? Math.max(
              0,
              Math.round(
                (row.ended_at.getTime() - row.started_at.getTime()) / 60000,
              ),
            )
          : 0;

      return {
        title: row.title,
        duration: `${durationMin} min`,
        volume: `${row.total_volume ?? 0} kg`,
        sets: row.total_sets ?? 0,
        exercises: [...grouped.values()],
      };
    });
  }

  async addExerciseToWorkout(
    actorUserId: string,
    workoutId: string,
    dto: AddExerciseToWorkoutDto,
  ) {
    return this.withWorkoutStorageHandling(async () => {
      const gymId = await this.getWorkoutGymId(workoutId);
      await this.assertCanAccessGym(actorUserId, gymId);
      const e = await this.prisma.exercise.findFirst({
        where: { id: dto.exercise_id, gymId },
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
          (id, workout_exercise_id, set_number, reps, weight, completed)
        VALUES
          (${randomUUID()}, ${dto.workout_exercise_id}, ${dto.set_number},
           ${dto.reps ?? 0}, ${dto.weight ?? 0}, false)
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
          ended_at = CURRENT_TIMESTAMP,
          total_volume = ${volume},
          total_sets = ${sets},
          updated_at = CURRENT_TIMESTAMP
        WHERE id = ${workoutId}
      `);
      return this.getWorkoutDetails(actorUserId, workoutId);
    });
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

  private async getWorkoutGymId(workoutId: string): Promise<string> {
    return this.withWorkoutStorageHandling(async () => {
      const row = await this.prisma.$queryRaw<Array<{ gym_id: string }>>(
        Prisma.sql`
        SELECT gym_id FROM member_workout_plans WHERE id = ${workoutId} LIMIT 1
      `,
      );
      const gymId = row[0]?.gym_id;
      if (!gymId) {
        throw new NotFoundException('Workout not found');
      }
      return gymId;
    });
  }

  private async assertCanAccessGym(
    actorUserId: string,
    gymId: string,
  ): Promise<void> {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
  }

  private async requireMemberAndAccess(actorUserId: string, gymUserId: string) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: gymUserId },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.assertCanAccessGym(actorUserId, member.gymId);
    return member;
  }

  private async assertWorkoutAccess(actorUserId: string, workoutId: string) {
    const gymId = await this.getWorkoutGymId(workoutId);
    await this.assertCanAccessGym(actorUserId, gymId);
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
}
