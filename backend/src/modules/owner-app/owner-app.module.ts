import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuthModule } from '../auth/auth.module';
import { GymsModule } from '../gyms/gyms.module';
import { PrismaModule } from '../prisma/prisma.module';
import { OwnerAppController } from './owner-app.controller';
import { OwnerAppService } from './owner-app.service';

@Module({
  imports: [PrismaModule, GymsModule, AuthModule],
  controllers: [OwnerAppController],
  providers: [OwnerAppService, GymAccessService],
})
export class OwnerAppModule {}
