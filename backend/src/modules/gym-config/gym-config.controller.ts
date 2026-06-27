import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateGymConfigDto } from './dto/update-gym-config.dto';
import { GymConfigService } from './gym-config.service';

/**
 * Legacy path-style gym config used by the Flutter owner app (`/gyms/:gymId/config`).
 * Backed by `GymSystemConfig` (same data as `/system-config?gymId=` but JSON field names match the mobile client).
 */
@Controller('gyms')
export class GymConfigController {
  constructor(private readonly gymConfig: GymConfigService) {}

  @Get(':gymId/config')
  get(@Param('gymId') gymId: string, @CurrentUser() user: JwtUser) {
    this.ensureGymId(gymId);
    return this.gymConfig.get(gymId, user.sub);
  }

  @Patch(':gymId/config')
  patch(
    @Param('gymId') gymId: string,
    @CurrentUser() user: JwtUser,
    @Body() body: UpdateGymConfigDto,
  ) {
    this.ensureGymId(gymId);
    return this.gymConfig.patch(gymId, user.sub, body);
  }

  /** Same semantics as PATCH (upsert / partial update). */
  @Post(':gymId/config')
  post(
    @Param('gymId') gymId: string,
    @CurrentUser() user: JwtUser,
    @Body() body: UpdateGymConfigDto,
  ) {
    this.ensureGymId(gymId);
    return this.gymConfig.patch(gymId, user.sub, body);
  }

  private ensureGymId(gymId: string): void {
    if (!gymId?.trim()) {
      throw new BadRequestException('Invalid gym id');
    }
  }
}
