import { IsEnum, IsNotEmpty, Matches } from 'class-validator';
import { OtpPurpose } from '@prisma/client';

export class SendOtpDto {
  @IsNotEmpty()
  @Matches(/^\+[1-9]\d{1,14}$/, {
    message: 'phone must be E.164 (e.g. +15551234567)',
  })
  phone: string;

  @IsEnum(OtpPurpose)
  purpose: OtpPurpose;
}
