import { IsInt, IsNotEmpty, IsOptional, IsString, Min } from 'class-validator';

export class CreateSubscriptionCompatDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsOptional()
  @IsString()
  plan_id?: string;

  @IsOptional()
  @IsString()
  plan_name?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  duration_months?: number;

  @IsInt()
  @Min(0)
  price: number;

  @IsString()
  @IsNotEmpty()
  start_date: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  discount?: number;
}
