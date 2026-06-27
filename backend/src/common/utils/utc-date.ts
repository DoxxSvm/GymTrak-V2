/** UTC calendar start (00:00:00.000) — matches dashboard aggregation style */
export function startOfUtcDay(d: Date): Date {
  const x = new Date(d);
  x.setUTCHours(0, 0, 0, 0);
  return x;
}

/** Inclusive end of expiring window: day + `days` at 23:59:59.999 UTC */
export function endOfUtcDayAfter(d: Date, days: number): Date {
  const x = startOfUtcDay(d);
  x.setUTCDate(x.getUTCDate() + days);
  x.setUTCHours(23, 59, 59, 999);
  return x;
}

/** Calendar arithmetic in UTC (avoids local DST surprises). */
export function addUtcDays(d: Date, days: number): Date {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + days);
  return x;
}

export function addUtcMonths(d: Date, months: number): Date {
  const x = new Date(d);
  x.setUTCMonth(x.getUTCMonth() + months);
  return x;
}

export function addUtcYears(d: Date, years: number): Date {
  const x = new Date(d);
  x.setUTCFullYear(x.getUTCFullYear() + years);
  return x;
}
