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
} from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateGymPlanDto } from './dto/create-gym-plan.dto';
import { PlanEnrolledQueryDto } from './dto/plan-enrolled-query.dto';
import { PlanListQueryDto } from './dto/plan-list-query.dto';
import { UpdateGymPlanDto } from './dto/update-gym-plan.dto';
import { CreatePlanCompatDto } from './dto/create-plan-compat.dto';
import { createPlanCompatBodyExamples } from './dto/create-plan-compat.swagger-examples';
import { UpdatePlanCompatDto } from './dto/update-plan-compat.dto';
import { ValidatePlanDto } from './dto/validate-plan.dto';
import { AssignMemberPlanBodyDto } from './dto/assign-member-plan.dto';
import {
  FreezeMemberPlanDto,
  UnfreezeMemberPlanDto,
} from './dto/freeze-member-plan.dto';
import { PlansService } from './plans.service';

@ApiTags('Plans')
@ApiBearerAuth()
@Controller('plans')
export class PlansController {
  constructor(private readonly plans: PlansService) {}

  @Get()
  list(
    @CurrentUser() user: JwtUser,
    @Query() query: PlanListQueryDto,
    @Query('includeInactive') includeInactiveRaw?: string,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    const includeInactive =
      includeInactiveRaw === 'true' || includeInactiveRaw === '1';
    return this.plans.list(
      user.sub,
      query.gymId,
      query.type,
      limit,
      offset,
      includeInactive,
    );
  }

  @Post()
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
  @ApiOperation({
    summary: 'Create plan (mobile / compat body)',
    description:
      'Body shape depends on `planType`. Use the **Examples** dropdown in Swagger to load sample JSON for each plan type. Snake_case aliases (`plan_name`, `trainer_id`, …) are also accepted.',
  })
  @ApiBody({
    type: CreatePlanCompatDto,
    examples: createPlanCompatBodyExamples,
  })
  createCompat(
    @CurrentUser() user: JwtUser,
    @Body() body: CreatePlanCompatDto,
  ) {
    return this.plans.createCompat(user.sub, body);
  }

  @Post('freeze')
  @ApiOperation({
    summary: 'Freeze member subscription',
    description:
      'Pauses access, sets status to `FROZEN`, extends `endsAt` by `duration_days`. Optional `freeze_fee` records a completed payment. Body: `gymId`, `member_subscription_id`, `freeze_start_date` (YYYY-MM-DD), `duration_days`, optional `freeze_fee`, optional `reason`.',
  })
  @ApiBody({ type: FreezeMemberPlanDto })
  freezeMemberPlan(
    @CurrentUser() user: JwtUser,
    @Body() body: FreezeMemberPlanDto,
  ) {
    return this.plans.freezeMemberPlan(user.sub, body);
  }

  @Post('unfreeze')
  @ApiOperation({
    summary: 'Unfreeze member subscription',
    description:
      'Clears freeze window and restores status to `ACTIVE`, `SCHEDULED`, or `ENDED` based on dates. Body: `gymId`, `member_subscription_id`.',
  })
  @ApiBody({ type: UnfreezeMemberPlanDto })
  unfreezeMemberPlan(
    @CurrentUser() user: JwtUser,
    @Body() body: UnfreezeMemberPlanDto,
  ) {
    return this.plans.unfreezeMemberPlan(user.sub, body);
  }

  @Get(':planId/enrolled')
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
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.plans.getOne(user.sub, query.gymId, planId);
  }

  @Patch(':planId')
  update(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateGymPlanDto,
  ) {
    return this.plans.update(user.sub, query.gymId, planId, body);
  }

  @Put(':planId')
  updateCompat(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdatePlanCompatDto,
  ) {
    return this.plans.updateCompat(user.sub, query.gymId, planId, body);
  }

  @Delete(':planId')
  remove(
    @CurrentUser() user: JwtUser,
    @Param('planId') planId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.plans.softDelete(user.sub, query.gymId, planId);
  }

  @Post('member-plans')
  assignToMember(
    @CurrentUser() user: JwtUser,
    @Body() body: AssignMemberPlanBodyDto,
  ) {
    return this.plans.assignMemberPlan(user.sub, body);
  }
}
