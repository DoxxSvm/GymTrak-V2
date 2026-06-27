import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  Post,
  Put,
  Query,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateMeMemberDto } from './dto/update-me-member.dto';
import { UpdateMeOwnerDto } from './dto/update-me-owner.dto';
import { UpdateMeTrainerDto } from './dto/update-me-trainer.dto';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { UpdateUnifiedProfileDto } from './dto/unified-profile.dto';
import { ProfileService } from './profile.service';

const UNIFIED_PROFILE_VALIDATION = new ValidationPipe({
  transform: true,
  whitelist: true,
  forbidNonWhitelisted: false,
});

@ApiTags('Core')
@ApiBearerAuth()
@Controller('profile')
export class ProfileController {
  constructor(private readonly profile: ProfileService) {}

  private requireTokenGymId(user: JwtUser): string {
    const gymId = user.gymId;
    if (!gymId?.trim()) {
      throw new BadRequestException(
        'Access token has no gymId. Complete gym onboarding or sign in again so the token includes your default gym.',
      );
    }
    return gymId.trim();
  }

  /**
   * Role-specific edit-profile payload for the current user at this gym.
   * Query: `gymId` (required). Phone is always read-only in the response.
   */
  @Get('me')
  @ApiOperation({
    summary: 'Current user at gym — edit-profile payload',
    description:
      'Requires gymId. Returns owner, member, or trainer shape; phone is read-only. See OpenAPI `ProfileGetMeResponse`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: true,
    description: 'Active gym context',
  })
  getMe(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.profile.getMe(user.sub, query.gymId);
  }

  /** Member app — name, photo, gender, DOB. Does not change phone. */
  @Put('me/member')
  @ApiOperation({
    summary: 'Update member self-profile',
    description:
      'Name, avatar (avatarUrl or profile_image), gender, dateOfBirth. Phone is not accepted.',
  })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiBody({ type: UpdateMeMemberDto })
  updateMeMember(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMeMemberDto,
  ) {
    return this.profile.updateMeMember(user.sub, query.gymId, body);
  }

  /** Owner app — personal name/photo + gym name, address, GST, logo. Does not change phone. */
  @Put('me/owner')
  @ApiOperation({
    summary: 'Update owner self-profile',
    description:
      'Personal fields plus gymName, gymAddress, gymGstNumber, gymLogoUrl. Requires gym ownership. Phone is not updated.',
  })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiBody({ type: UpdateMeOwnerDto })
  updateMeOwner(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMeOwnerDto,
  ) {
    return this.profile.updateMeOwner(user.sub, query.gymId, body);
  }

  /** Trainer app — name, photo, DOB, gender, experience, address, salary, expertise, shifts. Does not change phone. */
  @Put('me/trainer')
  @ApiOperation({
    summary: 'Update trainer self-profile',
    description:
      'fullName or firstName+lastName, avatar, DOB, gender, experience, address, salary/salaryCents, salaryPeriod, expertise, shifts. Phone/email not accepted here.',
  })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiBody({ type: UpdateMeTrainerDto })
  updateMeTrainer(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMeTrainerDto,
  ) {
    return this.profile.updateMeTrainer(user.sub, query.gymId, body);
  }

  /** Legacy owner summary (first owned gym). Static path before `GET /profile`. */
  @Get('legacy')
  @ApiOperation({
    summary: 'Legacy user profile (first owned gym)',
    description:
      'No `gymId`. Returns flat `name`, `email`, `phone`, `gym_name`, `profile_image`. Prefer unified `GET /profile` (JWT `gymId`).',
  })
  getLegacy(@CurrentUser() user: JwtUser) {
    return this.profile.getProfile(user.sub);
  }

  /** Unified mobile profile (owner or trainer). Gym context: `user.gymId` from access JWT. */
  @Get()
  getUnified(@CurrentUser() user: JwtUser) {
    const gymId = this.requireTokenGymId(user);
    return this.profile.getUnifiedProfile(user.sub, gymId);
  }

  @Put()
  @UsePipes(UNIFIED_PROFILE_VALIDATION)
  @ApiOperation({
    summary: 'Unified profile update (PUT)',
    description:
      '`role` must match your access at the JWT `gymId`. Omit opposing sections (`trainerDetails` for owner, `gymDetails` for trainer). Images: pre-uploaded URLs. GST required when any `gymDetails` field is sent as owner.',
  })
  @ApiBody({ type: UpdateUnifiedProfileDto })
  putUnified(
    @CurrentUser() user: JwtUser,
    @Body() body: UpdateUnifiedProfileDto,
  ) {
    const gymId = this.requireTokenGymId(user);
    return this.profile.updateUnifiedProfile(user.sub, gymId, body);
  }

  @Post()
  @UsePipes(UNIFIED_PROFILE_VALIDATION)
  @ApiOperation({
    summary: 'Unified profile update (POST)',
    description: 'Same body and rules as `PUT /profile` (JWT `gymId`).',
  })
  @ApiBody({ type: UpdateUnifiedProfileDto })
  postUnified(
    @CurrentUser() user: JwtUser,
    @Body() body: UpdateUnifiedProfileDto,
  ) {
    const gymId = this.requireTokenGymId(user);
    return this.profile.updateUnifiedProfile(user.sub, gymId, body);
  }

  @Put('legacy')
  @ApiOperation({
    summary: 'Legacy update profile',
    description:
      'Updates user name/email; `profile_image` sets logo on all owned gyms.',
  })
  @ApiBody({ type: UpdateProfileDto })
  updateLegacy(@CurrentUser() user: JwtUser, @Body() body: UpdateProfileDto) {
    return this.profile.updateProfile(user.sub, body);
  }

  @Delete()
  @ApiOperation({
    summary: 'Delete account (soft)',
    description:
      'Archives owned gyms, deactivates memberships, marks user DELETED.',
  })
  delete(@CurrentUser() user: JwtUser) {
    return this.profile.deleteAccount(user.sub);
  }
}
