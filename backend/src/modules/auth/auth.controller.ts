import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Post,
  UseGuards,
} from '@nestjs/common';
import { Throttle, ThrottlerGuard } from '@nestjs/throttler';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { Public } from '../../common/decorators/public.decorator';
import { AuthService } from './auth.service';
import { LogoutDto } from './dto/logout.dto';
import { AuthLoginBodyDto } from './dto/auth-login-body.dto';
import { MobileSendOtpDto } from './dto/mobile-send-otp.dto';
import { MobileVerifyOtpDto } from './dto/mobile-verify-otp.dto';
import { RefreshTokenDto } from './dto/refresh-token.dto';
import { SendOtpDto } from './dto/send-otp.dto';
import { StaffLoginDto } from './dto/staff-login.dto';
import { VerifyOtpDto } from './dto/verify-otp.dto';
import type { JwtUser } from './types/jwt-user.type';

@Controller('auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Public()
  @Post('otp/send')
  sendOtp(@Body() dto: SendOtpDto) {
    return this.auth.sendOtp(dto.phone, dto.purpose);
  }

  @Public()
  @Post('otp/verify')
  verifyOtp(@Body() dto: VerifyOtpDto) {
    return this.auth.verifyOtp(dto.phone, dto.code, dto.purpose);
  }

  @Public()
  @Post('staff/login')
  staffLogin(@Body() dto: StaffLoginDto) {
    return this.auth.staffLogin(dto.username, dto.password);
  }

  @Public()
  @Post('refresh')
  refresh(@Body() dto: RefreshTokenDto) {
    return this.auth.refresh(dto.refreshToken);
  }

  @Public()
  @UseGuards(ThrottlerGuard)
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('send-otp')
  sendOtpMobile(@Body() dto: MobileSendOtpDto) {
    return this.auth.sendOwnerAppOtp(dto.phone, dto.country_code);
  }

  @Public()
  @UseGuards(ThrottlerGuard)
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('resend-otp')
  resendOtpMobile(@Body() dto: MobileSendOtpDto) {
    return this.auth.sendOwnerAppOtp(dto.phone, dto.country_code);
  }

  @Public()
  @UseGuards(ThrottlerGuard)
  @Throttle({ default: { limit: 8, ttl: 60_000 } })
  @Post('verify-otp')
  verifyOtpMobile(@Body() dto: MobileVerifyOtpDto) {
    return this.auth.verifyOwnerAppOtp(dto.phone, dto.otp, dto.country_code);
  }

  @Public()
  @UseGuards(ThrottlerGuard)
  @Throttle({ default: { limit: 10, ttl: 60_000 } })
  @Post('login')
  loginMobile(@Body() dto: AuthLoginBodyDto) {
    if (dto.phone != null && String(dto.phone).length > 0) {
      return this.auth.phoneLoginSendOtp(dto.phone, dto.country_code);
    }
    if (dto.username && dto.password) {
      return this.auth.mobileLogin(dto.username, dto.password);
    }
    throw new BadRequestException(
      'Provide phone (and optional country_code) or username and password',
    );
  }

  @Post('logout')
  logout(@Body() dto: LogoutDto) {
    return this.auth.logout(dto.refresh_token);
  }

  /** User profile + onboarding flags (refresh client routing after app resume). */
  @Get('me')
  me(@CurrentUser() user: JwtUser) {
    return this.auth.getProfile(user.sub);
  }
}
