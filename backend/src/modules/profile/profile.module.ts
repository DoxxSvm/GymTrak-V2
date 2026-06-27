import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { GymProfileModule } from '../gym-profile/gym-profile.module';
import { PrismaModule } from '../prisma/prisma.module';
import { TrainersModule } from '../trainers/trainers.module';
import { ProfileController } from './profile.controller';
import { ProfileService } from './profile.service';

@Module({
  imports: [PrismaModule, TrainersModule, GymProfileModule],
  controllers: [ProfileController],
  providers: [ProfileService, GymAccessService],
})
export class ProfileModule {}
