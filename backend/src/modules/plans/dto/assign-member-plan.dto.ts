import { IsNotEmpty, IsOptional, IsString } from 'class-validator';

/**
 * `POST /plans/member-plans` — same identifiers as `POST /payments` (minus amount / payment_mode).
 * Creates a `MemberSubscription` with `paidCents: 0`.
 */
export class AssignMemberPlanBodyDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsString()
  @IsNotEmpty()
  gym_plan_id: string;

  /**
   * Period start (`startsAt`). `YYYY-MM-DD` → UTC midnight; omit → today (UTC); ISO date-time allowed.
   */
  @IsOptional()
  @IsString()
  date?: string;
}
