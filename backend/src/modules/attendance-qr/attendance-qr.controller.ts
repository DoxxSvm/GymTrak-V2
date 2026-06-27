import { Body, Controller, Get, Headers, Post, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { ConfigService } from '@nestjs/config';
import { verify } from 'jsonwebtoken';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { Public } from '../../common/decorators/public.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AttendanceQrService } from './attendance-qr.service';
import { AttendanceCheckInDto } from './dto/check-in.dto';

@ApiTags('Attendance QR')
@ApiBearerAuth()
@Controller('attendance-qr')
export class AttendanceQrController {
  constructor(
    private readonly attendanceQr: AttendanceQrService,
    private readonly config: ConfigService,
  ) {}

  /** Static attendance QR for current owner/trainer/member at a gym. */
  @Get(['my-qr', 'member-token'])
  @ApiOperation({
    summary: 'Get static attendance QR',
    description:
      'Returns one static signed QR token (plus base64 image) for the current user at this gym. Works for owner, trainer, and member. Preferred route: `GET /attendance-qr/my-qr`.',
  })
  memberToken(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.attendanceQr.getMemberToken(user.sub, query.gymId);
  }

  @Public()
  @Post('punch')
  @ApiOperation({
    summary: 'Attendance clock punch with static user QR',
    description:
      'Single punch API. Body: `token` from `GET /attendance-qr/my-qr`. If `Authorization: Bearer` is provided and token is owner QR, attendance is punched for the logged-in member/trainer/staff user.',
    security: [],
  })
  checkIn(
    @Body() body: AttendanceCheckInDto,
    @Headers('authorization') authorization?: string,
  ) {
    const actorUserId = this.extractActorUserIdFromBearer(authorization);
    return this.attendanceQr.checkInWithToken(body.token.trim(), actorUserId);
  }

  @Post('punch/me')
  @ApiOperation({
    summary: 'Attendance punch for logged-in user using gym QR',
    description:
      'Bearer required. Allows logged-in member/trainer/staff to punch themselves using any valid gym QR token (including owner QR) from the same gym.',
  })
  punchMe(
    @CurrentUser() user: JwtUser,
    @Body() body: AttendanceCheckInDto,
  ) {
    return this.attendanceQr.punchLoggedInUserWithToken(
      user.sub,
      body.token.trim(),
    );
  }

  private extractActorUserIdFromBearer(
    authorization?: string,
  ): string | undefined {
    if (!authorization) {
      return undefined;
    }
    const match = /^Bearer\s+(.+)$/i.exec(authorization.trim());
    if (!match) {
      return undefined;
    }
    const secret = this.config.get<string>('JWT_ACCESS_SECRET');
    if (!secret) {
      return undefined;
    }
    try {
      const decoded = verify(match[1], secret);
      if (!decoded || typeof decoded !== 'object') {
        return undefined;
      }
      const sub = (decoded as { sub?: unknown }).sub;
      return typeof sub === 'string' && sub.length > 0 ? sub : undefined;
    } catch {
      return undefined;
    }
  }
}
