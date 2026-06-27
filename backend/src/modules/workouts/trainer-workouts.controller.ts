import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
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
import { CreateWorkoutDto } from './dto/create-workout.dto';
import { CreateTrainerExerciseDto } from './dto/create-trainer-exercise.dto';
import { ExerciseDetailsQueryDto } from './dto/exercise-details-query.dto';
import { ListTrainerExercisesQueryDto } from './dto/list-trainer-exercises-query.dto';
import { ListTrainerWorkoutsQueryDto } from './dto/list-trainer-workouts-query.dto';
import { WorkoutIdOnlyDto } from './dto/workout-id-only.dto';
import { UnifiedWorkoutHistoryQueryDto } from './dto/unified-workout-history-query.dto';
import { UpdateTrainerExerciseDto } from './dto/update-trainer-exercise.dto';
import { UpdateTrainerWorkoutDto } from './dto/update-trainer-workout.dto';
import { WorkoutsService } from './workouts.service';

@ApiTags('Workouts')
@ApiBearerAuth()
@Controller('trainers/workouts')
export class TrainerWorkoutsController {
  constructor(private readonly workouts: WorkoutsService) {}

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
      'Same as `GET /exercises/{exerciseId}`: catalog metadata, PRs, chart, last performance, paginated history.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'For gym-scoped exercises — must match the exercise’s gym when set.',
  })
  @ApiQuery({
    name: 'chart_range',
    required: false,
    enum: ['3m', '6m', '12m'],
  })
  @ApiQuery({
    name: 'chart_metric',
    required: false,
    enum: ['heaviest_weight', 'one_rep_max', 'best_set_volume'],
  })
  @ApiQuery({ name: 'history_page', required: false })
  @ApiQuery({ name: 'history_limit', required: false })
  @ApiOkResponse({ description: 'Exercise detail payload' })
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
  removeExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.deleteExercise(user.sub, exerciseId, gymId);
  }

  @Get()
  @ApiOperation({
    summary: 'List workouts',
    description:
      'Lists `member_workout_plans` where `user_id` = JWT subject. Optional `gymId` scopes to that gym (caller must manage it); omit for personal workouts (`gym_id` null). Same as `GET /workouts`. Optional `created_by`: `all`, `member`, or `trainer`. Library templates only — sessions excluded. Max 200, newest first.',
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
            },
          },
        },
      },
    },
  })
  @ApiForbiddenResponse({ description: 'No access to gym' })
  listWorkouts(
    @CurrentUser() user: JwtUser,
    @Query() query: ListTrainerWorkoutsQueryDto,
  ) {
    return this.workouts.listTrainerWorkouts(user.sub, query);
  }

  @Post()
  @ApiOperation({
    summary: 'Create workout',
    description:
      '`member_id` in body → gym workout (`user_id` = JWT `sub`). Gym inferred from member; optional `gymId` must match if sent. No `member_id` → personal workout. Optional body **`created_by`**: `trainer` or `member`; omit to use JWT actor’s resolved gym role.',
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
    return this.workouts.createTrainerWorkout(user.sub, body, gymId);
  }

  @Post('save')
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
      'Same as `POST /workouts/save` — body only `workout_id`; sets `is_saved` on the plan.',
  })
  @ApiQuery({ name: 'gymId', required: false })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail' })
  saveWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.saveUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('start')
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
      'Same as `POST /workouts/start` — body only `workout_id`. Resumes a paused session.',
  })
  @ApiQuery({ name: 'gymId', required: false })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail' })
  startWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.startUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('stop')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Stop workout session',
    description: 'Same as `POST /workouts/stop` — body only `workout_id`.',
  })
  @ApiQuery({ name: 'gymId', required: false })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail' })
  stopWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.stopUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('pause')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Pause workout session',
    description: 'Same as `POST /workouts/pause` — body only `workout_id`.',
  })
  @ApiQuery({ name: 'gymId', required: false })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail' })
  pauseWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.pauseUnifiedWorkout(user.sub, body, gymId);
  }

  @Post('resume')
  @UsePipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  )
  @ApiOperation({
    summary: 'Resume workout session',
    description: 'Same as `POST /workouts/resume` — body only `workout_id`.',
  })
  @ApiQuery({ name: 'gymId', required: false })
  @ApiBody({ type: WorkoutIdOnlyDto })
  @ApiOkResponse({ description: 'Workout detail' })
  resumeWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: WorkoutIdOnlyDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.resumeUnifiedWorkout(user.sub, body, gymId);
  }

  @Get('history')
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
      'Same as `GET /workouts/history` — JWT subject only; optional `gymId`; no `userId` query.',
  })
  @ApiOkResponse({ description: 'Paginated workout summaries' })
  workoutHistory(
    @CurrentUser() user: JwtUser,
    @Query() query: UnifiedWorkoutHistoryQueryDto,
  ) {
    return this.workouts.listUnifiedWorkoutHistory(user.sub, query);
  }

  @Get(':workoutId')
  getWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.workouts.getWorkoutDetails(user.sub, workoutId);
  }

  @Patch(':workoutId')
  @ApiOperation({
    summary: 'Update workout',
    description:
      'Partial update: optional `title`, `notes`, and `created_by` (`trainer` | `member`). Response includes `created_by` and `created_by_role`.',
  })
  @ApiBody({ type: UpdateTrainerWorkoutDto })
  @ApiOkResponse({
    description:
      'Workout detail (includes `created_by` and `created_by_role`)',
  })
  updateWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Body() body: UpdateTrainerWorkoutDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.updateTrainerWorkout(user.sub, workoutId, body, gymId);
  }

  @Delete(':workoutId')
  deleteWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.workouts.deleteTrainerWorkout(user.sub, workoutId, gymId);
  }
}
