import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { TrainerWorkoutsController } from './trainer-workouts.controller';
import { WorkoutsController } from './workouts.controller';
import { WorkoutsService } from './workouts.service';

@Module({
  controllers: [WorkoutsController, TrainerWorkoutsController],
  providers: [WorkoutsService, GymAccessService],
  exports: [WorkoutsService],
})
export class WorkoutsModule {}
