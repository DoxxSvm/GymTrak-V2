import { Type } from 'class-transformer';
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
} from 'class-validator';

/** `GET .../attendance/summary` — month view + lifetime + recent + months overview */
export class MemberAttendanceSummaryQueryDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  /** 1–12; with `year`, selects calendar month (UTC boundaries, same as legacy attendance). */
  @IsOptional()
  @IsString()
  month?: string;

  @IsOptional()
  @IsString()
  year?: string;

  /** IANA timezone for display formatting (e.g. "Asia/Kolkata"). Falls back to gym timezone. */
  @IsOptional()
  @IsString()
  timezone?: string;

  /** Max rows in `months_overview` (default 24). */
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(60)
  monthsOverviewLimit?: number = 24;

  /** Max `recent_logs` (default 20). */
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(50)
  recentLimit?: number = 20;
}

/** `GET .../attendance/history` — paginated log with optional date range (YYYY-MM-DD, UTC date) */
export class MemberAttendanceHistoryQueryDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  from?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  to?: string;

  /** IANA timezone for display formatting (e.g. "Asia/Kolkata"). Falls back to gym timezone. */
  @IsOptional()
  @IsString()
  timezone?: string;

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
