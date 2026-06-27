import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { Public } from '../../common/decorators/public.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { GenerateGymQrDto } from './dto/generate-gym-qr.dto';
import { SelectRoleDto } from './dto/select-role.dto';
import { SetGymDto } from './dto/set-gym.dto';
import { OwnerAppService } from './owner-app.service';

@ApiTags('Owner signup')
@ApiBearerAuth()
@Controller()
export class OwnerAppController {
  constructor(private readonly ownerApp: OwnerAppService) {}

  @Post('user/select-role')
  @ApiOperation({
    summary: 'Select role (owner app + member onboarding)',
    description:
      '**Bearer is required** (`tempToken` after new-signup OTP, or `access_token`). Omitting it is not allowed on this route (avoids dev fallback user mismatch). `gym_owner` or `owner` → owner path, then `POST /gym` or `POST /gyms`. `trainer` → trainer path (must already be a trainer at a gym). `member` → gym-free consumer path (`session` + `user`), then `POST /onboarding/member`. **Response always includes** `access_token` and `refresh_token` (new pair).',
  })
  selectRole(@CurrentUser() user: JwtUser, @Body() dto: SelectRoleDto) {
    return this.ownerApp.selectRole(user, dto.role);
  }

  @Post('gym')
  @ApiOperation({
    summary: 'Set gym (owner app single-gym flow)',
    description:
      'Body: `gym_name`, `owner_name`. Returns access + refresh tokens when started from a temp signup token.',
  })
  setGym(@CurrentUser() user: JwtUser, @Body() dto: SetGymDto) {
    return this.ownerApp.setGym(user, dto);
  }

  /** Owner: save location + radius, generate QR (PNG data URL) into `Gym.gymQrCode`. */
  @Post('gym/generate-qr')
  @ApiOperation({
    summary: 'Generate gym location QR',
    description:
      'Gym owner or super-admin. Body: `gymId`, `address`, `latitude`, `longitude`, `gymRadius` (meters). Updates gym + stores PNG data URL in `gymQrCode`.',
  })
  generateGymQr(@CurrentUser() user: JwtUser, @Body() dto: GenerateGymQrDto) {
    return this.ownerApp.generateGymQr(user.sub, dto);
  }

  /** Public: decode QR payload and return current gym details from DB. */
  @Public()
  @Get('gym/scan-qr')
  @ApiOperation({
    summary: 'Gym details by gymId (public)',
    description:
      '**No Bearer.** Query **`gymId`**. Returns gym details; **`imageUrl`** is the stored gym QR as a PNG **data URL** (`data:image/png;base64,...`), same as `gymQrCode`, or `null` if `POST /gym/generate-qr` was never run.',
    security: [],
  })
  scanGymQr(@Query('gymId') gymId: string) {
    return this.ownerApp.scanGymQr(gymId ?? '');
  }
}
