/** Inclusive-exclusive UTC month range for calendar-month queries. */
export function monthUtcRange(
  year: number,
  month1to12: number,
): { start: Date; endExclusive: Date } {
  const start = new Date(Date.UTC(year, month1to12 - 1, 1));
  const endExclusive = new Date(Date.UTC(year, month1to12, 1));
  return { start, endExclusive };
}

/** Inclusive `dateFrom` / `dateTo` (YYYY-MM-DD) as UTC day bounds, end exclusive. */
export function utcDateRangeInclusive(
  dateFromYmd: string,
  dateToYmd: string,
): { start: Date; endExclusive: Date } {
  const [yf, mf, df] = dateFromYmd.split('-').map(Number);
  const [yt, mt, dt] = dateToYmd.split('-').map(Number);
  const start = new Date(Date.UTC(yf, mf - 1, df));
  const endExclusive = new Date(Date.UTC(yt, mt - 1, dt + 1));
  return { start, endExclusive };
}

/** UTC calendar year: Jan 1 00:00 through Dec 31 (end exclusive = Jan 1 next year). */
export function yearUtcRange(year: number): { start: Date; endExclusive: Date } {
  const start = new Date(Date.UTC(year, 0, 1));
  const endExclusive = new Date(Date.UTC(year + 1, 0, 1));
  return { start, endExclusive };
}
