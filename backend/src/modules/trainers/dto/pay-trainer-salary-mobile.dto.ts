import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Min,
} from 'class-validator';

export class PayTrainerSalaryMobileDto {
  @Type(() => Number)
  @IsInt()
  @Min(1)
  amount: number;

  @IsString()
  @IsNotEmpty()
  @IsIn(['cash', 'upi', 'card'])
  payment_mode: 'cash' | 'upi' | 'card';

  /** YYYY-MM-DD — payment date; month boundaries for salary period derived from this (UTC) */
  @IsOptional()
  @IsString()
  date?: string;
}
