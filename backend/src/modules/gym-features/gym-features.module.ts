import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { GymFeaturesController } from './gym-features.controller';
import { GymFeaturesService } from './gym-features.service';

@Module({
  controllers: [GymFeaturesController],
  providers: [GymFeaturesService, GymAccessService],
  exports: [GymFeaturesService],
})
export class GymFeaturesModule {}
