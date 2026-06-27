import { Type } from 'class-transformer';
import {
  IsIn,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  Min,
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
  @ValidateNested()
  @Type(() => InitialMemberSubscriptionDto)
  initialSubscription?: InitialMemberSubscriptionDto;
}
