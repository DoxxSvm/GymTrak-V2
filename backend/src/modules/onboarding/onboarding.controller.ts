import { Body, Controller, Post } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { MemberOnboardingDto } from './dto/member-onboarding.dto';
import { OwnerOnboardingDto } from './dto/owner-onboarding.dto';
import { OnboardingService } from './onboarding.service';

@ApiTags('Onboarding')
@ApiBearerAuth()
@Controller('onboarding')
export class OnboardingController {
  constructor(private readonly onboarding: OnboardingService) {}

  @Post('owner')
  @ApiOperation({ summary: 'Finish owner onboarding (create gym)' })
  completeOwner(@CurrentUser() jwt: JwtUser, @Body() dto: OwnerOnboardingDto) {
    return this.onboarding.completeOwner(jwt.sub, dto.gymName);
  }

  @Post('member')
  @ApiOperation({
    summary: 'Finish member profile (no gym / trainer / plan)',
    description:
      'Requires `POST /user/select-role` with `member` first. Returns `wellness` (BMI + maintenance kcal when age, gender, activityLevel are sent). ' +
      'If the user has active gym memberships, enqueues **Onboarding Welcome** WhatsApp per gym (when template enabled).',
  })
  @ApiBody({
    type: MemberOnboardingDto,
    examples: {
      default: {
        summary: 'Full profile (name + metrics + wellness inputs)',
        value: {
          fullName: 'Rahul',
          heightCm: 175,
          weightKg: 72,
          ageYears: 28,
          gender: 'MALE',
          activityLevel: 'HIGH',
          fitnessGoal: 'STAY_FIT',
        },
      },
    },
  })
  completeMember(
    @CurrentUser() jwt: JwtUser,
    @Body() dto: MemberOnboardingDto,
  ) {
    return this.onboarding.completeMember(jwt.sub, dto);
  }
}
