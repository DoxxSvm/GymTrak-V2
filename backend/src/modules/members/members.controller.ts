import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiConflictResponse,
  ApiExtraModels,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiForbiddenResponse,
  ApiQuery,
  ApiTags,
  getSchemaPath,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequireBearerAuth } from '../../common/decorators/require-bearer-auth.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { OptionalGymIdQueryDto } from '../../common/dto/optional-gym-id-query.dto';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AddMemberSubscriptionDto } from './dto/add-subscription.dto';
import { CreateMemberDto } from './dto/create-member.dto';
import { MemberListQueryDto } from './dto/member-list-query.dto';
import { MemberTabQueryDto } from './dto/member-tab-query.dto';
import { ReceivePaymentDto } from './dto/receive-payment.dto';
import { UpdateMemberDto } from './dto/update-member.dto';
import { MemberListFilter } from './member-list-filter';
import { AddDietEntryDto } from './dto/add-diet-entry.dto';
import {
  MemberAttendanceHistoryQueryDto,
  MemberAttendanceSummaryQueryDto,
} from './dto/member-attendance-query.dto';
import {
  MemberDetailResponseSwagger,
  MemberListResponseSwagger,
  MemberProfileCardSwagger,
} from './dto/member-detail.swagger';
import {
  MemberPersonalWorkoutDetailSwagger,
  MemberPersonalWorkoutExerciseGroupSwagger,
  MemberPersonalWorkoutHistoryItemSwagger,
  MemberPersonalWorkoutHistoryResponseSwagger,
  MemberPersonalWorkoutListItemSwagger,
  MemberPersonalWorkoutSetRowSwagger,
} from './dto/member-personal-workout.swagger';
import { CreateTrainerExerciseDto } from '../workouts/dto/create-trainer-exercise.dto';
import { UpdateTrainerExerciseDto } from '../workouts/dto/update-trainer-exercise.dto';
import { UpdateTrainerWorkoutDto } from '../workouts/dto/update-trainer-workout.dto';
import { UpdateWorkoutCompletionDto } from '../workouts/dto/update-workout-completion.dto';
import { WorkoutCompletionSummarySwagger } from '../workouts/dto/workout-completion.swagger';
import { CreateMemberPersonalWorkoutDto } from './dto/create-member-personal-workout.dto';
import { ListMemberPersonalExercisesQueryDto } from './dto/list-member-personal-exercises-query.dto';
import { StartMemberPersonalWorkoutDto } from './dto/start-member-personal-workout.dto';
import { MemberPersonalWorkoutHistoryQueryDto } from './dto/member-personal-workout-history-query.dto';
import { MemberPersonalCatalogService } from './member-personal-catalog.service';
import { MemberStatisticsService } from './member-statistics.service';
import { MembersService } from './members.service';
import { MemberStatisticsQueryDto } from './dto/member-statistics-query.dto';

@ApiTags('Members')
@ApiBearerAuth()
@ApiExtraModels(
  MemberPersonalWorkoutSetRowSwagger,
  MemberPersonalWorkoutExerciseGroupSwagger,
  MemberPersonalWorkoutDetailSwagger,
  MemberPersonalWorkoutListItemSwagger,
  MemberPersonalWorkoutHistoryItemSwagger,
  MemberPersonalWorkoutHistoryResponseSwagger,
  WorkoutCompletionSummarySwagger,
  MemberListResponseSwagger,
  MemberDetailResponseSwagger,
  MemberProfileCardSwagger,
)
@Controller('members')
export class MembersController {
  constructor(
    private readonly members: MembersService,
    private readonly personalCatalog: MemberPersonalCatalogService,
    private readonly memberStatistics: MemberStatisticsService,
  ) {}
  @Get()
  @ApiOperation({
    summary: 'List members',
    description:
      'Requires Bearer auth. **`members[]`** rows include **`first_name`**, **`last_name`**, **`address`** (`User.address`, else legacy **`notes`** `Address:` line), **`dob`** (YYYY-MM-DD), **`aadhaar_number`** (`User.aadhaar_number`, else legacy **`notes`** `Aadhaar:` line), **`emergency_name`**, **`emergency_contact_phone`**, **`notes`**, **`age`**, **`profile_image`**, etc. **`items[]`** carries the same profile fields plus camelCase **`gymUserId`**, **`lifecycleStatus`**, etc. Root **`GET /members/{memberId}`** **`summary`** uses the **`members[]`** card shape for parity. Trainer/staff need `members:manage` (owners always allowed).',
  })
  @ApiQuery({
    name: 'gymId',
    required: true,
    description:
      'Gym context (may be inferred when the owner has exactly one gym).',
  })
  @ApiQuery({ name: 'page', required: false, type: Number })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'q', required: false, description: 'Alias for search' })
  @ApiQuery({
    name: 'status',
    required: false,
    enum: ['all', 'active', 'expired', 'inactive'],
  })
  @ApiOkResponse({
    description:
      'Paged list; **`members[]`** matches **`MemberListSummarySwagger`** (**`items[]`** = same extended profile fields plus `gymUserId` / camelCase identifiers).',
    type: MemberListResponseSwagger,
  })
  list(@CurrentUser() user: JwtUser, @Query() query: MemberListQueryDto) {
    const limit = query.limit ?? 20;
    const page = query.page ?? 1;
    const offset = query.offset ?? (page - 1) * limit;
    const q = query.search ?? query.q;
    const filter = this.resolveMemberListFilter(query);
    return this.members.list(user.sub, query.gymId, q, filter, limit, offset);
  }

  @Delete(':memberId')
  @ApiOperation({
    summary: 'Delete member',
    description:
      'Deletes the `GymUser` by `memberId`. Owner or users who can manage that member’s gym.',
  })
  @ApiParam({
    name: 'memberId',
    description: 'GymUser id (same as list `members[].id`)',
  })
  @ApiOkResponse({
    description: 'Member deleted successfully',
    content: {
      'application/json': {
        schema: {
          type: 'object',
          properties: {
            message: { type: 'string', example: 'Member deleted successfully' },
          },
        },
      },
    },
  })
  @ApiForbiddenResponse({ description: 'Cannot manage this gym' })
  @ApiNotFoundResponse({ description: 'Member not found' })
  deleteMember(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
  ) {
    return this.members.deleteMember(user.sub, memberId);
  }

  /** Member mobile home — JWT `sub` = User.id. Must stay above `:memberId` routes. */
  @Get('dashboard')
  @ApiOperation({
    summary: 'Member dashboard (self)',
    description:
      '**`gymId`** optional. **With `gymId`:** full dashboard for that gym (caller must be an active member there). **Without `gymId`:** user-only rows from **`User`** + global unread notifications — no gym-scoped metrics.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'When present, scopes the dashboard to this gym (`userId` + `gymId`). When omitted, only `User.id` from the token is used.',
  })
  selfDashboard(@CurrentUser() user: JwtUser, @Query('gymId') gymId?: string) {
    return this.members.getSelfMemberDashboard(user.sub, gymId);
  }

  /** Member app statistics (self) — must stay above `:memberId` routes. */
  @Get('statistics')
  @RequireBearerAuth()
  @ApiOperation({
    summary: 'Member statistics (self)',
    description:
      'Aggregates **MemberPersonalWorkoutPlan** (JWT `userId`). Optional **`gymId`**: also includes **MemberWorkoutPlan** + **AttendanceRecord** + **`diet_food_consumptions`** for that gym (active member only). All date bucketing is **UTC**. `period` controls summary + previous-period comparison; `weekly_activity` is always the **Mon–Sun week** containing `date` (default today). `active_calories` include food logged via **`POST /diet/food-consume`** plus workout duration estimates (≈5 kcal/min). `volume`, `sets`, and `duration` come from completed workouts. `attendance.days_with_activity` lists `YYYY-MM-DD` in the requested calendar month (`calendar_year` / `calendar_month`, default: month of `date`).',
  })
  @ApiQuery({
    name: 'period',
    required: false,
    enum: ['week', 'month', 'year'],
    description: 'Default `week`',
  })
  @ApiQuery({
    name: 'date',
    required: false,
    description:
      'Anchor `YYYY-MM-DD` (UTC) for period, weekly chart week, and default calendar month',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'When set, merge gym workouts and gym check-in days (member access required)',
  })
  @ApiQuery({ name: 'calendar_year', required: false, example: '2026' })
  @ApiQuery({ name: 'calendar_month', required: false, example: '2' })
  @ApiOkResponse({
    description:
      'Summary, monthly goal, weekly chart series (4 metrics), attendance day keys',
  })
  getMemberStatistics(
    @CurrentUser() user: JwtUser,
    @Query() query: MemberStatisticsQueryDto,
  ) {
    return this.memberStatistics.getSelfStatistics(user.sub, query);
  }

  /** Personal exercise library for the signed-in user (`sub`). No `gymId`. */
  @Post('exercises')
  @ApiOperation({
    summary: 'Create personal exercise (self, no gym)',
    description:
      'Same JSON as `POST /exercises` (`CreateTrainerExerciseDto`). Rows are keyed to JWT `sub` only — no gym.',
  })
  @ApiBody({ type: CreateTrainerExerciseDto })
  @ApiOkResponse({
    description: 'New exercise id and name',
    schema: {
      example: {
        id: '550e8400-e29b-41d4-a716-446655440000',
        name: 'Bench press',
      },
    },
  })
  createPersonalExercise(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateTrainerExerciseDto,
  ) {
    return this.personalCatalog.createExercise(user.sub, body);
  }

  @Get('exercises')
  @ApiOperation({
    summary: 'List personal exercises (self, no gym)',
    description:
      'Returns active personal exercises for the authenticated user.',
  })
  @ApiQuery({
    name: 'search',
    required: false,
    description: 'Case-insensitive filter on `name`',
  })
  @ApiOkResponse({
    description:
      'Exercise catalog rows (snake_case fields, same shape as gym trainer list)',
  })
  listPersonalExercises(
    @CurrentUser() user: JwtUser,
    @Query() query: ListMemberPersonalExercisesQueryDto,
  ) {
    return this.personalCatalog.listExercises(user.sub, query.search);
  }

  @Get('exercises/:exerciseId')
  @ApiOperation({ summary: 'Get one personal exercise' })
  @ApiParam({ name: 'exerciseId', description: 'Personal exercise id' })
  @ApiNotFoundResponse({
    description: 'Exercise not found or not owned by this user',
  })
  @ApiOkResponse({ description: 'Single exercise object' })
  getPersonalExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
  ) {
    return this.personalCatalog.getExercise(user.sub, exerciseId);
  }

  @Patch('exercises/:exerciseId')
  @ApiOperation({ summary: 'Update personal exercise (partial)' })
  @ApiParam({ name: 'exerciseId', description: 'Personal exercise id' })
  @ApiBody({ type: UpdateTrainerExerciseDto })
  @ApiNotFoundResponse({ description: 'Exercise not found' })
  @ApiOkResponse({ description: 'Updated exercise object' })
  updatePersonalExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Body() body: UpdateTrainerExerciseDto,
  ) {
    return this.personalCatalog.updateExercise(user.sub, exerciseId, body);
  }

  @Delete('exercises/:exerciseId')
  @ApiOperation({ summary: 'Delete personal exercise' })
  @ApiParam({ name: 'exerciseId', description: 'Personal exercise id' })
  @ApiNotFoundResponse({ description: 'Exercise not found' })
  @ApiConflictResponse({
    description: 'Exercise is still referenced by a personal workout',
  })
  @ApiOkResponse({
    description: 'Deleted',
    schema: { example: { success: true } },
  })
  deletePersonalExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
  ) {
    return this.personalCatalog.deleteExercise(user.sub, exerciseId);
  }

  /** Personal workouts for JWT `sub` — same nested payload as `CreateWorkoutDto` without `member_id`. */
  @Post('workouts')
  @ApiOperation({
    summary: 'Create personal workout (self, no gym)',
    description:
      '`title`, optional `notes`, `exercises[]` with `exercise_id` + `sets`. Each `exercise_id` must belong to `GET /members/exercises`. Response matches gym workout detail summary (`title`, `duration`, `volume`, `sets`, `exercises`).',
  })
  @ApiBody({ type: CreateMemberPersonalWorkoutDto })
  @ApiOkResponse({
    description:
      'Workout detail payload (same summary shape as trainer workout GET)',
    type: MemberPersonalWorkoutDetailSwagger,
  })
  createPersonalWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateMemberPersonalWorkoutDto,
  ) {
    return this.personalCatalog.createWorkout(user.sub, body);
  }

  @Get('workouts')
  @ApiOperation({
    summary: 'List personal workouts',
    description: 'Summary rows for the authenticated user (no gym).',
  })
  @ApiOkResponse({
    description: 'List of workouts',
    type: MemberPersonalWorkoutListItemSwagger,
    isArray: true,
  })
  listPersonalWorkouts(@CurrentUser() user: JwtUser) {
    return this.personalCatalog.listWorkouts(user.sub);
  }

  /** Register before `workouts/:workoutId` so `history` is not parsed as an id. */
  @Get('workouts/history')
  @ApiOperation({
    summary: 'Personal workout history (self)',
    description:
      'Paginated list for the authenticated user (`userId` = JWT `sub`). Optional `from` / `to` (YYYY-MM-DD, UTC) filter on `startedAt`, and `completed` to limit to finished sessions.',
  })
  @ApiOkResponse({
    description:
      'Page of workout summaries (includes `userId` + `user_id` per row)',
    type: MemberPersonalWorkoutHistoryResponseSwagger,
  })
  getPersonalWorkoutHistory(
    @CurrentUser() user: JwtUser,
    @Query() query: MemberPersonalWorkoutHistoryQueryDto,
  ) {
    return this.personalCatalog.listWorkoutHistory(user.sub, query);
  }

  @Post('workouts/start')
  @ApiOperation({
    summary: 'Start personal workout session',
    description:
      'Creates an in-progress plan with `startedAt` now and no exercises. Add exercises via your existing flows or `POST /members/workouts` with a full body.',
  })
  @ApiBody({ type: StartMemberPersonalWorkoutDto, required: false })
  @ApiOkResponse({
    description:
      'New session detail (`exercises` may be empty until you add work)',
    type: MemberPersonalWorkoutDetailSwagger,
  })
  startPersonalWorkout(
    @CurrentUser() user: JwtUser,
    @Body() body: StartMemberPersonalWorkoutDto,
  ) {
    return this.personalCatalog.startWorkoutSession(user.sub, body);
  }

  @Get('workouts/:workoutId')
  @ApiOperation({
    summary: 'Get personal workout detail',
    description:
      'Same summary shape as gym workout details: `title`, `duration`, `volume`, `sets`, `exercises` with nested `sets`.',
  })
  @ApiParam({ name: 'workoutId', description: 'Personal workout id' })
  @ApiNotFoundResponse({ description: 'Workout not found' })
  @ApiOkResponse({ type: MemberPersonalWorkoutDetailSwagger })
  getPersonalWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.personalCatalog.getWorkoutDetails(user.sub, workoutId);
  }

  @Get('workouts/:workoutId/completion')
  @ApiOperation({
    summary: 'Get completed workout summary for editing',
    description:
      'Personal (`member_personal_workout_plans`) or gym (`member_workout_plans`) workout by id. Returns editable `duration_minutes`. Optional `?gymId=` / `X-Gym-Id` when validating gym scope.',
  })
  @ApiParam({ name: 'workoutId', description: 'Workout id' })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Optional gym scope when the workout belongs to a gym.',
  })
  @ApiNotFoundResponse({ description: 'Workout not found' })
  @ApiOkResponse({ type: WorkoutCompletionSummarySwagger })
  getPersonalWorkoutCompletion(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.personalCatalog.getWorkoutCompletionSummary(
      user.sub,
      workoutId,
      gymId,
    );
  }

  @Patch('workouts/:workoutId/completion')
  @ApiOperation({
    summary: 'Update completed workout duration',
    description:
      'Personal or gym workout. Persists edited `duration_minutes`. Optional `?gymId=` / `X-Gym-Id` for gym workouts.',
  })
  @ApiParam({ name: 'workoutId', description: 'Workout id' })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Optional gym scope when the workout belongs to a gym.',
  })
  @ApiBody({ type: UpdateWorkoutCompletionDto })
  @ApiNotFoundResponse({ description: 'Workout not found' })
  @ApiOkResponse({ type: WorkoutCompletionSummarySwagger })
  updatePersonalWorkoutCompletion(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Body() body: UpdateWorkoutCompletionDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.personalCatalog.updateWorkoutCompletionSummary(
      user.sub,
      workoutId,
      body,
      gymId,
    );
  }

  @Post('workouts/:workoutId/stop')
  @ApiOperation({
    summary: 'Stop personal workout session',
    description:
      'Sets `endedAt`, marks `completed`, and recomputes `totalVolume` / `totalSets` / `duration` from logged sets. Idempotent if already stopped.',
  })
  @ApiParam({ name: 'workoutId', description: 'Personal workout id' })
  @ApiNotFoundResponse({ description: 'Workout not found' })
  @ApiOkResponse({
    description: 'Workout completion summary after finalize',
    type: WorkoutCompletionSummarySwagger,
  })
  stopPersonalWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.personalCatalog.stopWorkoutSession(user.sub, workoutId);
  }

  @Patch('workouts/:workoutId')
  @ApiOperation({ summary: 'Update personal workout title / notes' })
  @ApiParam({ name: 'workoutId', description: 'Personal workout id' })
  @ApiBody({ type: UpdateTrainerWorkoutDto })
  @ApiNotFoundResponse({ description: 'Workout not found' })
  @ApiOkResponse({ type: MemberPersonalWorkoutDetailSwagger })
  updatePersonalWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Body() body: UpdateTrainerWorkoutDto,
  ) {
    return this.personalCatalog.updateWorkout(user.sub, workoutId, body);
  }

  @Delete('workouts/:workoutId')
  @ApiOperation({
    summary: 'Delete personal workout (cascades sets / plan exercises)',
  })
  @ApiParam({ name: 'workoutId', description: 'Personal workout id' })
  @ApiNotFoundResponse({ description: 'Workout not found' })
  @ApiOkResponse({
    description: 'Deleted',
    schema: { example: { success: true } },
  })
  deletePersonalWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.personalCatalog.deleteWorkout(user.sub, workoutId);
  }

  private resolveMemberListFilter(
    query: MemberListQueryDto,
  ): MemberListFilter | undefined {
    if (query.active === true && query.expired === true) {
      throw new BadRequestException(
        'Use only one of active=true or expired=true (or use status=active|expired).',
      );
    }
    if (query.status != null && query.status !== 'all') {
      return query.status === 'active'
        ? MemberListFilter.ACTIVE
        : query.status === 'expired'
          ? MemberListFilter.EXPIRED
          : query.status === 'inactive'
            ? MemberListFilter.INACTIVE
            : undefined;
    }
    if (query.expired === true) {
      return MemberListFilter.EXPIRED;
    }
    if (query.active === true) {
      return MemberListFilter.ACTIVE;
    }
    return query.filter;
  }

  @Post()
  create(@CurrentUser() user: JwtUser, @Body() body: CreateMemberDto) {
    return this.members.create(user.sub, body);
  }

  @Get(':memberId')
  @ApiOperation({
    summary: 'Member detail profile',
    description:
      '**Path:** `memberId` = GymUser id from `GET /members` (`members[].id` or `items[].gymUserId`). **Query:** `gymId` (required). Returns `summary` (list-card shape), `subscription` (`stats`, `current_subscriptions` — all in-window periods, `upcoming_subscriptions`, `expired_subscriptions`; dates `YYYY-MM-DD`), membership fields, `user` (incl. `avatarUrl`), `contact`, `tabs`, `attendance`, **`paymentSummary`** (paid this UTC year + subscription balance owed), **`paymentHistory`** (latest 50 completed payments with gym plan name). Bearer required; owner allowed; trainer/staff need `members:manage`. OpenAPI: `MemberDetailResponse` in `docs/gymtrak-api.openapi.json`.',
  })
  @ApiParam({
    name: 'memberId',
    description: 'GymUser id (member row id from the list)',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Gym context (optional)',
  })
  @ApiOkResponse({
    description: 'Member detail',
    type: MemberDetailResponseSwagger,
  })
  @ApiForbiddenResponse({
    description: 'Insufficient permissions for this gym',
  })
  @ApiNotFoundResponse({ description: 'Member not found' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.getDetail(user.sub, query.gymId, memberId);
  }

  @Get(':memberId/profile')
  @ApiOperation({
    summary: 'Member profile (legacy flat shape)',
    description:
      'Flat card: `stats`, subscriptions, **`heightCm`**, **`weightKg`**, **`activityLevel`**, **`fitnessGoal`**, **`wellness`**, **`maintenanceCalories`** (user-provided, persisted), **`age`**, **`profile_image`**. BMI in `wellness` is recomputed from height/weight. **With `gymId`:** `memberId` is GymUser id; returns **`gym`** (basic gym info), nested **`subscription`** (current, freeze, upcoming, past), plus top-level subscription arrays (legacy). Staff/owner or the member themselves. **Without `gymId`:** `memberId` = JWT user id (self).',
  })
  @ApiParam({
    name: 'memberId',
    description:
      'GymUser id when `gymId` is sent; authenticated User id when `gymId` is omitted (self only).',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'If set, gym-scoped profile from `GymUser`. If omitted, user-scoped profile from `User` (self only).',
  })
  @ApiOkResponse({
    description: 'Profile card',
    type: MemberProfileCardSwagger,
  })
  @ApiForbiddenResponse({ description: 'Insufficient permissions' })
  @ApiNotFoundResponse({ description: 'Member not found' })
  profile(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: OptionalGymIdQueryDto,
  ) {
    return this.members.getProfile(user.sub, query.gymId, memberId);
  }

  @Patch(':memberId/profile')
  @ApiOperation({
    summary: 'Update member profile',
    description:
      '**With `?gymId=`** (`memberId` = GymUser id): same payload shape as **`POST /members`** create — **`first_name`** / **`last_name`** / **`fullName`**, **`heightCm`**, **`weightKg`**, **`activityLevel`** (`LOW` \| `MODERATE` \| `HIGH`), **`fitnessGoal`**, **`maintenanceCalories`** (saved as sent), **`address`** on **`User`** (legacy `Address:` **`notes`** lines stripped on update), **`aadhaar_number`** on **`User`** (legacy `Aadhaar:` lines stripped), **`dob`** or **`dateOfBirth`**, **`avatarUrl`** / **`profile_image`**, **`emergency_contact_phone`** / **`emergency_contact_name`** (camelCase aliases accepted), **`date_of_joining`**, **`age`** (`User.ageYears`), etc. Server recomputes BMI in **`User.wellness`** when height/weight change. Omit **`gymId`** only for **`memberId`** = JWT user (**self**) — **`User`** fields including **`gender`**, **`address`**, and **`aadhaar_number`**; structured gym-only fields require **`gymId`**.',
  })
  @ApiParam({
    name: 'memberId',
    description:
      'GymUser id when `gymId` is sent; authenticated User id when `gymId` is omitted (self only).',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'When set, gym-scoped update (staff) and full detail for that member row. When omitted, self user update; required implicitly if you have more than one membership (to pick gym for full detail).',
  })
  @ApiBody({ type: UpdateMemberDto })
  @ApiOkResponse({
    description:
      'Updated profile card (same shape as GET profile) when `gymId` is set; otherwise gym-free self profile.',
    type: MemberProfileCardSwagger,
  })
  @ApiForbiddenResponse({ description: 'Insufficient permissions' })
  @ApiNotFoundResponse({ description: 'Member not found' })
  @ApiConflictResponse({ description: 'Email already in use' })
  updateProfile(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: OptionalGymIdQueryDto,
    @Body() body: UpdateMemberDto,
  ) {
    return this.members.updateMemberProfile(user.sub, query.gymId, memberId, body);
  }

  @Put(':memberId/profile')
  @ApiOperation({
    summary: 'Update member profile (PUT)',
    description:
      'Same as **`PATCH /members/{memberId}/profile`**. Persists **`heightCm`**, **`weightKg`**, **`activityLevel`**, **`fitnessGoal`**, **`maintenanceCalories`**, etc. Response is **`MemberProfileCardResponse`** when **`gymId`** is set, including top-level **`maintenanceCalories`** matching the saved value.',
  })
  @ApiParam({
    name: 'memberId',
    description:
      'GymUser id when `gymId` is sent; authenticated User id when `gymId` is omitted (self only).',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'When set, gym-scoped update; member may update their own row, staff/owner may update any member.',
  })
  @ApiBody({ type: UpdateMemberDto })
  @ApiOkResponse({
    description: 'Updated profile card (same shape as GET profile)',
    type: MemberProfileCardSwagger,
  })
  @ApiForbiddenResponse({ description: 'Insufficient permissions' })
  @ApiNotFoundResponse({ description: 'Member not found' })
  @ApiConflictResponse({ description: 'Email already in use' })
  putProfile(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: OptionalGymIdQueryDto,
    @Body() body: UpdateMemberDto,
  ) {
    return this.members.updateMemberProfile(user.sub, query.gymId, memberId, body);
  }

  @Patch(':memberId')
  update(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMemberDto,
  ) {
    return this.members.update(user.sub, query.gymId, memberId, body);
  }

  @Put(':memberId')
  replace(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMemberDto,
  ) {
    return this.members.update(user.sub, query.gymId, memberId, body);
  }

  @Delete(':memberId')
  remove(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.softDelete(user.sub, query.gymId, memberId);
  }

  @Get(':memberId/subscriptions')
  listSubscriptions(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.listSubscriptions(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
    );
  }

  @Post(':memberId/subscriptions')
  addSubscription(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: AddMemberSubscriptionDto,
  ) {
    return this.members.addSubscription(user.sub, query.gymId, memberId, body);
  }

  @Get(':memberId/attendance/summary')
  @ApiOperation({
    summary: 'Member attendance — month summary',
    description:
      'Calendar month (UTC grid) + stats + punctuality-labelled recent_logs + months_overview. Default month from gym timezone when month/year omitted. Query: gymId (required), month, year, monthsOverviewLimit, recentLimit.',
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({ name: 'month', required: false, description: '1–12 with year' })
  @ApiQuery({ name: 'year', required: false })
  @ApiQuery({
    name: 'monthsOverviewLimit',
    required: false,
    description: 'Max months_overview rows (default 24, max 60)',
  })
  @ApiQuery({
    name: 'recentLimit',
    required: false,
    description: 'Max recent_logs (default 20, max 50)',
  })
  attendanceSummary(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberAttendanceSummaryQueryDto,
  ) {
    return this.members.getAttendanceSummary(user.sub, query.gymId, memberId, {
      month: query.month,
      year: query.year,
      timezone: query.timezone,
      monthsOverviewLimit: query.monthsOverviewLimit,
      recentLimit: query.recentLimit,
    });
  }

  @Get(':memberId/attendance/history')
  @ApiOperation({
    summary: 'Member attendance — paginated history',
    description:
      'Optional from/to (YYYY-MM-DD UTC on attendedOn). OpenAPI: MemberAttendanceHistoryResponse in docs/gymtrak-api.openapi.json.',
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({ name: 'from', required: false, description: 'YYYY-MM-DD' })
  @ApiQuery({ name: 'to', required: false, description: 'YYYY-MM-DD' })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'offset', required: false })
  attendanceHistory(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberAttendanceHistoryQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.getAttendanceHistory(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
      query.from,
      query.to,
      query.timezone,
    );
  }

  /** @deprecated Prefer `GET .../attendance/summary` or `.../history`. */
  @Get(':memberId/attendance')
  @ApiOperation({
    summary: 'Member attendance (legacy)',
    description:
      'Deprecated. With month+year → same as attendance/summary; else same as attendance/history.',
    deprecated: true,
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({ name: 'month', required: false })
  @ApiQuery({ name: 'year', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'offset', required: false })
  listAttendance(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.listAttendance(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
      query.month,
      query.year,
    );
  }

  @Get(':memberId/payments')
  listPayments(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.listPayments(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
    );
  }

  @Get(':memberId/payment-summary')
  paymentSummary(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.getPaymentSummary(user.sub, query.gymId, memberId);
  }

  @Post(':memberId/payments')
  receivePayment(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: ReceivePaymentDto,
  ) {
    return this.members.receivePayment(user.sub, query.gymId, memberId, body);
  }

  @Get(':memberId/diet')
  diet(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.listDiet(user.sub, query.gymId, memberId);
  }

  @Post('/diet')
  addDiet(@CurrentUser() user: JwtUser, @Body() body: AddDietEntryDto) {
    return this.members.addDietEntry(user.sub, body);
  }
}
