import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { GymSaasController } from './gym-saas.controller';
import { SaasEntitlementsService } from './saas-entitlements.service';

@Module({
  controllers: [GymSaasController],
  providers: [SaasEntitlementsService, GymAccessService],
  exports: [SaasEntitlementsService],
})
export class SaasModule {}
