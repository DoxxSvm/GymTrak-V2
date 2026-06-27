import { ApiProperty } from '@nestjs/swagger';
import { IsString, MinLength } from 'class-validator';

export class SendTestNotificationDto {
  @ApiProperty({
    example: '+919999999999',
    description:
      'Phone number to receive the test. Must belong to the authenticated user unless the caller is SUPER_ADMIN.',
  })
  @IsString()
  @MinLength(1)
  phone!: string;
}
