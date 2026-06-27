import { Body, Controller, Get, Patch, Query } from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { SetGymFeatureDto } from './dto/set-gym-feature.dto';
import { GymFeaturesService } from './gym-features.service';

@Controller('gym-features')
export class GymFeaturesController {
  constructor(private readonly features: GymFeaturesService) {}

  /** Effective feature map for a gym (merged with defaults). */
  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.features.listEffective(user.sub, query.gymId);
  }

  /** Owner/super-admin: toggle one feature key. */
  @Patch()
  set(@CurrentUser() user: JwtUser, @Body() body: SetGymFeatureDto) {
    return this.features.set(user.sub, body.gymId, body.key, body.enabled);
  }
}
