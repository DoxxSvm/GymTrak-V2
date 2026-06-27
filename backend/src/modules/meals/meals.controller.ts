import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  Put,
  Query,
  UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AddFoodItemDto } from './dto/add-food-item.dto';
import { CreateMealDto } from './dto/create-meal.dto';
import { UpdateFoodItemDto } from './dto/update-food-item.dto';
import { UpdateMealDto } from './dto/update-meal.dto';
import { MealsService } from './meals.service';

@Controller()
@UseGuards(PermissionsGuard)
@RequirePermissions(PERMISSION_CODES.MEMBERS)
export class MealsController {
  constructor(private readonly meals: MealsService) {}

  @Post('meals')
  createMeal(@CurrentUser() user: JwtUser, @Body() body: CreateMealDto) {
    return this.meals.createMeal(user.sub, body);
  }

  @Get('members/:memberId/meals')
  memberMeals(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
  ) {
    return this.meals.listMealsByMember(user.sub, memberId);
  }

  @Get('meals/:mealId')
  mealDetails(@CurrentUser() user: JwtUser, @Param('mealId') mealId: string) {
    return this.meals.getMeal(user.sub, mealId);
  }

  @Post('meals/:mealId/foods')
  addFood(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Body() body: AddFoodItemDto,
  ) {
    return this.meals.addFoodItem(user.sub, mealId, body);
  }

  @Put('foods/:foodId')
  updateFood(
    @CurrentUser() user: JwtUser,
    @Param('foodId') foodId: string,
    @Body() body: UpdateFoodItemDto,
  ) {
    return this.meals.updateFoodItem(user.sub, foodId, body);
  }

  @Delete('foods/:foodId')
  deleteFood(@CurrentUser() user: JwtUser, @Param('foodId') foodId: string) {
    return this.meals.deleteFoodItem(user.sub, foodId);
  }

  @Put('meals/:mealId')
  updateMeal(
    @CurrentUser() user: JwtUser,
    @Param('mealId') mealId: string,
    @Body() body: UpdateMealDto,
  ) {
    return this.meals.updateMeal(user.sub, mealId, body);
  }

  @Delete('meals/:mealId')
  deleteMeal(@CurrentUser() user: JwtUser, @Param('mealId') mealId: string) {
    return this.meals.deleteMeal(user.sub, mealId);
  }

  @Get('foods/search')
  search(@CurrentUser() user: JwtUser, @Query('search') search?: string) {
    return this.meals.searchFoods(user.sub, search ?? '');
  }
}
