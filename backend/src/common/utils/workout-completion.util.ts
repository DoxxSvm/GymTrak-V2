const MONTH_LABELS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const;

export function computeWorkoutDurationMinutes(
  startedAt: Date | null,
  endedAt: Date | null,
): number {
  if (!startedAt || !endedAt) {
    return 0;
  }
  return Math.max(
    0,
    Math.round((endedAt.getTime() - startedAt.getTime()) / 60_000),
  );
}

export function formatWorkoutDurationLabel(durationMinutes: number): string {
  return `${Math.max(0, Math.round(durationMinutes))} min`;
}

export function formatWorkoutDateLabel(d: Date): string {
  return `${d.getUTCDate()} ${MONTH_LABELS[d.getUTCMonth()]}`;
}

export function workoutCompletionDateYmd(d: Date): string {
  return d.toISOString().slice(0, 10);
}

/** Adjust session timestamps so displayed duration matches `durationMinutes`. */
export function applyWorkoutDurationMinutes(
  startedAt: Date | null,
  endedAt: Date | null,
  durationMinutes: number,
): { startedAt: Date; endedAt: Date } {
  const safeMin = Math.max(0, Math.round(durationMinutes));
  const anchorEnd = endedAt ?? new Date();
  if (startedAt) {
    return {
      startedAt,
      endedAt: new Date(startedAt.getTime() + safeMin * 60_000),
    };
  }
  return {
    startedAt: new Date(anchorEnd.getTime() - safeMin * 60_000),
    endedAt: anchorEnd,
  };
}
