import { IsNotEmpty, IsOptional, IsString, Matches } from 'class-validator';

export class MobileSendOtpDto {
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
}
