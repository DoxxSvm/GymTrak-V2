import { Body, Controller, Get, Post, UseGuards } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';
import { Throttle, ThrottlerGuard } from '@nestjs/throttler';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { SwitchToMemberDto } from './dto/switch-to-member.dto';
import { OwnerAppService } from './owner-app.service';

@ApiTags('Owner signup')
@ApiBearerAuth()
@UseGuards(ThrottlerGuard)
@Throttle({ default: { limit: 30, ttl: 60_000 } })
@Controller('owner')
export class OwnerSwitchController {
  constructor(private readonly ownerApp: OwnerAppService) { }


  @Get('profile-status')
  @ApiOperation({
    summary: 'Owner dual persona: switch eligibility',
    description:
      '`isSwitcheable` is true only when both member_profiles and owner_profiles exist; then includes User.lastActiveRole (OWNER | MEMBER).',
  })
  profileStatus(@CurrentUser() user: JwtUser) {
    return this.ownerApp.getProfileStatus(user);
  }

  @Post('switch-to-member')
  @ApiOperation({
    summary: 'Owner persona → member persona',
    description:
      'Gym owners only; same mobile/User as owner. First call creates `member_profiles`, upserts `owner_profiles`, adds `GymUser` with role MEMBER at JWT-owned gym or first owned gym, syncs User physique fields and lastActiveRole, syncs membershipEndsAt for that MEMBER row, returns tokens with activeAppRole. Subsequent calls ensure MEMBER GymUser and switch lastActiveRole.',
  })
  @ApiBody({
    type: SwitchToMemberDto,
    required: false,
    description:
      'Required fields only when no member_profiles row exists yet (see OpenAPI OwnerSwitchToMemberRequest).',
  })
  switchToMember(@CurrentUser() user: JwtUser, @Body() dto: SwitchToMemberDto) {
    return this.ownerApp.switchToMember(user, dto);
  }

  @Post('switch-to-owner')
  @ApiOperation({
    summary: 'Member persona → owner persona',
    description:
      'Sets lastActiveRole OWNER; MEMBER GymUser row remains. Returns fresh tokens with activeAppRole owner.',
  })
  switchToOwner(@CurrentUser() user: JwtUser) {
    return this.ownerApp.switchToOwner(user);
  }
}
