import { Body, Controller, Post } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { ChooseRoleDto } from './dto/choose-role.dto';
import { MemberOnboardingDto } from './dto/member-onboarding.dto';
import { OwnerOnboardingDto } from './dto/owner-onboarding.dto';
import { OnboardingService } from './onboarding.service';

@Controller('onboarding')
export class OnboardingController {
  constructor(private readonly onboarding: OnboardingService) {}

  @Post('choose-role')
  chooseRole(@CurrentUser() jwt: JwtUser, @Body() dto: ChooseRoleDto) {
    return this.onboarding.chooseRole(jwt.sub, dto.role);
  }

  @Post('owner')
  completeOwner(@CurrentUser() jwt: JwtUser, @Body() dto: OwnerOnboardingDto) {
    return this.onboarding.completeOwner(jwt.sub, dto.gymName);
  }

  @Post('member')
  completeMember(
    @CurrentUser() jwt: JwtUser,
    @Body() dto: MemberOnboardingDto,
  ) {
    return this.onboarding.completeMember(jwt.sub, dto);
  }
}
