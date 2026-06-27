import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { RbacModule } from '../rbac/rbac.module';
import { MealsController } from './meals.controller';
import { MealsService } from './meals.service';

@Module({
  imports: [RbacModule],
  controllers: [MealsController],
  providers: [MealsService, GymAccessService],
})
export class MealsModule {}
