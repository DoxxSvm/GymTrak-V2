import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsNotEmpty, IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export type LeaderboardType = 'attendance' | 'workout';

export class LeaderboardQueryDto extends GymIdQueryDto {
  @ApiProperty({ enum: ['attendance', 'workout'] })
  @IsString()
  @IsNotEmpty()
  @IsIn(['attendance', 'workout'])
  type!: LeaderboardType;

  @ApiPropertyOptional({
    description: '1-based page (default 1). Coerced from string query.',
    example: '1',
  })
  @IsOptional()
  @IsString()
  page?: string;

  @ApiPropertyOptional({
    description: 'Page size (default 20, max 100). Coerced from string query.',
    example: '20',
  })
  @IsOptional()
  @IsString()
  limit?: string;
}
