import { Body, Controller, Get, Post, Query, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PaymentAnalyticsQueryDto } from './dto/payment-analytics-query.dto';
import { PaymentListQueryDto } from './dto/payment-list-query.dto';
import { SubscriptionsService } from './subscriptions.service';
import { ReceiveGymPaymentDto } from './dto/receive-gym-payment.dto';

@Controller('payments')
@UseGuards(PermissionsGuard)
@RequirePermissions(PERMISSION_CODES.PAYMENTS)
export class PaymentsController {
  constructor(private readonly subscriptions: SubscriptionsService) {}

  /** Daily series + list for charts (completed payments in range) */
  @Get('analytics')
  analytics(
    @CurrentUser() user: JwtUser,
    @Query() query: PaymentAnalyticsQueryDto,
  ) {
    const range = query.range ?? 'monthly';
    const now = new Date();
    const from =
      query.from ??
      (range === 'weekly'
        ? new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString()
        : range === 'yearly'
          ? new Date(Date.UTC(now.getUTCFullYear(), 0, 1)).toISOString()
          : new Date(
              Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1),
            ).toISOString());
    const to = query.to ?? now.toISOString();
    return this.subscriptions.paymentAnalytics(
      user.sub,
      query.gymId,
      from,
      to,
      range,
    );
  }

  /** Gym-wide payment history; filter by status (e.g. PENDING) or member */
  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: PaymentListQueryDto) {
    const limit = query.limit ?? 20;
    const page = query.page ?? 1;
    const offset = query.offset ?? (page - 1) * limit;
    return this.subscriptions.listPaymentsForGym(
      user.sub,
      query.gymId,
      query.status,
      query.memberId,
      limit,
      offset,
      query.search,
    );
  }

  /** Compatibility endpoint for owner app: receive payment by member/subscription ids. */
  @Post()
  receive(@CurrentUser() user: JwtUser, @Body() body: ReceiveGymPaymentDto) {
    return this.subscriptions.receivePaymentByMember(user.sub, body);
  }
}
