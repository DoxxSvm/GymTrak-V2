import { Type } from 'class-transformer';
import {
  IsIn,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

/** POST /expenses — only these body keys (+ required `amount`). */
export class CreateExpenseDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(2000)
  bill_name: string;

  /** `ExpenseCategory` value (e.g. `SALARY`) or slug (e.g. `salary`) */
  @IsString()
  @IsNotEmpty()
  @MaxLength(64)
  category: string;

  @IsString()
  @IsNotEmpty()
  date: string;

  @IsOptional()
  @IsString()
  trainer_id?: string;

  @IsOptional()
  @IsIn(['cash', 'upi', 'card'])
  payment_mode?: 'cash' | 'upi' | 'card';

  @IsOptional()
  @Type(() => Number)
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  @Max(100)
  gst?: number;

  /** Bill total in major currency units (≤ 2 decimals); stored as cents. */
  @Type(() => Number)
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  amount: number;
}
