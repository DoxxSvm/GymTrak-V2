import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  ArrayMinSize,
  IsArray,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';

export class WorkoutSetDto {
  @ApiProperty({ example: 1, minimum: 1 })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  set_number: number;

  @ApiPropertyOptional({ example: 10 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  reps?: number;

  @ApiPropertyOptional({ example: 60 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  weight?: number;
}

export class WorkoutExerciseDto {
  @ApiProperty({
    description: 'Exercise id (gym catalog or member personal exercise id)',
  })
  @IsString()
  @IsNotEmpty()
  exercise_id: string;

  @ApiProperty({ type: () => WorkoutSetDto, isArray: true })
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => WorkoutSetDto)
  sets: WorkoutSetDto[];
}

export class CreateWorkoutDto {
  @ApiProperty({
    description:
      'Optional. GymUser id of the member — when set, creates a gym workout (`user_id` = trainer JWT `sub`, `gym_user_id` = this id). Gym comes from this row; optional `?gymId=` must match that gym if provided.',
  })
  @IsString()
  @IsOptional()
  member_id?: string;

  @ApiProperty({ example: 'Push day' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(160)
  title: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;

  @ApiPropertyOptional({
    enum: ['trainer', 'member'],
    example: 'trainer',
    description:
      'Who created this workout: `trainer` (OWNER/TRAINER/STAFF) or `member`. Omit to use the JWT actor’s resolved gym role.',
  })
  @IsOptional()
  @IsIn(['trainer', 'member'])
  created_by?: 'trainer' | 'member';

  @ApiProperty({ type: () => WorkoutExerciseDto, isArray: true })
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => WorkoutExerciseDto)
  exercises: WorkoutExerciseDto[];
}
