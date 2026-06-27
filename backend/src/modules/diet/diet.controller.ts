import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBearerAuth,
  ApiBody,
  ApiCreatedResponse,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiTags,
  ApiUnauthorizedResponse,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import {
  DietHistoryResponseSwagger,
  DietMealResponseSwagger,
  DietMutationSuccessSwagger,
  FoodConsumeDietResponseSwagger,
} from './dto/diet-api.swagger';
import { CreateDietMealDto } from './dto/create-diet-meal.dto';
import { DietHistoryQueryDto } from './dto/diet-history-query.dto';
import { FoodConsumeDietDto } from './dto/food-consume.dto';
import { ListDietMealsQueryDto } from './dto/list-diet-meals-query.dto';
import { UpdateDietMealDto } from './dto/update-diet-meal.dto';
import { DietService } from './diet.service';

@ApiTags('Diet')
@ApiBearerAuth()
@Controller('diet')
export class DietController {
  constructor(private readonly diet: DietService) {}

  @Get('history')
  @ApiOperation({
    summary: 'Diet history (member day view)',
    description:
      'Common member API: same route for gym and free-plan users. Data is always the **authenticated user’s** log (`user_id` = JWT `sub`). ' +
      '**`gymId` is optional.** If provided, results are **gym-scoped** (only rows with that `gymId`) and you must be an **active member** at that gym; times use the gym’s timezone. ' +
      'If omitted, results are **user-scoped** (all your rows for that calendar day); time labels use UTC when a row has no gym. ' +
      '`date` defaults to today (UTC) when omitted. **`recurring_meals`** lists weekly scheduled meals for that day (`repeat_days`: 0=Mon … 6=Sun) without changing consumed `meal_logs` / `macros`.',
  })
  @ApiQuery({
    name: 'date',
    required: false,
    example: '2026-02-17',
    description: 'YYYY-MM-DD; defaults to today in UTC if omitted',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description:
      'Optional. Gym-scoped day: filter to this `gymId` + your `userId`; requires active member role at the gym.',
  })
  @ApiQuery({
    name: 'target_kcal',
    required: false,
    example: 2200,
    description:
      'Daily calorie goal for `remaining_kcal` (default 2000 if omitted)',
  })
  @ApiOkResponse({ type: DietHistoryResponseSwagger })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({
    description:
      '`gymId` was sent but you are not an active member at that gym (cross-gym access blocked)',
  })
  getHistory(
    @CurrentUser() user: JwtUser,
    @Query() query: DietHistoryQueryDto,
  ) {
    return this.diet.getDietHistory(user.sub, query);
  }

  @Post('food-consume')
  @HttpCode(201)
  @ApiOperation({
    summary: 'Log consumed food',
    description:
      'Record lines for the signed-in user’s `userId`, or (staff) the user behind `member_id`. ' +
      'Optional `gymId` is stored on the row; pass it to use a gym’s `GET /diet/food?gymId=` catalog. ' +
      'Omit `gymId` for personal-catalog foods only.',
  })
  @ApiQuery({ name: 'gymId', required: false })
  @ApiQuery({
    name: 'member_id',
    required: false,
    description: 'Staff: log for this member (GymUser id)',
  })
  @ApiCreatedResponse({ type: FoodConsumeDietResponseSwagger })
  @ApiBadRequestResponse({
    description: 'Invalid payload, date, or diet_food_id',
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'Not allowed to log for this member' })
  @ApiNotFoundResponse({ description: 'Invalid member_id' })
  consumeFood(
    @CurrentUser() user: JwtUser,
    @Body() body: FoodConsumeDietDto,
    @Query('gymId') gymId?: string,
    @Query('member_id') memberId?: string,
  ) {
    return this.diet.recordFoodConsumption(user.sub, body, gymId, memberId);
  }

  @Get()
  @ApiOperation({
    summary: 'List meals',
    description:
      'Max 200, newest first. Same full meal shape as `POST /diet` (including `created_by` and `created_by_role` on each row). **Staff:** optional `gymId` scopes to one gym or all gyms you manage. **Members:** optional `gymId` = your meals at that gym; omit `gymId` = all your meals (gym + personal, `member_id` null on personal rows). With `member_id`, staff filters to that GymUser. Optional `created_by`: `all` (default), `member`, or `trainer` (OWNER/TRAINER/STAFF).',
  })
  @ApiQuery({
    name: 'member_id',
    required: false,
    description: 'Filter by member GymUser id',
  })
  @ApiQuery({
    name: 'created_by',
    required: false,
    enum: ['all', 'member', 'trainer'],
    description: 'Filter by creator gym role bucket',
  })
  @ApiOkResponse({
    description: 'Meals with nested `food_items`',
    type: DietMealResponseSwagger,
    isArray: true,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({
    description: '`member_id` given but not a member of this gym',
  })
  list(@CurrentUser() user: JwtUser, @Query() query: ListDietMealsQueryDto) {
    return this.diet.listMeals(user.sub, query);
  }

  @Post()
  @HttpCode(201)
  @ApiOperation({
    summary: 'Create meal',
    description:
      'Common member API: **`member_id` is optional.** Omit it to create **your own** meal (`userId` = JWT `sub`). **`gymId` query is optional:** resolved from query, JWT default gym, or your only active member gym; if none apply, meal is **user-scoped** (`gymId` / `member_id` null, personal foods only). Gym-scoped self-create requires active **member** at that gym. ' +
      'With `member_id`, staff/owners create for that member’s GymUser id (same `gymId` resolution as other diet catalog writes). ' +
      '`food_items` / `diet_food_id` should reference foods for the **same** gym (see `GET /diet/food?gymId=`). `repeat_days`: 0 = Monday … 6 = Sunday. Optional body **`created_by`**: `trainer` or `member` — sets `created_by` / `created_by_role` on the response; omit to use JWT actor’s resolved gym role.',
  })
  @ApiBody({ type: CreateDietMealDto })
  @ApiCreatedResponse({
    description:
      'Created meal with `food_items`, `created_by`, and `created_by_role`',
    type: DietMealResponseSwagger,
  })
  @ApiBadRequestResponse({
    description: 'Invalid payload or unknown `diet_food_id`',
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({
    description:
      'No access to this gym, or (self-create) not an active member at the resolved gym',
  })
  @ApiNotFoundResponse({
    description:
      '`member_id` was sent but that member is not in the resolved gym',
  })
  create(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateDietMealDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.createMeal(user.sub, body, gymId, user.gymId);
  }

  @Get(':mealId')
  @ApiOperation({ summary: 'Get meal by id' })
  @ApiParam({ name: 'mealId', description: 'Diet meal id' })
  @ApiOkResponse({
    description:
      'Meal with `food_items`, `created_by`, and `created_by_role`',
    type: DietMealResponseSwagger,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Meal not found' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.getMeal(user.sub, mealId, gymId);
  }

  @Patch(':mealId')
  @ApiOperation({
    summary: 'Update meal',
    description:
      'Partial update. Optional **`created_by`**: `trainer` or `member`. When `food_items` is sent, it **replaces** all existing lines on the meal. Response includes `created_by` and `created_by_role`.',
  })
  @ApiParam({ name: 'mealId', description: 'Diet meal id' })
  @ApiBody({ type: UpdateDietMealDto })
  @ApiOkResponse({
    description:
      'Updated meal with `food_items`, `created_by`, and `created_by_role`',
    type: DietMealResponseSwagger,
  })
  @ApiBadRequestResponse({
    description:
      'No fields to update, or invalid `food_items` / `diet_food_id`',
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Meal not found' })
  update(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Body() body: UpdateDietMealDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.updateMeal(user.sub, mealId, body, gymId);
  }

  @Delete(':mealId')
  @ApiOperation({ summary: 'Delete meal' })
  @ApiParam({ name: 'mealId', description: 'Diet meal id' })
  @ApiOkResponse({
    description: 'Deletion acknowledged',
    type: DietMutationSuccessSwagger,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Meal not found' })
  remove(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.deleteMeal(user.sub, mealId, gymId);
  }
}
