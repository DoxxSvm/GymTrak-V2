import { Type } from 'class-transformer';
import { IsInt, IsOptional, IsString, Min } from 'class-validator';

export class RenewSubscriptionDto {
  /// When omitted, the subscription's current catalog plan is used
  @IsOptional()
  @IsString()
  planId?: string;

  @IsOptional()
  @IsString()
  plan_id?: string;

  /// When omitted, the subscription's current gym plan is used
  @IsOptional()
  @IsString()
  gymPlanId?: string;

  @IsOptional()
  @IsString()
  gym_plan_id?: string;

  @IsOptional()
  @IsString()
  start_date?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  price?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  discount?: number;
}
