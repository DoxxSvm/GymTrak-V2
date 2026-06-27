import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Put,
  Query,
  UseGuards,
} from '@nestjs/common';
import { GlobalRole } from '@prisma/client';
import { RequireGlobalRoles } from '../common/decorators/roles.decorator';
import { GlobalRolesGuard } from '../common/guards/global-roles.guard';
import { PlatformGymsQueryDto } from './dto/platform-gyms-query.dto';
import { SetPlatformGymStatusDto } from './dto/set-platform-gym-status.dto';
import { UpsertPlatformGymSubscriptionDto } from './dto/upsert-platform-gym-subscription.dto';
import { PlatformAdminService } from './platform-admin.service';

/**
 * Platform operator APIs — global scope, no `X-Gym-Id`.
 * Guarded by {@link GlobalRole.SUPER_ADMIN} only (separate from gym RBAC).
 */
@Controller('platform')
@UseGuards(GlobalRolesGuard)
@RequireGlobalRoles(GlobalRole.SUPER_ADMIN)
export class PlatformAdminController {
  constructor(private readonly platformAdmin: PlatformAdminService) {}

  @Get('gyms')
  listGyms(@Query() query: PlatformGymsQueryDto) {
    return this.platformAdmin.listGyms(query);
  }

  @Get('gyms/:gymId')
  getGym(@Param('gymId') gymId: string) {
    return this.platformAdmin.getGym(gymId);
  }

  @Patch('gyms/:gymId/status')
  setGymStatus(
    @Param('gymId') gymId: string,
    @Body() body: SetPlatformGymStatusDto,
  ) {
    return this.platformAdmin.setGymStatus(gymId, body.status);
  }

  @Get('gyms/:gymId/subscription')
  getGymSubscription(@Param('gymId') gymId: string) {
    return this.platformAdmin.getGymSubscription(gymId);
  }

  @Put('gyms/:gymId/subscription')
  upsertGymSubscription(
    @Param('gymId') gymId: string,
    @Body() body: UpsertPlatformGymSubscriptionDto,
  ) {
    return this.platformAdmin.upsertGymSubscription(gymId, body);
  }

  @Get('revenue/overview')
  revenueOverview() {
    return this.platformAdmin.revenueOverview();
  }

  /** Assignable GymTrak SaaS catalog rows (`SubscriptionPlan` with `saasTier`). */
  @Get('saas-plans')
  listSaasPlans() {
    return this.platformAdmin.listSaasPlans();
  }
}
