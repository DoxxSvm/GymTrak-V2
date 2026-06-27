import { Body, Controller, Post } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PlansService } from './plans.service';

@Controller()
export class PlanCompatController {
  constructor(private readonly plans: PlansService) {}

  @Post('member-plans')
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
