import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuditModule } from '../audit/audit.module';
import { MessagingModule } from '../messaging/messaging.module';
import { PrismaModule } from '../prisma/prisma.module';
import { RbacModule } from '../rbac/rbac.module';
import { InvoicesController } from './invoices.controller';
import { PaymentsController } from './payments.controller';
import { SubscriptionsController } from './subscriptions.controller';
import { SubscriptionsService } from './subscriptions.service';

@Module({
  imports: [PrismaModule, MessagingModule, RbacModule, AuditModule],
  controllers: [
    SubscriptionsController,
    PaymentsController,
    InvoicesController,
  ],
  providers: [SubscriptionsService, GymAccessService],
  exports: [SubscriptionsService],
})
export class SubscriptionsModule {}
