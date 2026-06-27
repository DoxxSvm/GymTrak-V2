/**
 * Classify member check-in time vs a simple "expected morning" window in the gym's IANA timezone.
 * Bands: EARLY (< 8:00), ON_TIME (8:00–8:30), REGULAR (8:30–9:30), LATE (after 9:30 local).
 */
export type AttendancePunctuality = 'EARLY' | 'ON_TIME' | 'REGULAR' | 'LATE';

function minuteOfDayInZone(d: Date, timeZone: string): number {
  const fmt = new Intl.DateTimeFormat('en-GB', {
    timeZone: timeZone || 'UTC',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  const parts = fmt.formatToParts(d);
  const h = parseInt(parts.find((p) => p.type === 'hour')?.value ?? '0', 10);
  const m = parseInt(parts.find((p) => p.type === 'minute')?.value ?? '0', 10);
  return h * 60 + m;
}

export function classifyAttendancePunctuality(
  checkedInAt: Date,
  timeZone: string,
): AttendancePunctuality {
  const mins = minuteOfDayInZone(checkedInAt, timeZone);
  const t8 = 8 * 60;
  const t830 = 8 * 60 + 30;
  const t930 = 9 * 60 + 30;
  if (mins < t8) {
    return 'EARLY';
  }
  if (mins < t830) {
    return 'ON_TIME';
  }
  if (mins < t930) {
    return 'REGULAR';
  }
  return 'LATE';
}

export function punctualityDisplayLabel(p: AttendancePunctuality): string {
  switch (p) {
    case 'EARLY':
      return 'EARLY';
    case 'ON_TIME':
      return 'ON TIME';
    case 'REGULAR':
      return 'REGULAR';
    case 'LATE':
      return 'LATE';
    default:
      return p;
  }
}

/** e.g. "Today, 8:15 AM" / "Yesterday, 7:45 AM" / "Oct 24, 2023, 8:30 AM" in gym TZ */
export function formatCheckInRelativeLine(
  checkedInAt: Date,
  now: Date,
  timeZone: string,
): string {
  const tz = timeZone || 'UTC';
  const dFmt = new Intl.DateTimeFormat('en-US', {
    timeZone: tz,
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
  const tFmt = new Intl.DateTimeFormat('en-US', {
    timeZone: tz,
    hour: 'numeric',
    minute: '2-digit',
  });

  const dayKey = (d: Date) =>
    new Intl.DateTimeFormat('en-CA', {
      timeZone: tz,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(d);

  const ck = dayKey(checkedInAt);
  const nk = dayKey(now);
  const yesterday = new Date(now);
  yesterday.setUTCDate(yesterday.getUTCDate() - 1);
  const yk = dayKey(yesterday);

  const timePart = tFmt.format(checkedInAt);
  if (ck === nk) {
    return `Today, ${timePart}`;
  }
  if (ck === yk) {
    return `Yesterday, ${timePart}`;
  }
  return `${dFmt.format(checkedInAt)}, ${timePart}`;
}

const MONTH_SHORT = [
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

export function monthShortLabel(month1to12: number): string {
  return MONTH_SHORT[Math.max(1, Math.min(12, month1to12)) - 1] ?? '';
}
