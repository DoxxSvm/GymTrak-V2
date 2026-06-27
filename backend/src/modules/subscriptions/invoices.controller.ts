import { Controller, Get, Param, Query, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { SubscriptionsService } from './subscriptions.service';

@Controller('invoices')
@UseGuards(PermissionsGuard)
@RequirePermissions(PERMISSION_CODES.PAYMENTS)
export class InvoicesController {
  constructor(private readonly subscriptions: SubscriptionsService) {}

  @Get(':invoiceId')
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('invoiceId') invoiceId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.subscriptions.getInvoice(user.sub, query.gymId, invoiceId);
  }
}
