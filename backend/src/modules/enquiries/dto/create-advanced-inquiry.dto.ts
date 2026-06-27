import {
  IsIn,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
} from 'class-validator';

export class CreateAdvancedInquiryDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(80)
  first_name: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(80)
  last_name: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(24)
  phone: string;

  @IsOptional()
  @IsString()
  date?: string;

  @IsOptional()
  @IsString()
  @IsIn(['male', 'female', 'other'])
  gender?: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  address?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  photo_url?: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  source?: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  medium?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  interest?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  interested_in?: string;

  @IsOptional()
  @IsString()
  @MaxLength(4000)
  notes?: string;
}
