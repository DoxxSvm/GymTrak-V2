import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, MaxLength } from 'class-validator';

/** Begins a live session (`startedAt` set, no exercises yet). */
export class StartMemberPersonalWorkoutDto {
  @ApiPropertyOptional({ example: 'Leg day' })
  @IsOptional()
  @IsString()
  @MaxLength(160)
  title?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;
}
