import { SalaryPeriod } from '@prisma/client';
import { Transform, Type } from 'class-transformer';
import {
  IsArray,
  IsEnum,
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { TrainerShiftDto } from '../../trainers/dto/trainer-shift.dto';

function trimGender(v: unknown): string | undefined {
  if (v == null || typeof v !== 'string') return undefined;
  const s = v.trim().toLowerCase();
  if (s === 'male' || s === 'female' || s === 'other') return s;
  return undefined;
}

/** Trainer self-service profile — phone / email not accepted here. */
export class UpdateMeTrainerDto {
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  avatarUrl?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  profile_image?: string;

  @IsOptional()
  @IsString()
  @MaxLength(60)
  firstName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(60)
  lastName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  fullName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(40)
  dateOfBirth?: string;

  @IsOptional()
  @Transform(({ value }) => trimGender(value))
  @IsString()
  @Matches(/^(male|female|other)$/)
  gender?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  experience?: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  address?: string;

  /** Salary in major units (e.g. USD dollars). Ignored if `salaryCents` is set. */
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  @Max(1_000_000)
  salary?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  salaryCents?: number;

  @IsOptional()
  @IsEnum(SalaryPeriod)
  salaryPeriod?: SalaryPeriod;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  expertise?: string[];

  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => TrainerShiftDto)
  shifts?: TrainerShiftDto[];
}
