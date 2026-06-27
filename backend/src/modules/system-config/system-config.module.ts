import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { SystemConfigController } from './system-config.controller';
import { SystemConfigService } from './system-config.service';

@Module({
  controllers: [SystemConfigController],
  providers: [SystemConfigService, GymAccessService],
  exports: [SystemConfigService],
})
export class SystemConfigModule {}
