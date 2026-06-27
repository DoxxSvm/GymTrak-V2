import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AuditService } from './audit.service';
import { AuditLogListQueryDto } from './dto/audit-log-list-query.dto';

/**
 * Immutable audit trail listing (admin / owner). Writes happen via {@link AuditService}
 * from domain services (members, billing, plans).
 */
@Controller('audit-logs')
@UseGuards(PermissionsGuard)
@RequirePermissions(PERMISSION_CODES.ADMIN)
export class AuditController {
  constructor(private readonly audit: AuditService) {}

  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: AuditLogListQueryDto) {
    return this.audit.listForGymWithAccess(
      user.sub,
      query.gymId,
      query.limit ?? 30,
      query.cursor,
      query.action,
    );
  }
}
