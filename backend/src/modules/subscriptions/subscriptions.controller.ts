import {
  Body,
  Controller,
  Get,
  Param,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import {
  RequireAnyPermission,
  RequirePermissions,
} from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CancelSubscriptionDto } from './dto/cancel-subscription.dto';
import { ExtendSubscriptionDto } from './dto/extend-subscription.dto';
import { FreezeSubscriptionDto } from './dto/freeze-subscription.dto';
import { RenewSubscriptionDto } from './dto/renew-subscription.dto';
import { SubscriptionListQueryDto } from './dto/subscription-list-query.dto';
import { UpgradeSubscriptionDto } from './dto/upgrade-subscription.dto';
import { SubscriptionsService } from './subscriptions.service';
import { CreateSubscriptionCompatDto } from './dto/create-subscription-compat.dto';

@Controller('subscriptions')
// @UseGuards(PermissionsGuard)
export class SubscriptionsController {
  constructor(private readonly subscriptions: SubscriptionsService) {}

  @Get()
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  list(@CurrentUser() user: JwtUser, @Query() query: SubscriptionListQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.subscriptions.listForGym(
      user.sub,
      query.gymId,
      query.tab,
      limit,
      offset,
      query.q,
    );
  }

  @Get(':subscriptionId')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.subscriptions.getDetail(user.sub, query.gymId, subscriptionId);
  }

  /** Owner app compatibility: create subscription from member/plan payload. */
  @Post()
  createCompat(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateSubscriptionCompatDto,
  ) {
    return this.subscriptions.createSubscriptionCompat(user.sub, body);
  }

  @Post(':subscriptionId/renew')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  renew(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: RenewSubscriptionDto,
  ) {
    return this.subscriptions.renew(
      user.sub,
      query.gymId,
      subscriptionId,
      body,
    );
  }

  @Post(':subscriptionId/extend')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  extend(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: ExtendSubscriptionDto,
  ) {
    return this.subscriptions.extend(
      user.sub,
      query.gymId,
      subscriptionId,
      body,
    );
  }

  @Post(':subscriptionId/upgrade')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  upgrade(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpgradeSubscriptionDto,
  ) {
    return this.subscriptions.upgrade(
      user.sub,
      query.gymId,
      subscriptionId,
      body,
    );
  }

  @Post(':subscriptionId/freeze')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  freeze(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: FreezeSubscriptionDto,
  ) {
    return this.subscriptions.freeze(
      user.sub,
      query.gymId,
      subscriptionId,
      body,
    );
  }

  @Post(':subscriptionId/unfreeze')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  unfreeze(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.subscriptions.unfreeze(user.sub, query.gymId, subscriptionId);
  }

  @Post(':subscriptionId/cancel')
  // @RequirePermissions(PERMISSION_CODES.MEMBERS)
  cancel(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: CancelSubscriptionDto,
  ) {
    return this.subscriptions.cancel(
      user.sub,
      query.gymId,
      subscriptionId,
      body,
    );
  }

  @Post(':subscriptionId/invoice')
  @RequireAnyPermission(PERMISSION_CODES.MEMBERS, PERMISSION_CODES.PAYMENTS)
  issueInvoice(
    @CurrentUser() user: JwtUser,
    @Param('subscriptionId') subscriptionId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.subscriptions.generateInvoice(
      user.sub,
      query.gymId,
      subscriptionId,
    );
  }
}
