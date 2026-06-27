import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  ArrayMinSize,
  IsArray,
  IsOptional,
  IsString,
  ValidateNested,
} from 'class-validator';
import { WorkoutExerciseDto } from './create-workout.dto';
import { UpdateTrainerWorkoutDto } from './update-trainer-workout.dto';

/**
 * Body for `PATCH /workouts` — partial `title` / `notes` / flags, or full exercise tree
 * when `exercises` is set (replaces all blocks and sets on the plan).
 */
export class PatchWorkoutBodyDto extends UpdateTrainerWorkoutDto {
  @ApiProperty({
    description: 'Member workout plan id (`member_workout_plans.id`).',
  })
  @IsString()
  workout_id!: string;

  @ApiPropertyOptional({
    description:
      'GymUser id — when replacing exercises on a gym workout, must match the plan’s `gym_user_id` if sent.',
  })
  @IsOptional()
  @IsString()
  member_id?: string;

  @ApiPropertyOptional({
    type: WorkoutExerciseDto,
    isArray: true,
    description:
      'When set (non-empty), replaces all exercises and sets (same shape as `POST /workouts`). Omit `title` to keep the current title.',
  })
  @IsOptional()
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => WorkoutExerciseDto)
  exercises?: WorkoutExerciseDto[];
}
