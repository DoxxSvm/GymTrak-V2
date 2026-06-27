import { IsNotEmpty, IsString } from 'class-validator';

export class AddExerciseToWorkoutDto {
  @IsString()
  @IsNotEmpty()
  exercise_id: string;
}
