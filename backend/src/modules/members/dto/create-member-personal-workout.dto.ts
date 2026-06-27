import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  ArrayMinSize,
  IsArray,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  ValidateNested,
} from 'class-validator';
import { WorkoutExerciseDto } from '../../workouts/dto/create-workout.dto';

/** Same shape as trainer `CreateWorkoutDto` except no `member_id` — scoped to JWT `sub`. */
export class CreateMemberPersonalWorkoutDto {
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

  @ApiProperty({
    type: () => WorkoutExerciseDto,
    isArray: true,
    description: 'Use `exercise_id` values from `GET /members/exercises`',
  })
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => WorkoutExerciseDto)
  exercises: WorkoutExerciseDto[];
}
