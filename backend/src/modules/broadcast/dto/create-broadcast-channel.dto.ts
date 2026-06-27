import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateBroadcastChannelDto {
  @ApiProperty({ description: 'Target gym id (caller must have access)' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @ApiProperty({ maxLength: 200, example: 'Elite Gym' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name: string;

  @ApiPropertyOptional({ maxLength: 2000, description: 'Channel description' })
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  description?: string;

  @ApiPropertyOptional({ description: 'URL to channel image/avatar' })
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  imageUrl?: string;
}
