import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Min,
} from 'class-validator';

export class CreateTrainerSalaryPaymentDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  /** `GymUser.id` with role TRAINER or STAFF at `gymId`. */
  @IsString()
  @IsNotEmpty()
  trainer_id: string;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  amount: number;

  @IsString()
  @IsNotEmpty()
  @IsIn(['cash', 'upi', 'card'])
  payment_mode: 'cash' | 'upi' | 'card';

  /** YYYY-MM-DD payment date (optional, defaults to today UTC). */
  @IsOptional()
  @IsString()
  date?: string;
}
