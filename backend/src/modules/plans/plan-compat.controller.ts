import { Body, Controller, Post } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AssignMemberPlanBodyDto } from './dto/assign-member-plan.dto';
import { PlansService } from './plans.service';

@Controller()
export class PlanCompatController {
  constructor(private readonly plans: PlansService) {}

  @Post('member-plans')
  assignToMember(
    @CurrentUser() user: JwtUser,
    @Body() body: AssignMemberPlanBodyDto,
  ) {
    return this.plans.assignMemberPlan(user.sub, body);
  }
}
