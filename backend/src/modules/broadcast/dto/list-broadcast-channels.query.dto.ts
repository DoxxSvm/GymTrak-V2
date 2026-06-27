import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  Min,
} from 'class-validator';

export class ListBroadcastChannelsQueryDto {
  @ApiProperty({ description: 'Gym id (CUID)' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @ApiPropertyOptional({ example: 1, description: '1-based page (default 1)' })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number;

  @ApiPropertyOptional({
    example: 20,
    description: 'Page size (default 20, max 100)',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number;

  @ApiPropertyOptional({
    description: 'Filter channel names (case-insensitive contains)',
  })
  @IsOptional()
  @IsString()
  search?: string;
}
