import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaModule } from '../prisma/prisma.module';
import { RbacModule } from '../rbac/rbac.module';
import { FinanceController } from './finance.controller';
import { FinanceService } from './finance.service';

@Module({
  imports: [PrismaModule, RbacModule],
  controllers: [FinanceController],
  providers: [FinanceService, GymAccessService],
})
export class FinanceModule {}
