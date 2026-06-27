import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

/** Body for `POST /workouts/save`, `POST /workouts/start`, and `POST /workouts/stop` — only `workout_id` allowed. */
export class WorkoutIdOnlyDto {
  @ApiProperty({
    description:
      'Member workout plan id (only field allowed in the JSON body).',
  })
  @IsString()
  @IsNotEmpty()
  workout_id!: string;
}
