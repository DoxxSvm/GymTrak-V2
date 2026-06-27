import {
  BadRequestException,
  Controller,
  Get,
  Query,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { DashboardService } from './dashboard.service';

@Controller('dashboard')
export class DashboardController {
  constructor(private readonly dashboard: DashboardService) {}

  @Get('owner')
  owner(@CurrentUser() user: JwtUser, @Query('gymId') gymId?: string) {
    return this.dashboard.getOwnerDashboard(user.sub, gymId);
  }

  @Get()
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