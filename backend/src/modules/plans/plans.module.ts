import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { RbacModule } from '../rbac/rbac.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { PlanCompatController } from './plan-compat.controller';
import { PlansController } from './plans.controller';
import { PlansService } from './plans.service';

@Module({
  imports: [RbacModule, SubscriptionsModule],
  controllers: [PlansController, PlanCompatController],
  providers: [PlansService, GymAccessService],
  exports: [PlansService],
})
export class PlansModule {}
