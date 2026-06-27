import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
  UseGuards,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateTrainerDto } from './dto/create-trainer.dto';
import { RecordTrainerSalaryPaymentDto } from './dto/record-trainer-salary-payment.dto';
import { TrainerListQueryDto } from './dto/trainer-list-query.dto';
import { TrainerTabQueryDto } from './dto/trainer-tab-query.dto';
import { UpdateTrainerDto } from './dto/update-trainer.dto';
import { ChangeTrainerPasswordDto } from './dto/change-trainer-password.dto';
import { UpdateTrainerPermissionsDto } from './dto/update-trainer-permissions.dto';
import { TrainerPermissionsDto } from './dto/trainer-permissions.dto';
import { CreateTrainerCompatDto } from './dto/create-trainer-compat.dto';
import { PayTrainerSalaryMobileDto } from './dto/pay-trainer-salary-mobile.dto';
import { TrainersService } from './trainers.service';
import { GymRole } from '@prisma/client';

const CREATE_TRAINER_COMPAT_PIPE = new ValidationPipe({
  transform: true,
  whitelist: true,
  forbidNonWhitelisted: false,
});

/**
 * `trainerId` path param is the trainer's `GymUser.id` (role TRAINER).
 */
@Controller('trainers')
export class TrainersController {
  constructor(private readonly trainers: TrainersService) { }

  /** Logged-in staff: coarse RBAC flags (prefer `GET /rbac/effective` for full matrix). */
  @Get('me/permissions')
  mePermissions(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.trainers.getSelfPermissions(user.sub, query.gymId);
  }

  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: TrainerListQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.trainers.list(
      user.sub,
      query.gymId,
      query.role,
      query.q,
      query.includeInactive,
      limit,
      offset,
    );
  }

  // @Post()
  // create(@CurrentUser() user: JwtUser, @Body() body: CreateTrainerDto) {
  //   return this.trainers.create(user.sub, body);
  // }

  @Post('compat')
  @UsePipes(CREATE_TRAINER_COMPAT_PIPE)
  createCompat(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateTrainerCompatDto,
  ) {
    const shifts =
      body.shifts && body.shifts.length > 0
        ? body.shifts.map((s) => ({
          dayOfWeek: s.dayOfWeek,
          startTime: s.startTime,
          endTime: s.endTime,
        }))
        : (body.shift?.days?.map((d) => ({
          dayOfWeek: this.dayToInt(d),
          startTime: body.shift!.start_time,
          endTime: body.shift!.end_time,
        })) ?? []);
    return this.trainers.create(user.sub, {
      gymId: body.gymId,
      role: body.role ?? GymRole.TRAINER,
      phone: body.phone,
      fullName: body.full_name,
      avatarUrl: body.profile_image,
      dateOfBirth: body.dob,
      gender: body.gender,
      experience: body.experience,
      address: body.address,
      expertise: body.expertise ?? [],
      salaryCents: body.salary != null ? body.salary : undefined,
      salaryPeriod: this.mapSalaryType(body.salary_type),
      notes: undefined,
      shifts,
      permissions: body.permissions as string[],
      generateLoginCredentials:
        !!body.credentials &&
        !body.credentials.trainer_id &&
        !body.credentials.password,
      username: body.credentials?.trainer_id,
      password: body.credentials?.password,
    });
  }

  @Get(':trainerId/plans')
  getPlansTab(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getPlansTab(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/clients')
  getClients(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getTrainerClients(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/revenue')
  getRevenue(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getTrainerRevenue(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/salary')
  getSalaryMobile(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getTrainerSalaryMobile(
      user.sub,
      query.gymId,
      trainerId,
    );
  }

  @Post(':trainerId/salary/pay')
  paySalaryMobile(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: PayTrainerSalaryMobileDto,
  ) {
    return this.trainers.payTrainerSalaryMobile(
      user.sub,
      query.gymId,
      trainerId,
      body,
    );
  }

  @Get(':trainerId/attendance')
  getAttendanceTab(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: TrainerTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.trainers.getAttendanceTab(
      user.sub,
      query.gymId,
      trainerId,
      limit,
      offset,
    );
  }

  @Get(':trainerId/salary-payments')
  getSalaryPaymentsTab(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: TrainerTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.trainers.getSalaryPaymentsTab(
      user.sub,
      query.gymId,
      trainerId,
      limit,
      offset,
    );
  }

  @Post([':trainerId/attendance/punch', ':trainerId/attendance/check-in'])
  punchAttendance(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.punchAttendance(user.sub, query.gymId, trainerId);
  }

  @Post(':trainerId/salary-payments')
  recordSalaryPayment(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: RecordTrainerSalaryPaymentDto,
  ) {
    return this.trainers.recordSalaryPayment(
      user.sub,
      query.gymId,
      trainerId,
      body,
    );
  }

  /** Issue or rotate staff login (username + password) for `POST /auth/staff/login`. */
  @Post(':trainerId/credentials')
  generateCredentials(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.generateLoginCredentials(
      user.sub,
      query.gymId,
      trainerId,
    );
  }

  @Patch(':trainerId')
  update(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateTrainerDto,
  ) {
    return this.trainers.update(user.sub, query.gymId, trainerId, body);
  }

  /** Trainer details — Basic info tab (profile, expertise, shifts, permissions). */
  @Get(':trainerId')
  @UseGuards(PermissionsGuard)
  getBasic(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getBasic(user.sub, query.gymId, trainerId, query.role);
  }

  @Get(':trainerId/members')
  members(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getAssignedMembers(user.sub, query.gymId, trainerId);
  }

  @Put(':trainerId')
  replace(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateTrainerDto,
  ) {
    return this.trainers.update(user.sub, query.gymId, trainerId, body);
  }

  @Delete(':trainerId')
  remove(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.softDelete(user.sub, query.gymId, trainerId);
  }

  @Put(':trainerId/password')
  changePassword(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: ChangeTrainerPasswordDto,
  ) {
    return this.trainers.changePassword(
      user.sub,
      query.gymId,
      trainerId,
      body.password,
    );
  }

  @Put(':trainerId/permissions')
  updatePermissions(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateTrainerPermissionsDto,
  ) {
    return this.trainers.updatePermissionsCompat(
      user.sub,
      query.gymId,
      trainerId,
      body,
    );
  }

  /** Dynamic trainer permission update (industry shape). */
  @Patch(':trainerId/permissions')
  updatePermissionsDynamic(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: TrainerPermissionsDto,
  ) {
    return this.trainers.updatePermissions(
      user.sub,
      query.gymId,
      trainerId,
      body,
    );
  }

  /**
   * Compat: string day name or Monday-indexed int (0=Mon..6=Sun) → DB int (0=Sun..6=Sat).
   */
  private dayToInt(d: string | number): number {
    if (typeof d === 'number') {
      return (d + 1) % 7;
    }
    const s = String(d).trim();
    if (/^\d+$/.test(s)) {
      const n = parseInt(s, 10);
      return (n + 1) % 7;
    }
    switch (s.toLowerCase()) {
      case 'sun':
      case 'sunday':
        return 0;
      case 'mon':
      case 'monday':
        return 1;
      case 'tue':
      case 'tuesday':
        return 2;
      case 'wed':
      case 'wednesday':
        return 3;
      case 'thu':
      case 'thursday':
        return 4;
      case 'fri':
      case 'friday':
        return 5;
      case 'sat':
      case 'saturday':
        return 6;
      default:
        return 1;
    }
  }

  private mapSalaryType(v?: string) {
    switch ((v ?? '').toLowerCase()) {
      case 'hourly':
        return 'HOURLY' as const;
      case 'weekly':
        return 'WEEKLY' as const;
      case 'yearly':
        return 'YEARLY' as const;
      case 'monthly':
      default:
        return 'MONTHLY' as const;
    }
  }
}
