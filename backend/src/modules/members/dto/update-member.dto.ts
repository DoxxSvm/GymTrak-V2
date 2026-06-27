import { Transform, Type } from 'class-transformer';
import {
  IsBoolean,
  IsEmail,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  MinLength,
  ValidateIf,
} from 'class-validator';

export class UpdateMemberDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  @MaxLength(120)
  fullName?: string;

  /** Same as **`POST /members`** create (`first_name` + `last_name` merge when `fullName` omitted). */
  @IsOptional()
  @IsString()
  @MaxLength(60)
  first_name?: string;

  @IsOptional()
  @IsString()
  @MaxLength(60)
  last_name?: string;

  @IsOptional()
  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim().toLowerCase() : value,
  )
  @IsEmail()
  @MaxLength(254)
  email?: string;

  @IsOptional()
  @IsBoolean()
  isLead?: boolean;

  @IsOptional()
  @IsBoolean()
  isActive?: boolean;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(50)
  @Max(280)
  heightCm?: number;

  /** Snake_case alias of **`heightCm`** (mobile clients). */
  @IsOptional()
  height_cm?: number | string;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg?: number;

  /** Snake_case alias of **`weightKg`** (mobile clients). */
  @IsOptional()
  weight_kg?: number | string;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(1)
  @Max(150)
  age?: number;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  profile_image?: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  fitnessGoal?: string;

  /** Consumer TDEE tier (`LOW` | `MODERATE` | `HIGH`); stored on `User.activityLevel`. */
  @IsOptional()
  @IsString()
  @Matches(/^(LOW|MODERATE|HIGH)$/i, {
    message: 'activityLevel must be LOW, MODERATE, or HIGH',
  })
  activityLevel?: string;

  /** Snake_case alias of **`activityLevel`** (mobile clients). */
  @IsOptional()
  @IsString()
  @Matches(/^(LOW|MODERATE|HIGH)$/i, {
    message: 'activity_level must be LOW, MODERATE, or HIGH',
  })
  activity_level?: string;

  /**
   * User-provided maintenance kcal/day — persisted and returned on GET/PATCH/PUT profile.
   */
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  maintenanceCalories?: number;

  /** Snake_case alias of **`maintenanceCalories`**. */
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  maintenance_calories?: number;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;

  /** Residential address on **`User.address`**; legacy `Address:` lines in **`notes`** are stripped on update. */
  @IsOptional()
  @Transform(({ value }) => {
    if (value === null) return null;
    if (value === undefined) return undefined;
    if (typeof value === 'string' && value.trim() === '') return null;
    return typeof value === 'string' ? value.trim() : value;
  })
  @ValidateIf((_, v) => v !== null && v !== undefined && v !== '')
  @IsString()
  @MaxLength(500)
  address?: string | null;

  @IsOptional()
  @Transform(({ value }) => {
    if (value === null) return null;
    if (value === undefined) return undefined;
    if (typeof value === 'string' && value.trim() === '') return null;
    return typeof value === 'string' ? value.replace(/\D/g, '') : value;
  })
  @ValidateIf((_, v) => v !== null && v !== undefined && v !== '')
  @IsString()
  @Matches(/^\d{12}$/, {
    message: 'aadhaar_number must be exactly 12 digits when provided',
  })
  aadhaar_number?: string | null;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  emergencyContactName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  emergency_name?: string;

  @IsOptional()
  @IsString()
  @MaxLength(24)
  emergencyContactPhone?: string;

  @IsOptional()
  @IsString()
  @MaxLength(24)
  emergency_contact_phone?: string;

  @IsOptional()
  @IsString()
  dateOfBirth?: string;

  /** Alias of **`dateOfBirth`** (create-member parity). */
  @IsOptional()
  @IsString()
  dob?: string;

  @IsOptional()
  @IsString()
  @MaxLength(32)
  gender?: string;

  @IsOptional()
  @IsString()
  membershipEndsAt?: string;

  /** Moves `GymUser.joinedAt` when **`gymId`** is supplied (ignored on gym-free self PATCH). */
  @IsOptional()
  @IsString()
  date_of_joining?: string;

  /** Same as **`profile_image`**; create parity with **`avatarUrl`**. */
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  avatarUrl?: string;
}
