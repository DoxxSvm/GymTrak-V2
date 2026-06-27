import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { Public } from '../../common/decorators/public.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { AttendanceService } from './attendance.service';
import { BiometricCheckInDto } from './dto/biometric-check-in.dto';
import { BlockAttendanceDto } from './dto/block-attendance.dto';
import { DailyLogsQueryDto } from './dto/daily-logs-query.dto';
import { MonthlyStatsQueryDto } from './dto/monthly-stats-query.dto';
import { RegisterBiometricDto } from './dto/register-biometric.dto';
import { MarkAttendanceDto } from './dto/mark-attendance.dto';
import { MemberAttendanceCheckInDto } from './dto/member-check-in.dto';
import { MemberAttendanceCheckOutDto } from './dto/member-check-out.dto';

@ApiTags('Attendance')
@ApiBearerAuth()
@Controller('attendance')
export class AttendanceController {
  constructor(private readonly attendance: AttendanceService) {}

  @Post()
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Mark member present or absent',
    description:
      'Body: `member_id` (GymUser id), `date` (YYYY-MM-DD), optional `time` (HH:mm), `status` present|absent. Gym owner allowed; staff need `members:manage`.',
  })
  mark(@CurrentUser() user: JwtUser, @Body() body: MarkAttendanceDto) {
    return this.attendance.markAttendanceByOwner(user.sub, body);
  }

  /** Owner: biometric-style check-in (deduped per member per day). */
  @Post(['clock-in', 'check-in'])
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Member clock-in (owner / staff)',
    description:
      'Explicit clock-in endpoint. Preferred one-endpoint flow: `POST /attendance/clock`. Legacy alias: `POST /attendance/check-in`.',
  })
  checkIn(
    @CurrentUser() user: JwtUser,
    @Body() body: MemberAttendanceCheckInDto,
  ) {
    return this.attendance.memberCheckInByOwner(user.sub, body);
  }

  @Post(['clock-out', 'check-out'])
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Member clock-out (owner / staff)',
    description:
      'Explicit clock-out endpoint. Preferred one-endpoint flow: `POST /attendance/clock`. Legacy alias: `POST /attendance/check-out`.',
  })
  checkOut(
    @CurrentUser() user: JwtUser,
    @Body() body: MemberAttendanceCheckOutDto,
  ) {
    return this.attendance.memberCheckOutByOwner(user.sub, body);
  }

  @Post(['clock', 'punch'])
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Member attendance clock (auto clock-in/clock-out)',
    description:
      'Single endpoint for both actions. If no record exists for the day, creates clock-in; if clocked-in and not clocked-out, performs clock-out. Legacy alias: `POST /attendance/punch`.',
  })
  punch(
    @CurrentUser() user: JwtUser,
    @Body() body: MemberAttendanceCheckInDto,
  ) {
    return this.attendance.memberPunchByOwner(user.sub, body);
  }

  /** Kiosk / device — no JWT; uses deviceId + apiKey from registration. */
  @Public()
  @Post(['biometric/punch', 'biometric/check-in'])
  @ApiOperation({
    summary: 'Biometric attendance punch (kiosk / device)',
    description:
      '**No Bearer token.** Auto punch behavior: first scan clocks in, second scan clocks out for the same day. Preferred path: `POST /attendance/biometric/punch` (legacy alias: `/attendance/biometric/check-in`). Body: `gymId`, `deviceId`, `apiKey` (from `POST /attendance/biometric/register`).',
    security: [],
  })
  biometricCheckIn(@Body() body: BiometricCheckInDto) {
    return this.attendance.checkInBiometric(
      body.gymId,
      body.deviceId.trim(),
      body.apiKey.trim(),
    );
  }

  @Post('biometric/register')
  @ApiOperation({
    summary: 'Register biometric device credentials',
    description: 'Body: `gymId`, optional `label`. Returns api key for kiosk.',
  })
  registerBiometric(
    @CurrentUser() user: JwtUser,
    @Body() body: RegisterBiometricDto,
  ) {
    return this.attendance.registerBiometric(user.sub, body.gymId, body.label);
  }

  @Get('biometric/credentials')
  @ApiOperation({ summary: 'List registered biometric credentials for a gym' })
  listBiometric(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.attendance.listBiometricCredentials(user.sub, query.gymId);
  }

  @Delete('biometric/credentials/:credentialId')
  @ApiOperation({ summary: 'Revoke a biometric credential' })
  revokeBiometric(
    @CurrentUser() user: JwtUser,
    @Param('credentialId') credentialId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.attendance.revokeBiometric(user.sub, query.gymId, credentialId);
  }

  @Get('logs/daily')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Daily attendance logs',
    description:
      'Query: `gymId`, `date` (YYYY-MM-DD), optional `limit`, `offset`.',
  })
  dailyLogs(@CurrentUser() user: JwtUser, @Query() query: DailyLogsQueryDto) {
    const limit = query.limit ?? 50;
    const offset = query.offset ?? 0;
    return this.attendance.listDaily(
      user.sub,
      query.gymId,
      query.date,
      limit,
      offset,
    );
  }

  @Get('logs/monthly')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Monthly attendance stats',
    description: 'Query: `gymId`, `year`, `month` (1–12).',
  })
  monthlyStats(
    @CurrentUser() user: JwtUser,
    @Query() query: MonthlyStatsQueryDto,
  ) {
    return this.attendance.monthlyStats(
      user.sub,
      query.gymId,
      query.year,
      query.month,
    );
  }

  @Get('members/:memberUserId/lifetime')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Member lifetime attendance stats',
    description: 'Path: `memberUserId` (User id). Query: `gymId`.',
  })
  lifetime(
    @CurrentUser() user: JwtUser,
    @Param('memberUserId') memberUserId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.attendance.memberLifetimeStats(
      user.sub,
      query.gymId,
      memberUserId,
    );
  }

  @Get('summary')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({ summary: 'Gym-wide attendance summary' })
  summary(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.attendance.gymSummary(user.sub, query.gymId);
  }

  /** Block or unblock member attendance (QR + biometric). */
  @Patch('members/:gymUserId/block')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  @ApiOperation({
    summary: 'Block or unblock member attendance',
    description:
      'Path: member `gymUserId`. Query: `gymId`. Body: `blocked` (boolean), optional `reason`.',
  })
  setBlock(
    @CurrentUser() user: JwtUser,
    @Param('gymUserId') gymUserId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: BlockAttendanceDto,
  ) {
    return this.attendance.setAttendanceBlock(
      user.sub,
      query.gymId,
      gymUserId,
      body.blocked,
      body.reason,
    );
  }
}
