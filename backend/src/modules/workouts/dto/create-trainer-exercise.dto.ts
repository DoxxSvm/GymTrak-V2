import { ExerciseEquipment, ExerciseType, Muscle } from '@prisma/client';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsArray,
  IsBoolean,
  IsEnum,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
} from 'class-validator';

export class CreateTrainerExerciseDto {
  @ApiProperty({ example: 'Barbell bench press' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name: string;

  @ApiPropertyOptional({ example: 'https://cdn.example.com/bench.mp4' })
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  asset_url?: string;

  @ApiProperty({ enum: ExerciseEquipment, example: ExerciseEquipment.BARBELL })
  @IsEnum(ExerciseEquipment)
  equipment: ExerciseEquipment;

  @ApiProperty({ enum: Muscle, example: Muscle.CHEST })
  @IsEnum(Muscle)
  primary_muscle: Muscle;

  @ApiPropertyOptional({
    enum: Muscle,
    isArray: true,
    example: [Muscle.TRICEPS],
  })
  @IsOptional()
  @IsArray()
  @IsEnum(Muscle, { each: true })
  secondary_muscles?: Muscle[];

  @ApiProperty({ enum: ExerciseType, example: ExerciseType.WEIGHT_REPS })
  @IsEnum(ExerciseType)
  exercise_type: ExerciseType;

  @ApiPropertyOptional({ default: true })
  @IsOptional()
  @IsBoolean()
  is_active?: boolean;
}
