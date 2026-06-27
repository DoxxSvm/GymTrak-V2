import { Type } from 'class-transformer';
import { IsInt, IsOptional, IsString, Min } from 'class-validator';

export class ExtendSubscriptionDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  addDays?: number;

  @IsOptional()
  @IsString()
  newEndsAt?: string;

  /// Extra amount charged for this extension (added to priceCents)
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  additionalPriceCents?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  additional_days?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  additional_fee?: number;

  @IsOptional()
  @IsString()
  reason?: string;
}
