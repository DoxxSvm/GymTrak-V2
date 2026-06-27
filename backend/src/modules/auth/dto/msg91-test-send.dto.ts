import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, Matches } from 'class-validator';

export class Msg91TestSendDto {
  @ApiProperty({
    example: '6353491081',
    description: 'National number (10 digits for India) or full international digits',
  })
  @IsString()
  @Matches(/^\d{6,15}$/, { message: 'phone must contain 6 to 15 digits' })
  phone!: string;

  @ApiPropertyOptional({ example: '+91', default: '+91' })
  @IsOptional()
  @IsString()
  @Matches(/^\+[1-9]\d{0,4}$/, {
    message: 'country_code must start with + and be valid',
  })
  country_code?: string;
}
