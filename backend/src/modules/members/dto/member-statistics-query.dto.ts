import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional, IsString, Matches } from 'class-validator';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * `GET /members/statistics` — all params optional; defaults: period=week, date=today (UTC), calendar = anchor month.
 */
export class MemberStatisticsQueryDto {
  @ApiPropertyOptional({ enum: ['week', 'month', 'year'], default: 'week' })
  @IsOptional()
  @IsIn(['week', 'month', 'year'])
  period?: 'week' | 'month' | 'year';

  /** Reference date (UTC) for: selected period range, the Mon–Sun week used for `weekly_activity`, and default calendar month. */
  @ApiPropertyOptional({ example: '2026-02-15' })
  @IsOptional()
  @IsString()
  @Matches(ISO_DATE, { message: 'date must be YYYY-MM-DD' })
  date?: string;

  /** If set, gym workouts (`member_workout_plans`) and gym attendance are merged; caller must be an active member. */
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  gymId?: string;

  @ApiPropertyOptional({
    description:
      'Calendar year for `attendance` section (1–12 with calendar_month)',
  })
  @IsOptional()
  @IsString()
  @Matches(/^\d{4}$/)
  calendar_year?: string;

  @ApiPropertyOptional({
    description: 'Calendar month 1–12 for `attendance` section',
  })
  @IsOptional()
  @IsString()
  @Matches(/^(1[0-2]|[1-9])$/)
  calendar_month?: string;
}
