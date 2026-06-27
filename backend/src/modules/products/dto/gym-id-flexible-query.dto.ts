import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

/** Query: `gymId` or `gym_id` (validated in service). */
export class GymIdFlexibleQueryDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  gymId?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  gym_id?: string;
}
