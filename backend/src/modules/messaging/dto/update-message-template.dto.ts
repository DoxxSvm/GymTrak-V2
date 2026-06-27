import { MessageTemplateKind } from '@prisma/client';
import {
  IsBoolean,
  IsEnum,
  IsOptional,
  IsString,
  MaxLength,
} from 'class-validator';

export class UpdateMessageTemplateDto {
  @IsEnum(MessageTemplateKind)
  kind!: MessageTemplateKind;

  @IsOptional()
  @IsBoolean()
  enabled?: boolean;

  @IsOptional()
  @IsString()
  @MaxLength(4000)
  overrideBody?: string | null;
}
