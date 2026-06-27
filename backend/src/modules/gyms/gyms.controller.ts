import { Body, Controller, Get, Post } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { AuthService } from '../auth/auth.service';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateGymDto } from './dto/create-gym.dto';
import { GymsService } from './gyms.service';

@Controller('gyms')
export class GymsController {
  constructor(
    private readonly gyms: GymsService,
    private readonly auth: AuthService,
  ) {}

  /** All gyms the user can switch to (owner, staff, trainer, member). */
  @Get()
  list(@CurrentUser() user: JwtUser) {
    return this.gyms.listForUser(user.sub);
  }

  /**
   * Create another owned gym, or the first gym during owner onboarding.
   * Data stays isolated by `gym_id` on all related tables; use `X-Gym-Id` on requests.
   */
  @Post()
  async create(@CurrentUser() user: JwtUser, @Body() body: CreateGymDto) {
    const { userId, wasTemp } = await this.auth.ensureUserForOwnerSignup(user);
    const gym = await this.gyms.create(userId, body);
    if (!wasTemp) {
      return gym;
    }
    const row = await this.auth.getUserForTokenPair(userId);
    const tokens = await this.auth.issueTokenPair(row);
    return {
      ...gym,
      success: true as const,
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
    };
  }
}
