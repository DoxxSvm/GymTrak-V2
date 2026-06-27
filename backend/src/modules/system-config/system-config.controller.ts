import { Body, Controller, Get, Patch, Query } from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateSystemConfigDto } from './dto/update-system-config.dto';
import { SystemConfigService } from './system-config.service';

/**
 * Per-gym system settings (currency, GST, default plan presets). Multi-tenant: always scope with `gymId`.
 */
@Controller('system-config')
export class SystemConfigController {
  constructor(private readonly systemConfig: SystemConfigService) {}

  /** Staff with gym access can read (POS / UI defaults). */
  @Get()
  get(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.systemConfig.get(user.sub, query.gymId);
  }

  /** Owner or platform super-admin only (matches gym feature toggles). */
  @Patch()
  update(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateSystemConfigDto,
  ) {
    return this.systemConfig.update(user.sub, query.gymId, body);
  }
}
