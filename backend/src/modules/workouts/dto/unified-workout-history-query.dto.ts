import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import {
  IsBoolean,
  IsInt,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
} from 'class-validator';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function optionalQueryBoolean(v: unknown): boolean | undefined {
  if (v === undefined || v === null || v === '') return undefined;
  if (v === true || v === 1) return true;
  if (v === false || v === 0) return false;
  if (typeof v === 'string') {
    const s = v.toLowerCase().trim();
    if (s === 'true' || s === '1' || s === 'yes') return true;
    if (s === 'false' || s === '0' || s === 'no') return false;
  }
  return undefined;
}

/** `GET /workouts/history` — identity from JWT only; optional `gymId` scopes gym sessions. */
export class UnifiedWorkoutHistoryQueryDto {
  @ApiPropertyOptional({
    description:
      'Omit: personal plans (`gym_id` null) for the authenticated user. Set: gym-scoped — members see their `gym_user_id` rows; trainers see plans they authored (`user_id` = JWT) in that gym.',
  })
  @IsOptional()
  @IsString()
  gymId?: string;

  @ApiPropertyOptional({ description: '0-based page (default 0).' })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  page?: number;

  @ApiPropertyOptional({ default: 20, maximum: 100 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number;

  @ApiPropertyOptional({
    description: 'Inclusive filter on `started_at`, YYYY-MM-DD (UTC).',
  })
  @IsOptional()
  @IsString()
  @Matches(ISO_DATE, { message: 'from must be YYYY-MM-DD' })
  from?: string;

  @ApiPropertyOptional({
    description: 'Inclusive filter on `started_at`, YYYY-MM-DD (UTC).',
  })
  @IsOptional()
  @IsString()
  @Matches(ISO_DATE, { message: 'to must be YYYY-MM-DD' })
  to?: string;

  @ApiPropertyOptional({ description: 'Filter by `completed`.' })
  @IsOptional()
  @Transform(({ value }) => optionalQueryBoolean(value))
  @IsBoolean()
  completed?: boolean;
}
