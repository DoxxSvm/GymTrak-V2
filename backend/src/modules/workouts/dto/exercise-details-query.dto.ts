import { ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsIn, IsInt, IsOptional, Max, Min } from 'class-validator';
import { OptionalGymIdQueryDto } from '../../../common/dto/optional-gym-id-query.dto';

export const EXERCISE_DETAIL_CHART_RANGES = ['3m', '6m', '12m'] as const;
export type ExerciseDetailChartRange =
  (typeof EXERCISE_DETAIL_CHART_RANGES)[number];

export const EXERCISE_DETAIL_CHART_METRICS = [
  'heaviest_weight',
  'one_rep_max',
  'best_set_volume',
] as const;
export type ExerciseDetailChartMetric =
  (typeof EXERCISE_DETAIL_CHART_METRICS)[number];

/** Query for `GET /exercises/:exerciseId` (summary chart + history pagination). */
export class ExerciseDetailsQueryDto extends OptionalGymIdQueryDto {
  @ApiPropertyOptional({
    enum: EXERCISE_DETAIL_CHART_RANGES,
    default: '3m',
    description:
      'Calendar months included in `summary.chart` (UTC month buckets).',
  })
  @IsOptional()
  @IsIn(EXERCISE_DETAIL_CHART_RANGES)
  chart_range?: ExerciseDetailChartRange;

  @ApiPropertyOptional({
    enum: EXERCISE_DETAIL_CHART_METRICS,
    default: 'heaviest_weight',
    description: 'Metric used for each month’s `summary.chart[].value`.',
  })
  @IsOptional()
  @IsIn(EXERCISE_DETAIL_CHART_METRICS)
  chart_metric?: ExerciseDetailChartMetric;

  @ApiPropertyOptional({ default: 1, minimum: 1 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  history_page?: number;

  @ApiPropertyOptional({ default: 20, minimum: 1, maximum: 50 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(50)
  history_limit?: number;
}
