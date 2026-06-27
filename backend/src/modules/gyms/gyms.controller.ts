import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBearerAuth,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiTags,
  ApiUnauthorizedResponse,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { AuthService } from '../auth/auth.service';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateGymDto } from './dto/create-gym.dto';
import { UpdateGymDto } from './dto/update-gym.dto';
import { GymsService } from './gyms.service';

@ApiTags('Gyms')
@ApiBearerAuth()
@Controller('gyms')
export class GymsController {
  constructor(
    private readonly gyms: GymsService,
    private readonly auth: AuthService,
  ) {}

  /** All gyms the user can switch to (owner, staff, trainer, member). */
  @Get()
  @ApiOperation({ summary: 'List gyms for current user' })
  list(@CurrentUser() user: JwtUser) {
    return this.gyms.listForUser(user.sub);
  }

  /**
   * Create another owned gym, or the first gym during owner onboarding.
   * Data stays isolated by `gym_id` on all related tables; use `X-Gym-Id` on requests.
   */
  @Post()
  @ApiOperation({
    summary: 'Create owned gym',
    description:
      'Returns access_token + refresh_token when completed with a temp signup token.',
  })
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

  @Patch(':id')
  @ApiOperation({
    summary: 'Update gym',
    description:
      'Owner or platform super-admin. Renaming updates the public `slug` when the name changes.',
  })
  @ApiParam({ name: 'id', description: 'Gym id' })
  @ApiOkResponse({
    description: 'Updated gym',
    schema: {
      type: 'object',
      properties: {
        id: { type: 'string' },
        name: { type: 'string' },
        slug: { type: 'string' },
        status: { type: 'string' },
      },
    },
  })
  @ApiBadRequestResponse({ description: 'Empty body / validation error' })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'Not the gym owner' })
  @ApiNotFoundResponse({ description: 'Gym not found' })
  patch(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Body() body: UpdateGymDto,
  ) {
    return this.gyms.update(user, id, body);
  }

  @Delete(':id')
  @ApiOperation({
    summary: 'Delete gym',
    description:
      'Owner or platform super-admin. Permanently removes the gym and cascades tenant data.',
  })
  @ApiParam({ name: 'id', description: 'Gym id' })
  @ApiOkResponse({
    description: 'Gym deleted',
    schema: {
      type: 'object',
      properties: { success: { type: 'boolean', example: true } },
    },
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'Not the gym owner' })
  @ApiNotFoundResponse({ description: 'Gym not found' })
  remove(@CurrentUser() user: JwtUser, @Param('id') id: string) {
    return this.gyms.remove(user, id);
  }
}
