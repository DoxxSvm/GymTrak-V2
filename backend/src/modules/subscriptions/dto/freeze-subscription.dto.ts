import { Type } from 'class-transformer';
import { IsInt, IsOptional, IsString, Min } from 'class-validator';

export class FreezeSubscriptionDto {
  /// Days membership is paused; the same number of days is added to endsAt
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  freezeDays?: number;

  @IsOptional()
  @IsString()
  freeze_start_date?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  freeze_fee?: number;

  @IsOptional()
  @IsString()
  reason?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  duration_days?: number;
}
