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
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateGymPlanDto } from './dto/create-gym-plan.dto';
import { PlanEnrolledQueryDto } from './dto/plan-enrolled-query.dto';
import { PlanListQueryDto } from './dto/plan-list-query.dto';
import { UpdateGymPlanDto } from './dto/update-gym-plan.dto';
import { CreatePlanCompatDto } from './dto/create-plan-compat.dto';
import { UpdatePlanCompatDto } from './dto/update-plan-compat.dto';
import { ValidatePlanDto } from './dto/validate-plan.dto';
import { PlansService } from './plans.service';

@Controller('plans')
@UseGuards(PermissionsGuard)
export class PlansController {
  constructor(private readonly plans: PlansService) {}

  @Get()
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  list(@CurrentUser() user: JwtUser, @Query() query: PlanListQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.plans.list(user.sub, query.gymId, query.type, limit, offset);
  }

  @Post()
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  create(
    @CurrentUser() user: JwtUser,
    @Query() q: GymIdQueryDto,
    @Body() body: CreateGymPlanDto,
  ) {
    return this.plans.create(user.sub, q.gymId, body);
  }

  @Post('validate')
  validatePlan(@Body() body: ValidatePlanDto) {
    return this.plans.validateCompatPayload(body);
  }

  @Post('compat')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  createCompat(
    @CurrentUser() user: JwtUser,
    @Body() body: CreatePlanCompatDto,
  ) {
    return this.plans.createCompat(user.sub, body);
  }

  @Get(':planId/enrolled')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  enrolled(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: PlanEnrolledQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.plans.listEnrolled(
      user.sub,
      query.gymId,
      planId,
      limit,
      offset,
    );
  }

  @Get(':planId')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.plans.getOne(user.sub, query.gymId, planId);
  }

  @Patch(':planId')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  update(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateGymPlanDto,
  ) {
    return this.plans.update(user.sub, query.gymId, planId, body);
  }

  @Put(':planId')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  updateCompat(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdatePlanCompatDto,
  ) {
    return this.plans.updateCompat(user.sub, query.gymId, planId, body);
  }

  @Delete(':planId')
  @RequirePermissions(PERMISSION_CODES.MEMBERS)
  remove(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.plans.softDelete(user.sub, query.gymId, planId);
  }

  @Post('/member-plans')
  assignToMember(
    @CurrentUser() user: JwtUser,
    @Body()
    body: {
      member_id: string;
      plan_id: string;
      start_date: string;
      discount?: number;
    },
  ) {
    return this.plans.assignMemberPlan(user.sub, body);
  }
}
