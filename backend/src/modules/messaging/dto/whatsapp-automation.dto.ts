import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  ArrayMinSize,
  IsArray,
  IsBoolean,
  IsIn,
  IsOptional,
  IsString,
  MaxLength,
  ValidateNested,
} from 'class-validator';
import type { WhatsAppAutomationTemplateId } from '../whatsapp-automation.config';

const AUTOMATION_TEMPLATE_IDS = [
  'onboarding_welcome',
  'expire_7_days',
  'expire_3_days',
  'expired_reminder',
  'payment_received',
] as const;

export class WhatsAppAutomationTemplateItemDto {
  @ApiProperty({
    enum: AUTOMATION_TEMPLATE_IDS,
    example: 'onboarding_welcome',
  })
  @IsIn(AUTOMATION_TEMPLATE_IDS)
  id!: WhatsAppAutomationTemplateId;

  @ApiProperty({ example: true })
  @IsBoolean()
  enabled!: boolean;

  @ApiPropertyOptional({
    description:
      'Custom welcome text (onboarding_welcome only). Omit or null to use server default.',
    example: 'Welcome to our gym family!',
    maxLength: 4000,
  })
  @IsOptional()
  @IsString()
  @MaxLength(4000)
  message?: string | null;
}

export class UpdateWhatsAppAutomationDto {
  @ApiProperty({
    type: [WhatsAppAutomationTemplateItemDto],
    description:
      'All five automation toggles from the WhatsApp Automation screen. Send the full list on save.',
  })
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => WhatsAppAutomationTemplateItemDto)
  templates!: WhatsAppAutomationTemplateItemDto[];
}
