import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuthModule } from '../auth/auth.module';
import { GymsModule } from '../gyms/gyms.module';
import { OnboardingModule } from '../onboarding/onboarding.module';
import { PrismaModule } from '../prisma/prisma.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { OwnerAppController } from './owner-app.controller';
import { OwnerAppService } from './owner-app.service';
import { OwnerSwitchController } from './owner-switch.controller';

@Module({
  imports: [
    PrismaModule,
    GymsModule,
    AuthModule,
    OnboardingModule,
    SubscriptionsModule,
  ],
  controllers: [OwnerAppController, OwnerSwitchController],
  providers: [OwnerAppService, GymAccessService],
})
export class OwnerAppModule {}
