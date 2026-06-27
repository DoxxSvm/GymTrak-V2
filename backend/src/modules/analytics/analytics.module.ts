import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { RbacModule } from '../rbac/rbac.module';
import { PrismaModule } from '../prisma/prisma.module';
import { AnalyticsController } from './analytics.controller';
import { AnalyticsService } from './analytics.service';

@Module({
  imports: [PrismaModule, RbacModule],
  controllers: [AnalyticsController],
  providers: [AnalyticsService, GymAccessService],
  exports: [AnalyticsService],
})
export class AnalyticsModule {}
