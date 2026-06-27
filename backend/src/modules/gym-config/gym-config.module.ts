import { Module } from '@nestjs/common';
import { GymConfigController } from './gym-config.controller';
import { GymConfigService } from './gym-config.service';

@Module({
  controllers: [GymConfigController],
  providers: [GymConfigService],
})
export class GymConfigModule {}
