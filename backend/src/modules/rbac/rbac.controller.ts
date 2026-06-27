import {
  BadRequestException,
  Controller,
  Get,
  Param,
  Query,
} from '@nestjs/common';
import { GymRole } from '@prisma/client';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PermissionEngineService } from './permission-engine.service';

@Controller('rbac')
export class RbacController {
  constructor(private readonly engine: PermissionEngineService) {}

  /**
   * Effective permission matrix for the current user at a gym (RBAC + gym feature toggles).
   * Use for bottom tabs / conditional UI; enforce the same rules with `PermissionsGuard` on APIs.
   */
  @Get('effective')
  async effective(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    const result = await this.engine.getEffective(user.sub, query.gymId);
    return {
      ...result,
      effective: this.engine.expandEffectivePermissions(result.effective),
    };
  }

  /** Suggested permission codes when assigning TRAINER or STAFF (UI defaults). */
  @Get('role-defaults/:role')
  roleDefaults(@Param('role') role: string) {
    const r = role.toUpperCase();
    if (r !== GymRole.TRAINER && r !== GymRole.STAFF) {
      throw new BadRequestException('role must be TRAINER or STAFF');
    }
    return this.engine.getRoleDefaults(r as GymRole);
  }
}
