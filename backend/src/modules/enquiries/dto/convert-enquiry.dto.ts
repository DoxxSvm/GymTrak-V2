import { Transform, Type } from 'class-transformer';
import {
  IsIn,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';
import { InitialMemberSubscriptionDto } from '../../members/dto/create-member.dto';

export class ConvertEnquiryDto {
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  fullNameOverride?: string;

  @IsOptional()
  @IsString()
  phoneOverride?: string;

  @IsOptional()
  @IsString()
  emailOverride?: string;

  /** When set, replaces enquiry `photoUrl` and is written to the new member’s `User.avatarUrl`. Omit to use the enquiry’s current `photoUrl`. */
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  photoUrlOverride?: string;

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
  notes?: string;

  @IsOptional()
  @IsString()
  @IsIn(['male', 'female', 'other', 'Male', 'Female', 'Other'])
  gender?: string;

  @IsOptional()
  @IsString()
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
  @ValidateNested()
  @Type(() => InitialMemberSubscriptionDto)
  initialSubscription?: InitialMemberSubscriptionDto;
}
