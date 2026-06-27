import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  MaxLength,
  Min,
} from 'class-validator';

export enum WhatsAppTestMessageType {
  ONBOARDING = 'onboarding',
  PAYMENT_CONFIRMATION = 'payment_confirmation',
  TEXT = 'text',
}

export class TestWhatsAppMessageDto {
  @ApiProperty({
    enum: WhatsAppTestMessageType,
    example: WhatsAppTestMessageType.ONBOARDING,
    description:
      '`onboarding` → Meta template via `/marketing_messages`; `payment_confirmation` → Meta template via `/messages`; `text` → plain text.',
  })
  @IsEnum(WhatsAppTestMessageType)
  type!: WhatsAppTestMessageType;

  @ApiPropertyOptional({
    example: '918130916940',
    description:
      'E.164 or digits. Required when `member_id` is omitted.',
  })
  @IsOptional()
  @IsString()
  phone?: string;

  @ApiPropertyOptional({
    description:
      'Gym member `GymUser.id`; phone and name resolved from the linked user.',
  })
  @IsOptional()
  @IsString()
  member_id?: string;

  @ApiPropertyOptional({
    example: 'Shivam',
    description: 'Overrides member display name in template parameters.',
  })
  @IsOptional()
  @IsString()
  @MaxLength(120)
  memberName?: string;

  @ApiPropertyOptional({
    example: 150000,
    description:
      'Payment amount in smallest currency unit (e.g. paise). Default `150000` (₹1,500) for `payment_confirmation`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  amountCents?: number;

  @ApiPropertyOptional({
    example: 'INR',
    description: 'Default `INR` for `payment_confirmation`.',
  })
  @IsOptional()
  @IsString()
  @MaxLength(8)
  currency?: string;

  @ApiPropertyOptional({
    example: 'GymTrak monthly membership',
    description: 'Default `Gym membership` for `payment_confirmation`.',
  })
  @IsOptional()
  @IsString()
  @MaxLength(200)
  planLabel?: string;

  @ApiPropertyOptional({
    description:
      'Plain text body when `type` is `text`. Defaults to a sample expiry-reminder message.',
  })
  @IsOptional()
  @IsString()
  @MaxLength(4000)
  message?: string;
}
