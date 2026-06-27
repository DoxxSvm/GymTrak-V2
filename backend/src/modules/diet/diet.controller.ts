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
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import {
  DietMealResponseSwagger,
  DietMutationSuccessSwagger,
} from './dto/diet-api.swagger';
import { CreateDietMealDto } from './dto/create-diet-meal.dto';
import { ListDietMealsQueryDto } from './dto/list-diet-meals-query.dto';
import { UpdateDietMealDto } from './dto/update-diet-meal.dto';
import { DietService } from './diet.service';

@ApiTags('Diet')
@ApiBearerAuth()
@Controller('diet')
export class DietController {
  constructor(private readonly diet: DietService) {}

  @Get()
  @ApiOperation({
    summary: 'List meals',
    description:
      'Returns scheduled meals for the gym, newest first (max 200). Optional `member_id` filters to one member’s GymUser id.',
  })
  @ApiQuery({
    name: 'member_id',
    required: false,
    description: 'Filter by member GymUser id',
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
  list(
    @CurrentUser() user: JwtUser,
    @Query() query: ListDietMealsQueryDto,
  ) {
    return this.diet.listMeals(user.sub, query);
  }

  @Post()
  @HttpCode(201)
  @ApiOperation({
    summary: 'Create meal',
    description:
      'Creates a meal for a gym member. `repeat_days`: 0 = Monday … 6 = Sunday. `food_items` may reference `diet_food_id` from `GET /diet/food` or supply `name` + calories/quantity.',
  })
  @ApiCreatedResponse({
    description: 'Created meal with `food_items`',
    type: DietMealResponseSwagger,
  })
  @ApiBadRequestResponse({
    description: 'Invalid payload or unknown `diet_food_id`',
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: '`member_id` not found in this gym' })
  create(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: CreateDietMealDto,
  ) {
    return this.diet.createMeal(user.sub, query.gymId, body);
  }

  @Get(':mealId')
  @ApiOperation({ summary: 'Get meal by id' })
  @ApiParam({ name: 'mealId', description: 'Diet meal id' })
  @ApiOkResponse({
    description: 'Meal with `food_items`',
    type: DietMealResponseSwagger,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Meal not found' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.diet.getMeal(user.sub, query.gymId, mealId);
  }

  @Patch(':mealId')
  @ApiOperation({
    summary: 'Update meal',
    description:
      'Partial update. When `food_items` is sent, it **replaces** all existing lines on the meal.',
  })
  @ApiParam({ name: 'mealId', description: 'Diet meal id' })
  @ApiOkResponse({
    description: 'Updated meal',
    type: DietMealResponseSwagger,
  })
  @ApiBadRequestResponse({
    description: 'No fields to update, or invalid `food_items` / `diet_food_id`',
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Meal not found' })
  update(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateDietMealDto,
  ) {
    return this.diet.updateMeal(user.sub, query.gymId, mealId, body);
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
    @Query() query: GymIdQueryDto,
  ) {
    return this.diet.deleteMeal(user.sub, query.gymId, mealId);
  }
}
