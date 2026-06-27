import { Module } from '@nestjs/common';
import { PlatformAdminController } from './platform-admin.controller';
import { PlatformAdminService } from './platform-admin.service';

/**
 * Super-admin / platform-operator surface: routes under `/api/v1/platform/*`,
 * isolated from gym-scoped modules and RBAC.
 */
@Module({
  controllers: [PlatformAdminController],
  providers: [PlatformAdminService],
  exports: [PlatformAdminService],
})
export class PlatformModule {}
