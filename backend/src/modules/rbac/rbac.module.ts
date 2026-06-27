import { Module } from '@nestjs/common';
import { GymFeaturesModule } from '../gym-features/gym-features.module';
import { PrismaModule } from '../prisma/prisma.module';
import { PermissionEngineService } from './permission-engine.service';
import { RbacController } from './rbac.controller';
import { PermissionsGuard } from '../../common/guards/permissions.guard';

@Module({
  imports: [PrismaModule, GymFeaturesModule],
  controllers: [RbacController],
  providers: [PermissionEngineService, PermissionsGuard],
  exports: [PermissionEngineService, PermissionsGuard],
})
export class RbacModule {}
