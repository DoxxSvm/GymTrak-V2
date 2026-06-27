import { Type } from 'class-transformer';
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  Min,
} from 'class-validator';

/** `gymId` + pagination for member tab endpoints (subscriptions, attendance, payments) */
export class MemberTabQueryDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  /** With `year`, switches `GET .../attendance` to calendar payload for that month (UTC). */
  @IsOptional()
  @IsString()
  month?: string;

  @IsOptional()
  @IsString()
  year?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number = 20;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  offset?: number = 0;
}
