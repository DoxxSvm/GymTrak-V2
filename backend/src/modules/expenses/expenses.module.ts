import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuditModule } from '../audit/audit.module';
import { RbacModule } from '../rbac/rbac.module';
import { SaasModule } from '../saas/saas.module';
import { ExpensesController } from './expenses.controller';
import { ExpensesService } from './expenses.service';

@Module({
  imports: [SaasModule, AuditModule, RbacModule],
  controllers: [ExpensesController],
  providers: [ExpensesService, GymAccessService],
})
export class ExpensesModule {}
