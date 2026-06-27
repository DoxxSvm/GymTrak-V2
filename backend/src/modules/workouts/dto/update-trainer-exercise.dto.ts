import { ExerciseEquipment, ExerciseType, Muscle } from '@prisma/client';
import { IsArray, IsBoolean, IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';

/** Partial update — send only fields to change. */
export class UpdateTrainerExerciseDto {
  @IsOptional()
  @IsString()
  @MaxLength(200)
  name?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  asset_url?: string;

  @IsOptional()
  @IsEnum(ExerciseEquipment)
  equipment?: ExerciseEquipment;

  @IsOptional()
  @IsEnum(Muscle)
  primary_muscle?: Muscle;

  @IsOptional()
  @IsArray()
  @IsEnum(Muscle, { each: true })
  secondary_muscles?: Muscle[];

  @IsOptional()
  @IsEnum(ExerciseType)
  exercise_type?: ExerciseType;

  @IsOptional()
  @IsBoolean()
  is_active?: boolean;
}
