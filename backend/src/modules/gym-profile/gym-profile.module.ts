import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaModule } from '../prisma/prisma.module';
import { GymProfileController } from './gym-profile.controller';
import { GymProfileService } from './gym-profile.service';

@Module({
  imports: [PrismaModule],
  controllers: [GymProfileController],
  providers: [GymProfileService, GymAccessService],
  exports: [GymProfileService],
})
export class GymProfileModule {}
