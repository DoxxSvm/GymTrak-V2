import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaModule } from '../prisma/prisma.module';
import { RbacModule } from '../rbac/rbac.module';
import { TrainerLeavesController } from './trainer-leaves.controller';
import { TrainerLeavesService } from './trainer-leaves.service';

@Module({
  imports: [PrismaModule, RbacModule],
  controllers: [TrainerLeavesController],
  providers: [TrainerLeavesService, GymAccessService],
})
export class TrainerLeavesModule {}
