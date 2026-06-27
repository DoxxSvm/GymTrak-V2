import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaModule } from '../prisma/prisma.module';
import { BroadcastController } from './broadcast.controller';
import { BroadcastService } from './broadcast.service';

@Module({
  imports: [PrismaModule],
  controllers: [BroadcastController],
  providers: [BroadcastService, GymAccessService],
  exports: [BroadcastService],
})
export class BroadcastModule {}
