import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { FinanceService } from './finance.service';

@Controller('finance')
@UseGuards(PermissionsGuard)
@RequirePermissions(PERMISSION_CODES.PAYMENTS)
export class FinanceController {
  constructor(private readonly finance: FinanceService) {}

  @Get('summary')
  summary(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.finance.summary(user.sub, query.gymId);
  }
}
