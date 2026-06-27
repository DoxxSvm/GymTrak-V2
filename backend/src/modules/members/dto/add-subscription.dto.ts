import { Type } from 'class-transformer';
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  Min,
} from 'class-validator';

export class AddMemberSubscriptionDto {
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

  @IsString()
  @IsNotEmpty()
  startsAt: string;

  @IsString()
  @IsNotEmpty()
  endsAt: string;

  /// Override catalog price for this period (e.g. discount)
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  priceCents?: number;

  @IsOptional()
  @IsString()
  @MaxLength(8)
  currency?: string;
}
