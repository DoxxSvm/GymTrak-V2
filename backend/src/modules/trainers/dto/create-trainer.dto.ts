import { Type } from 'class-transformer';
import { GymRole, SalaryPeriod } from '@prisma/client';
import {
  IsArray,
  IsBoolean,
  IsEmail,
  IsEnum,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  ValidateNested,
} from 'class-validator';
import { TrainerShiftDto } from './trainer-shift.dto';

export class CreateTrainerDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsOptional()
  @IsEnum(GymRole)
  role?: GymRole;

  @IsString()
  @IsNotEmpty()
  phone: string;

  @IsString()
  @IsNotEmpty()
  fullName: string;

  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  avatarUrl?: string;

  @IsOptional()
  @IsString()
  dateOfBirth?: string;

  @IsOptional()
  @IsString()
  @IsIn(['male', 'female', 'other'])
  gender?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  experience?: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  address?: string;

  /** Expertise labels (tags are created per gym as needed). */
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  expertise?: string[];

  @IsOptional()
  @IsInt()
  salaryCents?: number;

  @IsOptional()
  @IsEnum(SalaryPeriod)
  salaryPeriod?: SalaryPeriod;

  @IsOptional()
  @IsString()
  contractStartsAt?: string;

  @IsOptional()
  @IsString()
  contractEndsAt?: string;

  @IsOptional()
  @IsString()
  notes?: string;

  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => TrainerShiftDto)
  shifts?: TrainerShiftDto[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  planIds?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  permissions?: string[];

  /** When true, response includes one-time `username` + `password` for `POST /auth/staff/login`. */
  @IsOptional()
  @IsBoolean()
  generateLoginCredentials?: boolean;

  /** Optional explicit username for `POST /auth/staff/login`. */
  @IsOptional()
  @IsString()
  @MaxLength(120)
  username?: string;

  /** Optional explicit password for `POST /auth/staff/login`. */
  @IsOptional()
  @IsString()
  @MaxLength(120)
  password?: string;
}
