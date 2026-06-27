import {
  IsArray,
  IsIn,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

class TrainerCredentialsDto {
  @IsOptional()
  @IsString()
  trainer_id?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  password?: string;
}

class TrainerShiftCompatDto {
  @IsArray()
  @IsString({ each: true })
  days: string[];

  @IsString()
  @IsNotEmpty()
  start_time: string;

  @IsString()
  @IsNotEmpty()
  end_time: string;
}

class TrainerPermissionsCompatDto {
  @IsOptional()
  add_members?: boolean;
  @IsOptional()
  add_clients?: boolean;
  @IsOptional()
  view_dashboard?: boolean;
  @IsOptional()
  show_dashboard?: boolean;
  @IsOptional()
  view_payments?: boolean;
  @IsOptional()
  show_payments?: boolean;
  @IsOptional()
  show_payment_in_details?: boolean;
  @IsOptional()
  view_member_details?: boolean;
  @IsOptional()
  add_trainer?: boolean;
}

export class CreateTrainerCompatDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsString()
  @IsNotEmpty()
  phone: string;

  @IsString()
  @IsNotEmpty()
  full_name: string;

  @IsOptional()
  @IsString()
  dob?: string;

  @IsOptional()
  @IsIn(['male', 'female', 'other'])
  gender?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  experience?: string;

  @IsOptional()
  @IsString()
  address?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  profile_image?: string;

  @IsOptional()
  @IsNumber()
  @Min(0)
  salary?: number;

  @IsOptional()
  @IsIn(['monthly', 'weekly', 'hourly', 'yearly'])
  salary_type?: 'monthly' | 'weekly' | 'hourly' | 'yearly';

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  expertise?: string[];

  @IsOptional()
  @ValidateNested()
  @Type(() => TrainerShiftCompatDto)
  shift?: TrainerShiftCompatDto;

  @IsOptional()
  @ValidateNested()
  @Type(() => TrainerCredentialsDto)
  credentials?: TrainerCredentialsDto;

  @IsOptional()
  @ValidateNested()
  @Type(() => TrainerPermissionsCompatDto)
  permissions?: TrainerPermissionsCompatDto;
}
