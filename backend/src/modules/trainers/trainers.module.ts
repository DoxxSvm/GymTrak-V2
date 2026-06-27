import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuditModule } from '../audit/audit.module';
import { GymFeaturesModule } from '../gym-features/gym-features.module';
import { RbacModule } from '../rbac/rbac.module';
import { TrainersController } from './trainers.controller';
import { TrainersService } from './trainers.service';

@Module({
  imports: [GymFeaturesModule, RbacModule, AuditModule],
  controllers: [TrainersController],
  providers: [TrainersService, GymAccessService],
  exports: [TrainersService],
})
export class TrainersModule {}
