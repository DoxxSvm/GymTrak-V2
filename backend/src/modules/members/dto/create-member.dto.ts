import { Transform, Type } from 'class-transformer';
import {
  IsBoolean,
  IsEmail,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  MinLength,
  ValidateIf,
  ValidateNested,
} from 'class-validator';

export class InitialMemberSubscriptionDto {
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  planId?: string;

  @IsOptional()
  @IsString()
  @IsNotEmpty()
  gymPlanId?: string;

  @IsString()
  @IsNotEmpty()
  startsAt: string;

  @IsString()
  @IsNotEmpty()
  endsAt: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  priceCents?: number;

  @IsOptional()
  @IsString()
  @MaxLength(8)
  currency?: string;
}

/** Full add-member payload; optional / “expandable” fields are optional at root level */
export class CreateMemberDto {
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  gymId?: string;

  @IsString()
  @IsNotEmpty()
  @MinLength(6)
  @MaxLength(24)
  phone: string;

  @IsOptional()
  @IsString()
  @IsNotEmpty()
  @MinLength(1)
  @MaxLength(120)
  fullName?: string;

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
  @Type(() => Number)
  @IsNumber()
  @Min(50)
  @Max(280)
  heightCm?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg?: number;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  emergencyContactName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(24)
  emergencyContactPhone?: string;

  @IsOptional()
  @IsString()
  dateOfBirth?: string;

  @IsOptional()
  @IsString()
  dob?: string;

  @IsOptional()
  @IsString()
  @MaxLength(32)
  gender?: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  address?: string;

  @IsOptional()
  @Transform(({ value }) => {
    if (value === null || value === undefined) return undefined;
    if (typeof value === 'string' && value.trim() === '') return undefined;
    return typeof value === 'string' ? value.replace(/\D/g, '') : value;
  })
  @ValidateIf((_, v) => v !== undefined && v !== null && v !== '')
  @IsString()
  @Matches(/^\d{12}$/, {
    message: 'aadhaar_number must be exactly 12 digits when provided',
  })
  aadhaar_number?: string;

  @IsOptional()
  @IsString()
  date_of_joining?: string;

  @IsOptional()
  @IsString()
  membershipEndsAt?: string;

  @IsOptional()
  @ValidateNested()
  @Type(() => InitialMemberSubscriptionDto)
  initialSubscription?: InitialMemberSubscriptionDto;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  avatarUrl?: string;
}
