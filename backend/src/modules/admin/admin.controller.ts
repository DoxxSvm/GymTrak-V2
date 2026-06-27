import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Query,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { Throttle, ThrottlerGuard } from '@nestjs/throttler';
import { AdminGuard } from '../../common/guards/admin.guard';
import { UpsertPlatformGymSubscriptionDto } from '../../platform/dto/upsert-platform-gym-subscription.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AdminService } from './admin.service';
import { AdminActivityQueryDto } from './dto/admin-activity-query.dto';
import { AdminGymsQueryDto } from './dto/admin-gyms-query.dto';
import { AdminSubscriptionsQueryDto } from './dto/admin-subscriptions-query.dto';
import { AdminUsersQueryDto } from './dto/admin-users-query.dto';
import { AdminSuccessInterceptor } from './interceptors/admin-success.interceptor';

@Controller('admin')
@UseGuards(AdminGuard, ThrottlerGuard)
@Throttle({ default: { limit: 120, ttl: 60_000 } })
@UseInterceptors(AdminSuccessInterceptor)
export class AdminController {
  constructor(private readonly admin: AdminService) {}

  /** Lightweight identity for admin UI routing (SUPER_ADMIN only). */
  @Get('me')
  me(@CurrentUser() user: JwtUser) {
    return this.admin.me(user.sub);
  }

  @Get('gyms')
  listGyms(@Query() query: AdminGymsQueryDto) {
    return this.admin.listGyms(query);
  }

  @Get('gyms/:gymId')
  getGym(@Param('gymId') gymId: string) {
    return this.admin.getGymDetail(gymId);
  }

  @Patch('gyms/:gymId/block')
  blockGym(@Param('gymId') gymId: string) {
    return this.admin.blockGym(gymId);
  }

  @Patch('gyms/:gymId/unblock')
  unblockGym(@Param('gymId') gymId: string) {
    return this.admin.unblockGym(gymId);
  }

  @Get('users')
  listUsers(@Query() query: AdminUsersQueryDto) {
    return this.admin.listUsers(query);
  }

  @Get('users/:userId')
  getUser(@Param('userId') userId: string) {
    return this.admin.getUserDetail(userId);
  }

  @Get('analytics')
  analytics() {
    return this.admin.getAnalytics();
  }

  @Get('subscriptions')
  listSubscriptions(@Query() query: AdminSubscriptionsQueryDto) {
    return this.admin.listSubscriptions(query);
  }

  @Patch('subscriptions/:gymId')
  patchSubscription(
    @Param('gymId') gymId: string,
    @Body() body: UpsertPlatformGymSubscriptionDto,
  ) {
    return this.admin.patchSubscription(gymId, body);
  }

  /** Cross-tenant audit trail (system activity). */
  @Get('activity')
  activity(@Query() query: AdminActivityQueryDto) {
    return this.admin.activity(query);
  }

  /** SaaS catalog for subscription PATCH bodies. */
  @Get('saas-plans')
  saasPlans() {
    return this.admin.listSaasPlans();
  }
}
