import { Body, Controller, Get, Patch, Post, Put, Query } from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateGymProfileDto } from './dto/update-gym-profile.dto';
import { GymProfileService } from './gym-profile.service';

@Controller('gym-profile')
export class GymProfileController {
  constructor(private readonly gymProfile: GymProfileService) {}

  @Get()
  get(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.gymProfile.get(user.sub, query.gymId);
  }

  @Patch()
  update(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateGymProfileDto,
  ) {
    return this.gymProfile.update(user.sub, query.gymId, body);
  }

  @Put()
  replace(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateGymProfileDto,
  ) {
    return this.gymProfile.update(user.sub, query.gymId, body);
  }

  @Put('/compat')
  updateCompat(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body()
    body: {
      gym_name?: string;
      owner_name?: string;
      address?: string;
      gst_number?: string;
      logo_url?: string;
    },
  ) {
    return this.gymProfile.update(user.sub, query.gymId, {
      address: body.address,
      gstin: body.gst_number,
      logoUrl: body.logo_url,
    });
  }

  @Post('/location')
  setLocation(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: { latitude: number; longitude: number; address?: string },
  ) {
    return this.gymProfile.update(user.sub, query.gymId, {
      latitude: body.latitude,
      longitude: body.longitude,
      address: body.address,
    });
  }

  @Post('/location/confirm')
  async confirmLocation(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
  ) {
    const gym = await this.gymProfile.get(user.sub, query.gymId);
    return {
      confirmed: true,
      latitude: gym.latitude,
      longitude: gym.longitude,
      address: gym.address,
    };
  }
}
