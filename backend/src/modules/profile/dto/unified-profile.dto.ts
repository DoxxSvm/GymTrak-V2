import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import {
  ArrayMinSize,
  IsArray,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';

export const UNIFIED_PROFILE_ROLES = ['gym_owner', 'trainer'] as const;
export type UnifiedProfileRoleDto = (typeof UNIFIED_PROFILE_ROLES)[number];

export const UNIFIED_SALARY_DURATIONS = ['month', 'week', 'year'] as const;
export type UnifiedSalaryDurationDto =
  (typeof UNIFIED_SALARY_DURATIONS)[number];

export class UnifiedPersonalInfoInputDto {
  @ApiPropertyOptional({ maxLength: 60 })
  @IsOptional()
  @IsString()
  @MaxLength(60)
  firstName?: string;

  @ApiPropertyOptional({ maxLength: 60 })
  @IsOptional()
  @IsString()
  @MaxLength(60)
  lastName?: string;

  @ApiPropertyOptional({ maxLength: 120 })
  @IsOptional()
  @IsString()
  @MaxLength(120)
  fullName?: string;

  @ApiPropertyOptional({ description: 'Pre-uploaded image URL' })
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  profileImage?: string;

  @ApiPropertyOptional({ example: '1990-05-20' })
  @IsOptional()
  @IsString()
  @MaxLength(40)
  dateOfBirth?: string;

  @ApiPropertyOptional({ enum: ['male', 'female', 'other'] })
  @IsOptional()
  @IsString()
  @Matches(/^(male|female|other)$/)
  gender?: string;

  @ApiPropertyOptional({ maxLength: 500 })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  address?: string;
}

export class UnifiedGymDetailsInputDto {
  @ApiPropertyOptional({ maxLength: 200 })
  @IsOptional()
  @IsString()
  @MaxLength(200)
  gymName?: string;

  @ApiPropertyOptional({ maxLength: 500 })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  gymAddress?: string;

  @ApiPropertyOptional({
    description:
      '15-character GSTIN (required when sending other gymDetails fields as gym_owner)',
    maxLength: 15,
  })
  @IsOptional()
  @Transform(({ value }) => (value === '' ? undefined : value))
  @IsString()
  @MaxLength(15)
  @Matches(/^[0-9A-Z]{15}$/i, {
    message: 'gstNumber must be 15 alphanumeric characters',
  })
  gstNumber?: string;

  @ApiPropertyOptional({ description: 'Pre-uploaded logo URL' })
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  gymLogo?: string;
}

export class UnifiedTrainerShiftInputDto {
  @ApiPropertyOptional({ example: 'Morning' })
  @IsOptional()
  @IsString()
  @MaxLength(80)
  name?: string;

  @ApiProperty({
    minimum: 0,
    maximum: 6,
    description: '0 = Sunday … 6 = Saturday',
  })
  @IsInt()
  @Min(0)
  @Max(6)
  dayOfWeek: number;

  @ApiProperty({ example: '09:00' })
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  startTime: string;

  @ApiProperty({ example: '17:00' })
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  endTime: string;
}

export class UnifiedTrainerDetailsInputDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(120)
  experience?: string;

  @ApiPropertyOptional({
    description: 'Salary in major units (e.g. rupees / dollars)',
  })
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(1_000_000)
  salary?: number;

  @ApiPropertyOptional({ enum: UNIFIED_SALARY_DURATIONS })
  @IsOptional()
  @IsEnum(UNIFIED_SALARY_DURATIONS)
  salaryDuration?: UnifiedSalaryDurationDto;

  @ApiPropertyOptional({ type: [String] })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  expertise?: string[];

  @ApiPropertyOptional({
    type: [UnifiedTrainerShiftInputDto],
    description:
      'When this object is sent, include at least one shift (dayOfWeek + HH:mm times).',
  })
  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => UnifiedTrainerShiftInputDto)
  @ArrayMinSize(1, {
    message:
      'trainerDetails.shifts must contain at least one shift when provided',
  })
  shifts?: UnifiedTrainerShiftInputDto[];
}

export class UpdateUnifiedProfileDto {
  @ApiProperty({ enum: UNIFIED_PROFILE_ROLES })
  @IsEnum(UNIFIED_PROFILE_ROLES)
  @IsNotEmpty()
  role: UnifiedProfileRoleDto;

  @ApiPropertyOptional({ type: UnifiedPersonalInfoInputDto })
  @IsOptional()
  @ValidateNested()
  @Type(() => UnifiedPersonalInfoInputDto)
  personalInfo?: UnifiedPersonalInfoInputDto;

  @ApiPropertyOptional({
    type: UnifiedGymDetailsInputDto,
    description:
      'Ignored when role is trainer. GST required when any gym field is set.',
  })
  @IsOptional()
  @ValidateNested()
  @Type(() => UnifiedGymDetailsInputDto)
  gymDetails?: UnifiedGymDetailsInputDto;

  @ApiPropertyOptional({
    type: UnifiedTrainerDetailsInputDto,
    description: 'Ignored when role is gym_owner.',
  })
  @IsOptional()
  @ValidateNested()
  @Type(() => UnifiedTrainerDetailsInputDto)
  trainerDetails?: UnifiedTrainerDetailsInputDto;
}
