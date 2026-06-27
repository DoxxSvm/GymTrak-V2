import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  MinLength,
} from 'class-validator';

export const MEMBER_ONBOARDING_GENDERS = ['MALE', 'FEMALE', 'OTHER'] as const;
export const MEMBER_ONBOARDING_ACTIVITY = ['LOW', 'MODERATE', 'HIGH'] as const;

export class MemberOnboardingDto {
  @IsString()
  @IsNotEmpty()
  @MinLength(1)
  @MaxLength(120)
  fullName: string;

  @Type(() => Number)
  @IsNumber()
  @Min(50)
  @Max(280)
  heightCm: number;

  @Type(() => Number)
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(13)
  @Max(120)
  ageYears?: number;

  @IsOptional()
  @IsString()
  @IsIn(MEMBER_ONBOARDING_GENDERS)
  gender?: (typeof MEMBER_ONBOARDING_GENDERS)[number];

  @IsOptional()
  @IsString()
  @IsIn(MEMBER_ONBOARDING_ACTIVITY)
  activityLevel?: (typeof MEMBER_ONBOARDING_ACTIVITY)[number];

  @IsOptional()
  @IsString()
  @MaxLength(64)
  fitnessGoal?: string;
}
