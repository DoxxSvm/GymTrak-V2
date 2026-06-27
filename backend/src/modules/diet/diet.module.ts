import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { DietFoodController } from './diet-food.controller';
import { DietController } from './diet.controller';
import { DietService } from './diet.service';

@Module({
  // Register the more specific `/diet/food` routes before `/diet/:mealId`
  // to avoid `food` being captured by the dynamic meal id route.
  controllers: [DietFoodController, DietController],
  providers: [DietService, GymAccessService],
  exports: [DietService],
})
export class DietModule {}
