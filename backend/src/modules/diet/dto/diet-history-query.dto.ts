import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import {
  IsInt,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
} from 'class-validator';

export class DietHistoryQueryDto {
  @ApiPropertyOptional({
    example: '2026-02-17',
    description:
      'Day to load (YYYY-MM-DD). Defaults to today (UTC) when omitted',
  })
  @IsOptional()
  @Transform(({ value }) => (value === '' || value == null ? undefined : value))
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  date?: string;

  @ApiPropertyOptional({
    description:
      'Optional. If set, gym-scoped read: only rows with this `gymId` and your user id; you must be an active **member** at that gym. Times use the gym’s timezone. If omitted, user-scoped: all your rows for the day (UTC for times when rows have no gym).',
  })
  @IsOptional()
  @IsString()
  gymId?: string;

  @ApiPropertyOptional({
    example: 2200,
    description:
      'Daily calorie goal; used to compute `remaining` in the summary (defaults to 2000 if omitted)',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  @Max(20000)
  target_kcal?: number;
}
