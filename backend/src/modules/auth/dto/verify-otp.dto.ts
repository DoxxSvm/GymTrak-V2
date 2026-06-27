import { IsEnum, IsNotEmpty, IsString, Length, Matches } from 'class-validator';
import { OtpPurpose } from '@prisma/client';

export class VerifyOtpDto {
  @IsNotEmpty()
  @Matches(/^\+[1-9]\d{1,14}$/, {
    message: 'phone must be E.164 (e.g. +15551234567)',
  })
  phone: string;

  @IsString()
  @Length(4, 8)
  code: string;

  @IsEnum(OtpPurpose)
  purpose: OtpPurpose;
}
