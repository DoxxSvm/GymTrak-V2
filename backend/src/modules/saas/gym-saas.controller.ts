import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymAccessService } from '../../common/services/gym-access.service';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { SaasEntitlementsService } from './saas-entitlements.service';

/**
 * GymTrak SaaS: tier + feature flags for client gating (Basic / Plus / Premium).
 */
@Controller('gym-saas')
export class GymSaasController {
  constructor(
    private readonly saas: SaasEntitlementsService,
    private readonly gymAccess: GymAccessService,
  ) {}

  @Get('entitlements')
  async entitlements(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
  ) {
    await this.gymAccess.assertCanManageGym(user.sub, query.gymId);
    return this.saas.getEntitlements(query.gymId, user.sub);
  }

  @Get('/plans')
  plans() {
    return this.saas.listSaasPlans();
  }

  @Post('/custom-subscription-plans')
  createCustomPlan(
    @Body()
    body: {
      plan_name: string;
      billing_cycle: string;
      price: number;
      features?: string[];
    },
  ) {
    return this.saas.createCustomPlan(body);
  }
}
