import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuditModule } from '../audit/audit.module';
import { DietModule } from '../diet/diet.module';
import { MessagingModule } from '../messaging/messaging.module';
import { RbacModule } from '../rbac/rbac.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { WorkoutsModule } from '../workouts/workouts.module';
import { MemberPersonalCatalogService } from './member-personal-catalog.service';
import { MemberStatisticsService } from './member-statistics.service';
import { MembersController } from './members.controller';
import { MembersService } from './members.service';

@Module({
  imports: [
    SubscriptionsModule,
    MessagingModule,
    RbacModule,
    AuditModule,
    DietModule,
    WorkoutsModule,
  ],
  controllers: [MembersController],
  providers: [
    MembersService,
    MemberPersonalCatalogService,
    MemberStatisticsService,
    GymAccessService,
  ],
  exports: [MembersService],
})
export class MembersModule {}
