import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuditModule } from '../audit/audit.module';
import { MessagingModule } from '../messaging/messaging.module';
import { RbacModule } from '../rbac/rbac.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { MembersController } from './members.controller';
import { MembersService } from './members.service';

@Module({
  imports: [SubscriptionsModule, MessagingModule, RbacModule, AuditModule],
  controllers: [MembersController],
  providers: [MembersService, GymAccessService],
  exports: [MembersService],
})
export class MembersModule {}
