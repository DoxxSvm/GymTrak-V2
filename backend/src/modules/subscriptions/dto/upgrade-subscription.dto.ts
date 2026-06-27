import { IsNotEmpty, IsOptional, IsString } from 'class-validator';

export class UpgradeSubscriptionDto {
  /// Global catalog plan (omit if using `gymPlanId`)
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  planId?: string;

  /// Per-gym plan from Plans module (omit if using `planId`)
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  gymPlanId?: string;

  /// Defaults to now
  @IsOptional()
  @IsString()
  startsAt?: string;

  /// If omitted, one plan interval is applied from startsAt
  @IsOptional()
  @IsString()
  endsAt?: string;
}
