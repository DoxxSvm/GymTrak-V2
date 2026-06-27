import {
  IsNotEmpty,
  IsOptional,
  IsString,
  Length,
  Matches,
} from 'class-validator';

export class MobileVerifyOtpDto {
  @IsNotEmpty()
  @IsString()
  @Matches(/^\d{6,15}$/, {
    message: 'phone must contain 6 to 15 digits',
  })
  phone: string;

  @IsOptional()
  @IsString()
  @Matches(/^\+[1-9]\d{0,4}$/, {
    message: 'country_code must start with + and be valid',
  })
  country_code?: string;

  @IsNotEmpty()
  @IsString()
  @Length(4, 8)
  otp: string;
}
