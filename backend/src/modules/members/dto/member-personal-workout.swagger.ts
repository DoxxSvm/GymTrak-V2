import { ApiProperty } from '@nestjs/swagger';

/** One set row under a personal-workout exercise. */
export class MemberPersonalWorkoutSetRowSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty()
  set_number: number;

  @ApiProperty()
  reps: number;

  @ApiProperty()
  weight: number;

  @ApiProperty()
  completed: boolean;
}

/** One exercise block with nested sets (snake_case, matches API). */
export class MemberPersonalWorkoutExerciseGroupSwagger {
  @ApiProperty()
  exercise_id: string;

  @ApiProperty()
  name: string;

  @ApiProperty({ type: [MemberPersonalWorkoutSetRowSwagger] })
  sets: MemberPersonalWorkoutSetRowSwagger[];
}

/**
 * Response from `GET /members/workouts/:id`, `POST /members/workouts`, `POST /members/workouts/start`, `POST /members/workouts/:id/stop`, `PATCH /members/workouts/:id`.
 */
export class MemberPersonalWorkoutDetailSwagger {
  @ApiProperty({ format: 'uuid' })
  workout_id: string;

  @ApiProperty()
  title: string;

  @ApiProperty({ example: '32 min' })
  duration: string;

  @ApiProperty({ example: '1200 kg' })
  volume: string;

  @ApiProperty({ description: 'Total set count' })
  sets: number;

  @ApiProperty({ type: [MemberPersonalWorkoutExerciseGroupSwagger] })
  exercises: MemberPersonalWorkoutExerciseGroupSwagger[];
}

/** Row from `GET /members/workouts` (list). */
export class MemberPersonalWorkoutListItemSwagger {
  @ApiProperty()
  workout_id: string;

  @ApiProperty()
  title: string;

  @ApiProperty({ type: String, format: 'date-time', nullable: true })
  date: Date | null;

  @ApiProperty()
  exercise_count: number;
}

/** One row in `GET /members/workouts/history`. */
export class MemberPersonalWorkoutHistoryItemSwagger {
  @ApiProperty()
  workout_id: string;

  @ApiProperty()
  user_id: string;

  @ApiProperty()
  title: string;

  @ApiProperty({ type: String, format: 'date-time', nullable: true })
  started_at: string | null;

  @ApiProperty({ type: String, format: 'date-time', nullable: true })
  ended_at: string | null;

  @ApiProperty()
  completed: boolean;

  @ApiProperty()
  total_volume: number;

  @ApiProperty()
  total_sets: number;

  @ApiProperty({ example: '45 min', nullable: true })
  duration: string | null;

  @ApiProperty()
  exercise_count: number;

  @ApiProperty({
    type: 'array',
    items: {
      type: 'object',
      properties: {
        exercise_id: { type: 'string' },
        name: { type: 'string' },
        asset_url: { type: 'string', nullable: true },
      },
    },
    description: 'Exercises in this session with catalog image URL for UI thumbnails.',
  })
  exercises: Array<{
    exercise_id: string;
    name: string;
    asset_url: string | null;
  }>;
}

/** Paged `GET /members/workouts/history` response. */
export class MemberPersonalWorkoutHistoryResponseSwagger {
  @ApiProperty()
  userId: string;

  @ApiProperty()
  page: number;

  @ApiProperty()
  limit: number;

  @ApiProperty()
  total: number;

  @ApiProperty({ type: [MemberPersonalWorkoutHistoryItemSwagger] })
  items: MemberPersonalWorkoutHistoryItemSwagger[];
}
