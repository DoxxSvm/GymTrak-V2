import { ApiProperty } from '@nestjs/swagger';
import { MemberPersonalWorkoutExerciseGroupSwagger } from '../../members/dto/member-personal-workout.swagger';

export class WorkoutCompletionSummarySwagger {
  @ApiProperty()
  workout_id: string;

  @ApiProperty()
  title: string;

  @ApiProperty()
  completed: boolean;

  @ApiProperty({ format: 'date', example: '2026-06-12' })
  date: string;

  @ApiProperty({ example: '12 Jun' })
  date_label: string;

  @ApiProperty({ type: String, format: 'date-time', nullable: true })
  started_at: string | null;

  @ApiProperty({ type: String, format: 'date-time', nullable: true })
  ended_at: string | null;

  @ApiProperty({ example: 45 })
  duration_minutes: number;

  @ApiProperty({ example: '45 min' })
  duration: string;

  @ApiProperty({ example: 1200 })
  volume_kg: number;

  @ApiProperty({ example: '1200 kg' })
  volume: string;

  @ApiProperty()
  sets: number;

  @ApiProperty({ type: [MemberPersonalWorkoutExerciseGroupSwagger] })
  exercises: MemberPersonalWorkoutExerciseGroupSwagger[];
}
