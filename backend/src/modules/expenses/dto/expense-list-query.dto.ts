import { ExpenseCategory } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  IsEnum,
  IsIn,
  IsInt,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
} from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class ExpenseListQueryDto extends GymIdQueryDto {
  @IsOptional()
  @IsEnum(ExpenseCategory)
  category?: ExpenseCategory;

  /** YYYY-MM — filter expenses to this calendar month (UTC) */
  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}$/)
  month?: string;

  /** Inclusive start date (YYYY-MM-DD, UTC day bounds). If set with `dateTo`, `month` is ignored for the date filter. */
  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  dateFrom?: string;

  /** Inclusive end date (YYYY-MM-DD, UTC day bounds) */
  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  dateTo?: string;

  /**
   * `this_month` / `last_month`: sets effective `month` (UTC). `yearly`: full UTC calendar year;
   * use optional `year` (defaults current UTC year). Ignores query `month` when `yearly`.
   */
  @IsOptional()
  @IsIn(['this_month', 'last_month', 'yearly'])
  filter?: 'this_month' | 'last_month' | 'yearly';

  /** With `filter=yearly`, which calendar year (UTC). Defaults to current UTC year. */
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(2000)
  @Max(2100)
  year?: number;

  /** `simple`: bare array for mobile lists (use with filter or month) */
  @IsOptional()
  @IsIn(['simple', 'default'])
  format?: 'simple' | 'default';

  @IsOptional()
  @IsIn(['occurredOn', 'createdAt', 'amountCents', 'category'])
  sortBy?: 'occurredOn' | 'createdAt' | 'amountCents' | 'category';

  @IsOptional()
  @IsIn(['asc', 'desc'])
  sortOrder?: 'asc' | 'desc';

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
