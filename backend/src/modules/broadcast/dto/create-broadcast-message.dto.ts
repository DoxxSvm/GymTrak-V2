import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateBroadcastMessageDto {
  @ApiProperty({ maxLength: 200, example: 'Reminder' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  title: string;

  @ApiPropertyOptional({ maxLength: 8000, description: 'Message body' })
  @IsOptional()
  @IsString()
  @MaxLength(8000)
  description?: string;

  @ApiPropertyOptional({ description: 'Image attached to the broadcast' })
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  imageUrl?: string;
}
