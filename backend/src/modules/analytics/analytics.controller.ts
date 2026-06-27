import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import {
  RequireAnyPermission,
  RequirePermissions,
} from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AnalyticsService } from './analytics.service';
import { AnalyticsDateRangeQueryDto } from './dto/analytics-date-range-query.dto';
import { AnalyticsRangeQueryDto } from './dto/analytics-range-query.dto';

/**
 * Aggregated reporting for charts (Flutter / web). Uses indexed SQL group-by;
 * keep ranges ≤ 450 days (see {@link AnalyticsService}).
 */
@Controller('analytics')
@UseGuards(PermissionsGuard)
export class AnalyticsController {
  constructor(private readonly analytics: AnalyticsService) {}

  /** One round-trip: sections depend on RBAC (payments / members / dashboard). */
  @Get('overview')
  @RequireAnyPermission(
    PERMISSION_CODES.DASHBOARD,
    PERMISSION_CODES.PAYMENTS,
    PERMISSION_CODES.MEMBERS,
  )
  overview(
    @CurrentUser() user: JwtUser,
    @Query() query: AnalyticsRangeQueryDto,
  ) {
    return this.analytics.getOverview(
      user.sub,
      query.gymId,
      query.from,
      query.to,
      query.granularity,
    );
  }

  @Get('payments/growth')
  @RequirePermissions(PERMISSION_CODES.PAYMENTS)
  paymentGrowth(
    @CurrentUser() user: JwtUser,
    @Query() query: AnalyticsRangeQueryDto,
  ) {
    return this.analytics.paymentGrowth(
      user.sub,
      query.gymId,
      query.from,
      query.to,
      query.granularity,
    );
  }

  @Get('revenue/monthly')
  @RequirePermissions(PERMISSION_CODES.PAYMENTS)
  monthlyRevenue(
    @CurrentUser() user: JwtUser,
    @Query() query: AnalyticsDateRangeQueryDto,
  ) {
    return this.analytics.monthlyRevenue(
      user.sub,
      query.gymId,
      query.from,
      query.to,
    );
  }

  @Get('members/growth')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  memberGrowth(
    @CurrentUser() user: JwtUser,
    @Query() query: AnalyticsRangeQueryDto,
  ) {
    return this.analytics.memberGrowth(
      user.sub,
      query.gymId,
      query.from,
      query.to,
      query.granularity,
    );
  }

  @Get('attendance/trends')
  @RequirePermissions(PERMISSION_CODES.DASHBOARD)
  attendanceTrends(
    @CurrentUser() user: JwtUser,
    @Query() query: AnalyticsRangeQueryDto,
  ) {
    return this.analytics.attendanceTrends(
      user.sub,
      query.gymId,
      query.from,
      query.to,
      query.granularity,
    );
  }
}
