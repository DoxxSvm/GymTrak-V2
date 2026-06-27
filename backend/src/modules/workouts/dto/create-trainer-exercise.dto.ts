import { ExerciseEquipment, ExerciseType, Muscle } from '@prisma/client';
import { IsArray, IsBoolean, IsEnum, IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateTrainerExerciseDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  asset_url?: string;

  @IsEnum(ExerciseEquipment)
  equipment: ExerciseEquipment;

  @IsEnum(Muscle)
  primary_muscle: Muscle;

  @IsOptional()
  @IsArray()
  @IsEnum(Muscle, { each: true })
  secondary_muscles?: Muscle[];

  @IsEnum(ExerciseType)
  exercise_type: ExerciseType;

  @IsOptional()
  @IsBoolean()
  is_active?: boolean;
}
