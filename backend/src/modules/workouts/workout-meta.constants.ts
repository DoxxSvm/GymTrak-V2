/**
 * Static labels/icons for exercise enums — GET /meta/all (single source for app forms).
 * Stored values in DB are the `value` fields only.
 */
export const WORKOUT_META_ALL = {
  equipments: [
    { label: 'None', value: 'NONE', icon: 'none.png' },
    { label: 'Barbell', value: 'BARBELL', icon: 'barbell.png' },
    { label: 'Dumbbell', value: 'DUMBBELL', icon: 'dumbbell.png' },
    { label: 'Kettlebell', value: 'KETTLEBELL', icon: 'kettlebell.png' },
    { label: 'Machine', value: 'MACHINE', icon: 'machine.png' },
    { label: 'Plate', value: 'PLATE', icon: 'plate.png' },
    { label: 'Resistance Band', value: 'RESISTANCE_BAND', icon: 'band.png' },
    { label: 'Suspension Band', value: 'SUSPENSION_BAND', icon: 'suspension.png' },
    { label: 'Other', value: 'OTHER', icon: 'other.png' },
  ],
  muscles: [
    { label: 'Abdominals', value: 'ABDOMINALS' },
    { label: 'Abductors', value: 'ABDUCTORS' },
    { label: 'Adductors', value: 'ADDUCTORS' },
    { label: 'Biceps', value: 'BICEPS' },
    { label: 'Calves', value: 'CALVES' },
    { label: 'Cardio', value: 'CARDIO' },
    { label: 'Chest', value: 'CHEST' },
    { label: 'Forearms', value: 'FOREARMS' },
    { label: 'Full Body', value: 'FULL_BODY' },
    { label: 'Glutes', value: 'GLUTES' },
    { label: 'Hamstrings', value: 'HAMSTRINGS' },
    { label: 'Lats', value: 'LATS' },
    { label: 'Lower Back', value: 'LOWER_BACK' },
    { label: 'Neck', value: 'NECK' },
    { label: 'Quadriceps', value: 'QUADRICEPS' },
    { label: 'Shoulders', value: 'SHOULDERS' },
    { label: 'Traps', value: 'TRAPS' },
    { label: 'Triceps', value: 'TRICEPS' },
    { label: 'Upper Back', value: 'UPPER_BACK' },
    { label: 'Other', value: 'OTHER' },
  ],
  exercise_types: [
    { label: 'Weight & Reps', value: 'WEIGHT_REPS', fields: ['REPS', 'KG'] },
    { label: 'Bodyweight Reps', value: 'BODYWEIGHT_REPS', fields: ['REPS'] },
    {
      label: 'Weighted Bodyweight',
      value: 'WEIGHTED_BODYWEIGHT',
      fields: ['REPS', '+KG'],
    },
    {
      label: 'Assisted Bodyweight',
      value: 'ASSISTED_BODYWEIGHT',
      fields: ['REPS', '-KG'],
    },
    { label: 'Duration', value: 'DURATION', fields: ['TIME'] },
    {
      label: 'Duration & Weight',
      value: 'DURATION_WEIGHT',
      fields: ['TIME', 'KG'],
    },
    {
      label: 'Distance & Duration',
      value: 'DISTANCE_DURATION',
      fields: ['TIME', 'KM'],
    },
    {
      label: 'Weight & Distance',
      value: 'WEIGHT_DISTANCE',
      fields: ['KG', 'KM'],
    },
  ],
} as const;
