import {
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Min,
} from 'class-validator';

export class ReceiveGymPaymentDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsOptional()
  @IsString()
  subscription_id?: string;

  @IsInt()
  @Min(1)
  amount: number;

  @IsString()
  @IsIn(['cash', 'upi', 'card'])
  payment_mode: 'cash' | 'upi' | 'card';

  @IsOptional()
  @IsString()
  date?: string;
}
