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

/**
 * `trainerId` path param is the trainer's `GymUser.id` (role TRAINER).
 */
@Controller('trainers')
export class TrainersController {
  constructor(private readonly trainers: TrainersService) {}

  /** Logged-in staff: coarse RBAC flags (prefer `GET /rbac/effective` for full matrix). */
  @Get('me/permissions')
  mePermissions(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.trainers.getSelfPermissions(user.sub, query.gymId);
  }

  @Get()
  @UseGuards(PermissionsGuard)
  list(@CurrentUser() user: JwtUser, @Query() query: TrainerListQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.trainers.list(
      user.sub,
      query.gymId,
      query.q,
      query.includeInactive,
      limit,
      offset,
    );
  }

  @Post()
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  create(@CurrentUser() user: JwtUser, @Body() body: CreateTrainerDto) {
    return this.trainers.create(user.sub, body);
  }

  @Post('compat')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  createCompat(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateTrainerCompatDto,
  ) {
    const shifts =
      body.shift?.days?.map((d) => ({
        dayOfWeek: this.dayToInt(d),
        startTime: body.shift!.start_time,
        endTime: body.shift!.end_time,
      })) ?? [];
    return this.trainers.create(user.sub, {
      gymId: body.gymId,
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
      permissions: {
        members: !!(
          body.permissions?.add_members ||
          body.permissions?.add_clients ||
          body.permissions?.view_member_details
        ),
        dashboard: !!(
          body.permissions?.view_dashboard || body.permissions?.show_dashboard
        ),
        payments: !!(
          body.permissions?.view_payments ||
          body.permissions?.show_payments ||
          body.permissions?.show_payment_in_details
        ),
        admin: !!body.permissions?.add_trainer,
      },
      generateLoginCredentials:
        !!body.credentials &&
        !body.credentials.trainer_id &&
        !body.credentials.password,
      username: body.credentials?.trainer_id,
      password: body.credentials?.password,
    });
  }

  @Get(':trainerId/plans')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  getPlansTab(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getPlansTab(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/clients')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  getClients(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getTrainerClients(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/revenue')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  getRevenue(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getTrainerRevenue(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/salary')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
    return this.trainers.getBasic(user.sub, query.gymId, trainerId);
  }

  @Get(':trainerId/members')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  members(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.getAssignedMembers(user.sub, query.gymId, trainerId);
  }

  @Put(':trainerId')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  replace(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateTrainerDto,
  ) {
    return this.trainers.update(user.sub, query.gymId, trainerId, body);
  }

  @Delete(':trainerId')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
  remove(
    @CurrentUser() user: JwtUser,
    @Param('trainerId') trainerId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.trainers.softDelete(user.sub, query.gymId, trainerId);
  }

  @Put(':trainerId/password')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.ADMIN)
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

  private dayToInt(d: string): number {
    switch (d.toLowerCase()) {
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
