import {
  BadRequestException,
  Controller,
  Get,
  Query,
  UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { DashboardService } from './dashboard.service';

@Controller('dashboard')
export class DashboardController {
  constructor(private readonly dashboard: DashboardService) {}

  /**
   * Single aggregated payload for owner home — filter by gym via `gymId` (defaults to first gym).
   */
  @Get('owner')
  owner(@CurrentUser() user: JwtUser, @Query('gymId') gymId?: string) {
    return this.dashboard.getOwnerDashboard(user.sub, gymId);
  }

  /**
   * Staff / owner unified dashboard. Requires `dashboard:access` (or gym owner bypass).
   * Prefer this for trainer & staff apps; use `GET dashboard/owner` for legacy owner-only clients.
   */
  @Get()
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.DASHBOARD)
  summary(
    @CurrentUser() user: JwtUser,
    @Query('gymId') gymId?: string,
    @Query('mobileView') mobileView?: string,
  ) {
    if (mobileView === 'owner') {
      return this.dashboard.getOwnerHomeDashboard(user.sub, gymId);
    }
    if (!gymId?.trim()) {
      throw new BadRequestException('gymId is required');
    }
    return this.dashboard.getDashboardForUser(user.sub, gymId);
  }
}
