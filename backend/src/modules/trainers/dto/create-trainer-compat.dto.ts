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
import { TrainerPermissionsDto } from './trainer-permissions.dto';
import { TrainerShiftDto } from './trainer-shift.dto';
import { GymRole } from '@prisma/client';

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

class TrainerPermissionsCompatDto extends TrainerPermissionsDto {
  @IsOptional()
  add_members?: boolean;

  @IsOptional()
  view_dashboard?: boolean;

  @IsOptional()
  view_payments?: boolean;

  @IsOptional()
  view_member_details?: boolean;
}

export class CreateTrainerCompatDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  /** Omit or `TRAINER` (default); use `STAFF` for reception / admin staff — same response shape as trainer. */
  @IsOptional()
  @IsIn([GymRole.TRAINER, GymRole.STAFF])
  role?: GymRole;

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

  /** Same as DB `TrainerShift` (0=Sun..6=Sat). If non-empty, legacy `shift` is ignored. */
  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => TrainerShiftDto)
  shifts?: TrainerShiftDto[];

  @IsOptional()
  @ValidateNested()
  @Type(() => TrainerShiftCompatDto)
  shift?: TrainerShiftCompatDto;

  @IsOptional()
  @ValidateNested()
  @Type(() => TrainerCredentialsDto)
  credentials?: TrainerCredentialsDto;

  @IsOptional()
  // @ValidateNested()
  // @Type(() => TrainerPermissionsCompatDto)
  permissions?: string[];
}
