import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, MaxLength } from 'class-validator';

export class ProductListQueryDto {
  @ApiPropertyOptional({ description: 'Target gym (`gym_id` also accepted)' })
  @IsOptional()
  @IsString()
  gymId?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  gym_id?: string;

  @ApiPropertyOptional({ default: 1 })
  @IsOptional()
  @IsString()
  page?: string;

  @ApiPropertyOptional({ default: 10 })
  @IsOptional()
  @IsString()
  limit?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(200)
  search?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(100)
  category?: string;

  @ApiPropertyOptional({
    description:
      'true when caller can manage catalog to include inactive products',
  })
  @IsOptional()
  @IsString()
  include_inactive?: string;
}
