import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, MaxLength } from 'class-validator';

export class RegisterDeviceTokenDto {
  /** FCM registration token; omit or empty string to unregister this device */
  @ApiPropertyOptional({
    description:
      'FCM device token from the mobile SDK. Send empty string to clear.',
    maxLength: 4096,
  })
  @IsOptional()
  @IsString()
  @MaxLength(4096)
  token?: string;
}
