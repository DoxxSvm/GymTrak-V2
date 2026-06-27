import { ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsBoolean,
  IsIn,
  IsOptional,
  IsString,
  MaxLength,
} from 'class-validator';

export class UpdateTrainerWorkoutDto {
  @ApiPropertyOptional({ maxLength: 160 })
  @IsOptional()
  @IsString()
  @MaxLength(160)
  title?: string;

  @ApiPropertyOptional({ maxLength: 2000 })
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;

  @ApiPropertyOptional({
    enum: ['trainer', 'member'],
    example: 'trainer',
    description:
      'Update creator bucket: `trainer` (OWNER/TRAINER/STAFF) or `member` (MEMBER).',
  })
  @IsOptional()
  @IsIn(['trainer', 'member'])
  created_by?: 'trainer' | 'member';

  @ApiPropertyOptional({ type: Boolean })
  @IsOptional()
  @IsBoolean()
  completed?: boolean;

  @ApiPropertyOptional({ type: Boolean })
  @IsOptional()
  @IsBoolean()
  isSaved?: boolean;
}
