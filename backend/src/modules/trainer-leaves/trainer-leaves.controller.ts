import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';
// TEMP: trainer-leaves — re-enable before production
// import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
// import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
// import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateTrainerLeaveDto } from './dto/create-trainer-leave.dto';
import { LeaveBalanceQueryDto } from './dto/leave-balance-query.dto';
import { ListLeavesQueryDto } from './dto/list-leaves-query.dto';
import { RejectLeaveDto } from './dto/reject-leave.dto';
import { UpdateTrainerLeaveDto } from './dto/update-trainer-leave.dto';
import { TrainerLeavesService } from './trainer-leaves.service';

@ApiTags('Leaves')
@ApiBearerAuth()
@Controller('leaves')
// @UseGuards(PermissionsGuard)
export class TrainerLeavesController {
  constructor(private readonly leaves: TrainerLeavesService) {}

  @Get('balance')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_READ)
  @ApiOperation({ summary: 'Remaining leave balance (annual allowance − approved days this UTC year)' })
  balance(
    @CurrentUser() user: JwtUser,
    @Query() query: LeaveBalanceQueryDto,
  ) {
    return this.leaves.balance(user.sub, query.gymId, query.trainerId);
  }

  @Get()
  // @RequirePermissions(PERMISSION_CODES.LEAVE_READ)
  @ApiOperation({
    summary: 'List leaves',
    description:
      'Trainers see only their rows. Owners / staff with approve|reject|delete see all; optional trainerId, status, month (YYYY-MM), date range, q (name/phone).',
  })
  list(@CurrentUser() user: JwtUser, @Query() query: ListLeavesQueryDto) {
    return this.leaves.list(user.sub, query);
  }

  @Post()
  // @RequirePermissions(PERMISSION_CODES.LEAVE_CREATE)
  @ApiOperation({
    summary: 'Create leave',
    description:
      'Trainer: self only. Owner/staff: set `trainerId` (trainer GymUser id).',
  })
  create(@CurrentUser() user: JwtUser, @Body() body: CreateTrainerLeaveDto) {
    return this.leaves.create(user.sub, body);
  }

  @Patch(':leaveId/approve')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_APPROVE)
  @ApiOperation({ summary: 'Approve leave (owner / staff)' })
  approve(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Param('leaveId') leaveId: string,
  ) {
    return this.leaves.approve(user.sub, query.gymId, leaveId);
  }

  @Patch(':leaveId/reject')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_REJECT)
  @ApiOperation({ summary: 'Reject leave (owner / staff)' })
  reject(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Param('leaveId') leaveId: string,
    @Body() body: RejectLeaveDto,
  ) {
    return this.leaves.reject(user.sub, query.gymId, leaveId, body);
  }

  @Patch(':leaveId/cancel')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_UPDATE)
  @ApiOperation({ summary: 'Cancel own pending leave (trainer)' })
  cancel(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Param('leaveId') leaveId: string,
  ) {
    return this.leaves.cancelMine(user.sub, query.gymId, leaveId);
  }

  @Get(':leaveId')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_READ)
  @ApiOperation({ summary: 'Get one leave' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Param('leaveId') leaveId: string,
  ) {
    return this.leaves.getOne(user.sub, query.gymId, leaveId);
  }

  @Patch(':leaveId')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_UPDATE)
  @ApiOperation({ summary: 'Update own pending leave (trainer)' })
  update(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Param('leaveId') leaveId: string,
    @Body() body: UpdateTrainerLeaveDto,
  ) {
    return this.leaves.update(user.sub, query.gymId, leaveId, body);
  }

  @Delete(':leaveId')
  // @RequirePermissions(PERMISSION_CODES.LEAVE_DELETE)
  @ApiOperation({ summary: 'Delete leave record (owner / staff)' })
  remove(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Param('leaveId') leaveId: string,
  ) {
    return this.leaves.remove(user.sub, query.gymId, leaveId);
  }
}
