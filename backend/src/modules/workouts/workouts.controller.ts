import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiForbiddenResponse,
  ApiOkResponse,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AddExerciseToWorkoutDto } from './dto/add-exercise-to-workout.dto';
import { AddSetDto } from './dto/add-set.dto';
import { CreateTrainerExerciseDto } from './dto/create-trainer-exercise.dto';
import { ExerciseDetailsQueryDto } from './dto/exercise-details-query.dto';
import { ListTrainerExercisesQueryDto } from './dto/list-trainer-exercises-query.dto';
import { UpdateTrainerExerciseDto } from './dto/update-trainer-exercise.dto';
import { CreateWorkoutDto } from './dto/create-workout.dto';
import { ListWorkoutsQueryDto } from './dto/list-workouts-query.dto';
import { WorkoutIdOnlyDto } from './dto/workout-id-only.dto';
import { UnifiedWorkoutHistoryQueryDto } from './dto/unified-workout-history-query.dto';
import { PatchWorkoutBodyDto } from './dto/patch-workout-body.dto';
import { UpdateSetDto } from './dto/update-set.dto';
import { UpdateWorkoutCompletionDto } from './dto/update-workout-completion.dto';
import { WorkoutCompletionSummarySwagger } from './dto/workout-completion.swagger';
import { WorkoutsService } from './workouts.service';

@ApiTags('Workouts')
@ApiBearerAuth()
@Controller()
export class WorkoutsController {
  constructor(private readonly workouts: WorkoutsService) { }

  @Post('exercises')
  createExercise(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateTrainerExerciseDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.createExercise(user.sub, body, gymId);
  }

  @Get('exercises')
  listExercises(
    @CurrentUser() user: JwtUser,
    @Query() query: ListTrainerExercisesQueryDto,
  ) {
    return this.workouts.listExercises(user.sub, query);
  }

  @Get('exercises/:exerciseId')
  @ApiOperation({
    summary: 'Exercise detail (Summary + History)',
    description:
      'Catalog exercise metadata plus logged stats from your workouts: PRs, monthly chart (`chart_range` × `chart_metric`), last session headline for that metric, and paginated session history. **Personal catalog:** stats use your personal workout logs (`gym_id` null). **Gym catalog:** stats use your **member** sessions at that gym (`gym_user_id`); optional `gymId` must match the exercise’s gym. `gymId` must be omitted for personal-scoped exercises.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'For gym-scoped catalog exercises only — must match the exercise’s gym when set.',
  })
  @ApiQuery({
    name: 'chart_range',
    required: false,
    enum: ['3m', '6m', '12m'],
    description: 'UTC calendar months in `summary.chart` (default `3m`).',
  })
  @ApiQuery({
    name: 'chart_metric',
    required: false,
    enum: ['heaviest_weight', 'one_rep_max', 'best_set_volume'],
    description:
      'Metric for chart + `last_performance` (default `heaviest_weight`).',
  })
  @ApiQuery({
    name: 'history_page',
    required: false,
    description: '1-based page for `history.items` (default 1).',
  })
  @ApiQuery({
    name: 'history_limit',
    required: false,
    description: 'Page size for history (default 20, max 50).',
  })
  @ApiOkResponse({
    description:
      '`exercise` (catalog), `summary` (PRs, chart, last performance), `history` (sessions with sets).',
  })
  @ApiForbiddenResponse({ description: 'No access to gym or catalog entry' })
  getExerciseDetails(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Query() query: ExerciseDetailsQueryDto,
  ) {
    return this.workouts.getExerciseDetails(user.sub, exerciseId, query);
  }

  @Patch('exercises/:exerciseId')
  updateExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Body() body: UpdateTrainerExerciseDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.updateExercise(user.sub, exerciseId, body, gymId);
  }

  @Delete('exercises/:exerciseId')
  deleteCatalogExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.deleteExercise(user.sub, exerciseId, gymId);
  }

  @Post('workouts')
  @ApiOperation({
    summary: 'Create workout (legacy path)',
    description:
      '`member_id` in body → gym workout (`user_id` = JWT `sub`, member via `gym_user_id`). Gym inferred from member; optional `gymId` must match if sent. No `member_id` → personal workout. Optional body **`created_by`**: `trainer` or `member` — sets persisted `created_by_role` / response `created_by`; omit to use JWT actor’s resolved gym role.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Optional with `member_id` — must match that member’s gym when set.',
  })
  @ApiBody({ type: CreateWorkoutDto })
  @ApiOkResponse({
    description: 'Workout detail (includes `created_by` and `created_by_role`)',
    schema: {
      type: 'object',
      properties: {
        title: { type: 'string', example: 'Push day' },
        created_by: {
          type: 'string',
          enum: ['trainer', 'member'],
          example: 'trainer',
        },
        created_by_role: {
          type: 'string',
          enum: ['OWNER', 'TRAINER', 'STAFF', 'MEMBER'],
          example: 'TRAINER',
        },
        duration: { type: 'string', example: '0 min' },
        volume: { type: 'string', example: '0 kg' },
        sets: { type: 'integer', example: 3 },
        is_saved: { type: 'boolean', example: false },
        exercises: { type: 'array', items: { type: 'object' } },
      },
    },
  })
  createWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateWorkoutDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.createWorkout(user.sub, body, gymId);
  }

  @Post('workouts/save')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Save workout (checkpoint)',
    description:
      'Body must be **only** `{ "workout_id": "..." }` — any other property is rejected. Sets `is_saved` to **true** on the plan, then returns the same payload as `GET /workouts/:workoutId` (includes `is_saved`). Gym-scoped plans require `?gymId=` matching the row. To change title/notes use `PATCH`; to add exercises/sets use `POST /workouts/:workoutId/exercises`, `POST /exercise-sets`, etc.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Required when the workout has `gym_id` set — must match. Omit for personal (`gym_id` null) workouts.',
  })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({
    description: 'Workout detail (same as GET /workouts/:workoutId)',
  })
  @ApiForbiddenResponse({ description: 'No access to this workout' })
  saveWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.saveUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('workouts/start')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Start workout session',
    description:
      '(Re)opens an active session on a **clone** of the plan: sets `started_at` now, clears `ended_at`, `completed=false`, `is_started=true`. The library template (`workout_id` in the request) is unchanged — use the **returned** `workout_id` for stop/sets/logging. Resumes a **paused** session on the same id. Idempotent if already in progress. Gym-scoped plans require `?gymId=`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Required when the plan has `gym_id` set — must match.',
  })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail (exercises may be empty)' })
  startWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.startUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('workouts/stop')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Stop workout session',
    description:
      'Body must be **only** `{ "workout_id": "..." }`. Sets `ended_at`, `completed`, and recomputes totals from all logged sets. Idempotent if already stopped. Gym-scoped plans require `?gymId=`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Required when stopping a gym-scoped workout (must match `gym_id` on the plan).',
  })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail after finalize' })
  stopWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.stopUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('workouts/pause')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Pause workout session',
    description:
      'Body must be **only** `{ "workout_id": "..." }`. Sets `is_started=false` while keeping the session open (`ended_at` null, `completed=false`). Idempotent if already paused. Call `POST /workouts/resume` to continue. Gym-scoped plans require `?gymId=`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Required when pausing a gym-scoped workout (must match `gym_id` on the plan).',
  })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail after pause' })
  pauseWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.pauseUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('workouts/resume')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Resume workout session',
    description:
      'Body must be **only** `{ "workout_id": "..." }`. Sets `is_started=true` on a paused session without resetting `started_at`. Idempotent if already in progress. Returns `400` if the workout was never started or is already completed. Gym-scoped plans require `?gymId=`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Required when resuming a gym-scoped workout (must match `gym_id` on the plan).',
  })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail after resume' })
  resumeWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.resumeUnifiedWorkout(user.sub, body, gymId);
  }

  @Patch('workouts')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Update workout (legacy path)',
    description:
      '**Partial:** `workout_id` plus optional `title` / `notes` / `created_by` / `completed` / `isSaved` (same as `PATCH /trainers/workouts/{workoutId}`; `title`, `notes`, and `created_by` are persisted unless you send **`exercises`**). **Full structure:** include non-empty **`exercises`** (same shape as `POST /workouts`); replaces all exercises and sets on the plan, optional **`member_id`** on gym workouts must match the plan’s member when sent. Optional **`?gymId=`** required when the plan has `gym_id` set.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Required when updating a gym-scoped workout — must match the plan’s `gym_id`.',
  })
  @ApiBody({ type: PatchWorkoutBodyDto })
  @ApiOkResponse({
    description:
      'Workout detail (includes `created_by` and `created_by_role`; same as GET /workouts/:workoutId)',
  })
  @ApiForbiddenResponse({ description: 'No access to this workout' })
  patchWorkoutLegacy(
    @CurrentUser() user: JwtUser,
    @Body() body: PatchWorkoutBodyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.patchLegacyWorkout(user.sub, body, gymId);
  }

  @Delete('workouts')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Delete workout (legacy path)',
    description:
      'Same behavior as `DELETE /trainers/workouts/{workoutId}` but pass **`workout_id`** in the JSON body. Optional **`?gymId=`** required when the plan is gym-scoped.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Required when deleting a gym-scoped workout — must match the plan’s `gym_id`.',
  })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({
    description: '{ success: true }',
    schema: {
      type: 'object',
      properties: { success: { type: 'boolean', example: true } },
    },
  })
  @ApiForbiddenResponse({ description: 'No access to this workout' })
  deleteWorkoutLegacy(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.deleteTrainerWorkout(
      user.sub,
      body.workout_id.trim(),
      gymId,
    );
  }

  @Get('workouts')
  @ApiOperation({
    summary: 'List workouts (legacy path)',
    description:
      'Same contract as `GET /trainers/workouts`. Rows where `user_id` = JWT subject. Optional `gymId` scopes to that gym (caller must manage it); omit for personal workouts (`gym_id` null). Optional `created_by`: `all` (default), `member`, or `trainer` (OWNER/TRAINER/STAFF). Returns **library templates only** (`started_at` null, not completed) — starting/stopping a session does not add rows here. Use `GET /workouts/history` for finished sessions. Max 200, newest first.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'When set, lists this user’s workouts in that gym; when omitted, personal workouts only.',
  })
  @ApiQuery({
    name: 'created_by',
    required: false,
    enum: ['all', 'member', 'trainer'],
    description: 'Filter by creator gym role bucket',
  })
  @ApiOkResponse({
    description: 'Workout summaries',
    content: {
      'application/json': {
        schema: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              workout_id: { type: 'string' },
              member_id: { type: 'string', nullable: true },
              title: { type: 'string' },
              date: { type: 'string', format: 'date-time' },
              exercise_count: { type: 'integer' },
              is_saved: { type: 'boolean' },
              created_by: {
                type: 'string',
                enum: ['trainer', 'member'],
                description: 'Creator bucket for UI filters',
              },
              created_by_role: {
                type: 'string',
                enum: ['OWNER', 'TRAINER', 'STAFF', 'MEMBER'],
              },
              assetUrl: { type: 'string', nullable: true },
            },
          },
        },
      },
    },
  })
  @ApiForbiddenResponse({ description: 'No access to gym' })
  listWorkouts(
    @CurrentUser() user: JwtUser,
    @Query() query: ListWorkoutsQueryDto,
  ) {
    return this.workouts.listWorkouts(user.sub, query);
  }

  /** Registered before `GET workouts/:workoutId` so `history` is not captured as an id. */
  @Get('workouts/history')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Workout history (paginated)',
    description:
      'Lists workouts for the JWT subject only (no `userId` query). Omit `gymId` for personal plans (`gym_id` null). With `gymId`: members see their sessions at that gym (`gym_user_id`); trainers see plans they authored in that gym. Each item includes `exercises` with `asset_url` for app thumbnails. `page` is **0-based**.',
  })
  @ApiOkResponse({
    description: 'Paginated summaries with duration and exercise_count',
  })
  @ApiForbiddenResponse({ description: 'No gym access' })
  workoutHistory(
    @CurrentUser() user: JwtUser,
    @Query() query: UnifiedWorkoutHistoryQueryDto,
  ) {
    return this.workouts.listUnifiedWorkoutHistory(user.sub, query);
  }

  @Get('members/:memberId/workouts')
  @ApiOperation({
    summary: 'List trainer-assigned workouts for one member',
    description:
      'Returns workouts created by gym staff (OWNER / TRAINER / STAFF) for the member’s GymUser id. Default filter is **`created_by=trainer`**. Each row includes `created_by_name` (trainer or owner display name). Pass **`created_by=all`** to include member-created plans.',
  })
  @ApiQuery({
    name: 'created_by',
    required: false,
    enum: ['all', 'member', 'trainer'],
    description:
      'Filter by creator bucket. Default **`trainer`** (staff-assigned plans only).',
  })
  memberWorkouts(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query('created_by') createdBy?: string,
  ) {
    return this.workouts.listMemberWorkouts(
      user.sub,
      memberId,
      createdBy?.trim() || 'trainer',
    );
  }

  @Get('workouts/:workoutId/completion')
  @ApiOperation({
    summary: 'Get completed workout summary for editing',
    description:
      'Returns saved completion data (`duration_minutes`, volume, sets, exercises). Use `PATCH /workouts/:workoutId/completion` to update duration. Gym-scoped plans require `?gymId=`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Required when the plan has `gym_id` set — must match.',
  })
  @ApiOkResponse({ type: WorkoutCompletionSummarySwagger })
  getWorkoutCompletion(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.getWorkoutCompletionSummary(
      user.sub,
      workoutId,
      gymId,
    );
  }

  @Patch('workouts/:workoutId/completion')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Update completed workout duration',
    description:
      'Adjusts session timestamps so `duration_minutes` matches the edited value. Gym-scoped plans require `?gymId=`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Required when the plan has `gym_id` set — must match.',
  })
  @ApiBody({ type: UpdateWorkoutCompletionDto })
  @ApiOkResponse({ type: WorkoutCompletionSummarySwagger })
  patchWorkoutCompletion(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Body() body: UpdateWorkoutCompletionDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.updateWorkoutCompletionSummary(
      user.sub,
      workoutId,
      body,
      gymId,
    );
  }

  @Get('workouts/:workoutId')
  details(@CurrentUser() user: JwtUser, @Param('workoutId') workoutId: string) {
    return this.workouts.getWorkoutDetails(user.sub, workoutId);
  }

  @Post('workouts/:workoutId/exercises')
  addExercise(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Body() body: AddExerciseToWorkoutDto,
  ) {
    return this.workouts.addExerciseToWorkout(user.sub, workoutId, body);
  }

  @Delete('workouts/:workoutId/exercises/:exerciseId')
  removeExercise(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Param('exerciseId') exerciseId: string,
  ) {
    return this.workouts.deleteExerciseFromWorkout(
      user.sub,
      workoutId,
      exerciseId,
    );
  }

  @Put('sets/:setId')
  updateSet(
    @CurrentUser() user: JwtUser,
    @Param('setId') setId: string,
    @Body() body: UpdateSetDto,
  ) {
    return this.workouts.updateSet(user.sub, setId, body);
  }

  @Post('exercise-sets')
  addSet(@CurrentUser() user: JwtUser, @Body() body: AddSetDto) {
    return this.workouts.addSet(user.sub, body);
  }

  @Post('workouts/:workoutId/complete')
  @ApiOperation({
    summary: 'Mark workout complete (legacy)',
    description:
      'Sets `completed`, `ended_at`, and aggregates volume/sets from completed sets. Returns completion summary (`duration_minutes`, exercises, etc.).',
  })
  @ApiOkResponse({ type: WorkoutCompletionSummarySwagger })
  complete(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.workouts.completeWorkout(user.sub, workoutId);
  }
}
