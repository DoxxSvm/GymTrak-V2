import { ExerciseEquipment, ExerciseType, Muscle } from '@prisma/client';
import { ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsArray,
  IsBoolean,
  IsEnum,
  IsOptional,
  IsString,
  MaxLength,
} from 'class-validator';

/** Partial update — send only fields to change. */
export class UpdateTrainerExerciseDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(200)
  name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  asset_url?: string;

  @ApiPropertyOptional({ enum: ExerciseEquipment })
  @IsOptional()
  @IsEnum(ExerciseEquipment)
  equipment?: ExerciseEquipment;

  @ApiPropertyOptional({ enum: Muscle })
  @IsOptional()
  @IsEnum(Muscle)
  primary_muscle?: Muscle;

  @ApiPropertyOptional({ enum: Muscle, isArray: true })
  @IsOptional()
  @IsArray()
  @IsEnum(Muscle, { each: true })
  secondary_muscles?: Muscle[];

  @ApiPropertyOptional({ enum: ExerciseType })
  @IsOptional()
  @IsEnum(ExerciseType)
  exercise_type?: ExerciseType;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  is_active?: boolean;
}
