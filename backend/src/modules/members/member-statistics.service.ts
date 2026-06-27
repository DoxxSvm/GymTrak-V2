import { Injectable } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type { MemberStatisticsQueryDto } from './dto/member-statistics-query.dto';

/** Rough active-calorie estimate from duration (no per-user HR data in DB). */
const KCAL_PER_MINUTE_EST = 5;
const DEFAULT_MONTHLY_WORKOUT_GOAL = 20;

type WorkoutRow = {
  startedAt: Date | null;
  endedAt: Date | null;
  totalVolume: number;
  totalSets: number;
};

type ConsumptionRow = {
  consumedOn: Date;
  calories: number;
};

function parseYmd(ymd: string | undefined, fallback: Date): Date {
  if (!ymd) {
    return fallback;
  }
  const m = ymd.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!m) {
    return fallback;
  }
  return new Date(
    Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]), 12, 0, 0, 0),
  );
}

function startOfUtcDay(d: Date): Date {
  return new Date(
    Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), 0, 0, 0, 0),
  );
}

function addUtcDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + n);
  return x;
}

function startOfIsoWeekMonday(d: Date): Date {
  const day = d.getUTCDay();
  const diff = day === 0 ? -6 : 1 - day;
  return startOfUtcDay(addUtcDays(d, diff));
}

function ymdUtc(d: Date): string {
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, '0');
  const day = String(d.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function minutesBetween(start: Date | null, end: Date | null): number {
  if (!start || !end) {
    return 0;
  }
  return Math.max(0, Math.round((end.getTime() - start.getTime()) / 60000));
}

function formatDurationDisplayShort(totalMin: number): string {
  if (totalMin <= 0) {
    return '0h';
  }
  const h = Math.floor(totalMin / 60);
  if (h >= 1) {
    return `${h}h`;
  }
  return `${totalMin}m`;
}

function formatDurationLong(totalMin: number): string {
  if (totalMin <= 0) {
    return '0 mins';
  }
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  if (h > 0 && m > 0) {
    return `${h} hrs ${m} mins`;
  }
  if (h > 0) {
    return `${h} hrs`;
  }
  return `${m} mins`;
}

function formatCaloriesDisplay(kcal: number): string {
  if (kcal >= 1000) {
    return `${(kcal / 1000).toFixed(1)}k`;
  }
  return String(kcal);
}

function pctChange(
  current: number,
  previous: number,
): { percent_change: number | null; direction: 'up' | 'down' | 'flat' } {
  if (previous <= 0) {
    if (current <= 0) {
      return { percent_change: null, direction: 'flat' };
    }
    return { percent_change: 100, direction: 'up' };
  }
  const p = ((current - previous) / previous) * 100;
  const rounded = Math.round(p * 10) / 10;
  if (Math.abs(rounded) < 0.05) {
    return { percent_change: 0, direction: 'flat' };
  }
  return {
    percent_change: rounded,
    direction: rounded > 0 ? 'up' : 'down',
  };
}

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] as const;

function emptyWeekDays(
  weekStart: Date,
): {
  weekday: (typeof WEEKDAYS)[number];
  weekday_index: number;
  date: string;
}[] {
  return WEEKDAYS.map((weekday, weekday_index) => ({
    weekday,
    weekday_index,
    date: ymdUtc(addUtcDays(weekStart, weekday_index)),
  }));
}

@Injectable()
export class MemberStatisticsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async getSelfStatistics(userId: string, query: MemberStatisticsQueryDto) {
    const now = new Date();
    const anchor = parseYmd(query.date, now);
    const period = query.period ?? 'week';

    const range = this.getPeriodRange(anchor, period);
    const previousRange = this.getPreviousRange(anchor, period);

    let gymUserId: string | null = null;
    let gymId: string | null = null;
    if (query.gymId) {
      const m = await this.gymAccess.assertMemberAtGym(userId, query.gymId);
      gymUserId = m.gymUserId;
      gymId = query.gymId;
    }

    const [
      personalCurrent,
      personalPrev,
      gymCurrent,
      gymPrev,
      consumptionCurrent,
      consumptionPrev,
    ] = await Promise.all([
      this.loadPersonalWorkouts(userId, range.start, range.end),
      this.loadPersonalWorkouts(userId, previousRange.start, previousRange.end),
      gymId && gymUserId
        ? this.loadGymWorkouts(gymId, gymUserId, range.start, range.end)
        : Promise.resolve([] as WorkoutRow[]),
      gymId && gymUserId
        ? this.loadGymWorkouts(
            gymId,
            gymUserId,
            previousRange.start,
            previousRange.end,
          )
        : Promise.resolve([] as WorkoutRow[]),
      this.loadDietConsumptions(userId, gymId, range.start, range.end),
      this.loadDietConsumptions(
        userId,
        gymId,
        previousRange.start,
        previousRange.end,
      ),
    ]);

    const curAgg = this.aggregateRows(
      personalCurrent,
      gymCurrent,
      consumptionCurrent,
    );
    const prevAgg = this.aggregateRows(
      personalPrev,
      gymPrev,
      consumptionPrev,
    );

    const streakDays = await this.loadStreakActivityDays(
      userId,
      gymId,
      gymUserId,
      addUtcDays(now, -400),
      now,
    );
    const bestStreak = longestConsecutiveStreak(streakDays);

    const compLabel = this.comparisonLabel(period);

    const calYear = query.calendar_year
      ? parseInt(query.calendar_year, 10)
      : anchor.getUTCFullYear();
    const calMonth = query.calendar_month
      ? parseInt(query.calendar_month, 10)
      : anchor.getUTCMonth() + 1;
    const attendance = await this.buildAttendanceCalendar(
      userId,
      gymId,
      gymUserId,
      calYear,
      calMonth,
    );

    const weekStart = startOfIsoWeekMonday(anchor);
    const weekEnd = addUtcDays(weekStart, 7);
    const personalWeek = await this.loadPersonalWorkouts(
      userId,
      weekStart,
      weekEnd,
    );
    const [gymWeek, consumptionWeek] = await Promise.all([
      gymId && gymUserId
        ? this.loadGymWorkouts(gymId, gymUserId, weekStart, weekEnd)
        : Promise.resolve([] as WorkoutRow[]),
      this.loadDietConsumptions(userId, gymId, weekStart, weekEnd),
    ]);
    const weeklyMap = this.buildDailyMetricMap(
      weekStart,
      personalWeek,
      gymWeek,
      consumptionWeek,
    );

    const monthAnchor = new Date(
      Date.UTC(anchor.getUTCFullYear(), anchor.getUTCMonth(), 1, 0, 0, 0, 0),
    );
    const nextMonth = new Date(
      Date.UTC(
        anchor.getUTCFullYear(),
        anchor.getUTCMonth() + 1,
        1,
        0,
        0,
        0,
        0,
      ),
    );
    const personalMonth = await this.loadPersonalWorkouts(
      userId,
      monthAnchor,
      nextMonth,
    );
    const gymMonth =
      gymId && gymUserId
        ? await this.loadGymWorkouts(gymId, gymUserId, monthAnchor, nextMonth)
        : [];
    const monthAgg = this.aggregateRows(personalMonth, gymMonth);
    const prevMonthStart = new Date(
      Date.UTC(
        anchor.getUTCFullYear(),
        anchor.getUTCMonth() - 1,
        1,
        0,
        0,
        0,
        0,
      ),
    );
    const prevMonthEnd = monthAnchor;
    const personalMonthPrev = await this.loadPersonalWorkouts(
      userId,
      prevMonthStart,
      prevMonthEnd,
    );
    const gymMonthPrev =
      gymId && gymUserId
        ? await this.loadGymWorkouts(
            gymId,
            gymUserId,
            prevMonthStart,
            prevMonthEnd,
          )
        : [];
    const monthPrevAgg = this.aggregateRows(personalMonthPrev, gymMonthPrev);
    const monthTrend = pctChange(
      monthAgg.workoutCount,
      monthPrevAgg.workoutCount,
    );

    return {
      user_id: userId,
      period,
      comparison_label: compLabel,
      range: {
        from: range.start.toISOString(),
        to: new Date(range.end.getTime() - 1).toISOString(),
        label: range.label,
      },
      previous_range: {
        from: previousRange.start.toISOString(),
        to: new Date(previousRange.end.getTime() - 1).toISOString(),
        label: previousRange.label,
      },
      summary: {
        total_workouts: {
          value: curAgg.workoutCount,
          previous_value: prevAgg.workoutCount,
          ...pctChange(curAgg.workoutCount, prevAgg.workoutCount),
          comparison_label: compLabel,
        },
        total_duration: {
          value_minutes: curAgg.totalMinutes,
          display: formatDurationDisplayShort(curAgg.totalMinutes),
          previous_value_minutes: prevAgg.totalMinutes,
          ...pctChange(curAgg.totalMinutes, prevAgg.totalMinutes),
          comparison_label: compLabel,
        },
        active_calories: {
          value: curAgg.activeCaloriesEstimate,
          display: formatCaloriesDisplay(curAgg.activeCaloriesEstimate),
          previous_value: prevAgg.activeCaloriesEstimate,
          ...pctChange(
            curAgg.activeCaloriesEstimate,
            prevAgg.activeCaloriesEstimate,
          ),
          comparison_label: compLabel,
          estimate_note:
            'Calories include food logged via `POST /diet/food-consume` plus workout session estimates (≈5 kcal/min when duration is known)',
        },
        best_streak: {
          value_days: bestStreak,
          display: bestStreak > 0 ? `${bestStreak}d` : '0d',
          source:
            'Longest run of days with a completed personal workout' +
            (gymId ? ' or gym workout / check-in' : '') +
            ' (last ~400 days, UTC).',
        },
      },
      monthly_goal: {
        year: monthAnchor.getUTCFullYear(),
        month: monthAnchor.getUTCMonth() + 1,
        target_workouts: DEFAULT_MONTHLY_WORKOUT_GOAL,
        workout_count: monthAgg.workoutCount,
        duration_minutes: monthAgg.totalMinutes,
        duration_formatted: formatDurationLong(monthAgg.totalMinutes),
        percent_change_vs_previous_month: monthTrend.percent_change,
        message: (() => {
          if (monthAgg.workoutCount >= DEFAULT_MONTHLY_WORKOUT_GOAL) {
            return "Great job! you're crushing your monthly goal.";
          }
          const remaining = Math.max(
            0,
            DEFAULT_MONTHLY_WORKOUT_GOAL - monthAgg.workoutCount,
          );
          return `Keep going — ${remaining} more workout${remaining === 1 ? '' : 's'} to hit the target.`;
        })(),
      },
      weekly_activity: {
        week_start: ymdUtc(weekStart),
        week_end: ymdUtc(addUtcDays(weekStart, 6)),
        by_metric: {
          active_calories: {
            unit: 'kcal',
            total: weeklyMap.totalKcal,
            title_example: `Weekly Activity: ${weeklyMap.totalKcal.toLocaleString('en-US')} kcal total`,
            points: weeklyMap.days.map((d) => ({
              ...d,
              value: d.calories,
            })),
          },
          volume: {
            unit: 'kg',
            total: weeklyMap.totalVolume,
            points: weeklyMap.days.map((d) => ({
              weekday: d.weekday,
              weekday_index: d.weekday_index,
              date: d.date,
              value: d.volume,
            })),
          },
          sets: {
            unit: 'count',
            total: weeklyMap.totalSets,
            points: weeklyMap.days.map((d) => ({
              weekday: d.weekday,
              weekday_index: d.weekday_index,
              date: d.date,
              value: d.sets,
            })),
          },
          duration: {
            unit: 'min',
            total: weeklyMap.totalMinutes,
            points: weeklyMap.days.map((d) => ({
              weekday: d.weekday,
              weekday_index: d.weekday_index,
              date: d.date,
              value: d.minutes,
            })),
          },
        },
      },
      attendance: {
        year: calYear,
        month: calMonth,
        days_with_activity: attendance,
        day_keys: {
          with_activity:
            'completed personal workout' +
            (gymId ? ' or gym workout or gym check-in' : ''),
        },
      },
    };
  }

  private comparisonLabel(period: 'week' | 'month' | 'year'): string {
    switch (period) {
      case 'week':
        return 'vs previous week';
      case 'month':
        return 'vs previous month';
      case 'year':
        return 'vs previous year';
      default:
        return 'vs previous period';
    }
  }

  private getPeriodRange(
    anchor: Date,
    period: 'week' | 'month' | 'year',
  ): { start: Date; end: Date; label: string } {
    if (period === 'week') {
      const start = startOfIsoWeekMonday(anchor);
      const end = addUtcDays(start, 7);
      return {
        start,
        end,
        label: `Week ${ymdUtc(start)} – ${ymdUtc(addUtcDays(start, 6))}`,
      };
    }
    if (period === 'month') {
      const y = anchor.getUTCFullYear();
      const m = anchor.getUTCMonth();
      const start = new Date(Date.UTC(y, m, 1, 0, 0, 0, 0));
      const end = new Date(Date.UTC(y, m + 1, 1, 0, 0, 0, 0));
      return { start, end, label: `${y}-${String(m + 1).padStart(2, '0')}` };
    }
    const y = anchor.getUTCFullYear();
    const start = new Date(Date.UTC(y, 0, 1, 0, 0, 0, 0));
    const end = new Date(Date.UTC(y + 1, 0, 1, 0, 0, 0, 0));
    return { start, end, label: String(y) };
  }

  private getPreviousRange(
    anchor: Date,
    period: 'week' | 'month' | 'year',
  ): { start: Date; end: Date; label: string } {
    if (period === 'week') {
      const curStart = startOfIsoWeekMonday(anchor);
      const end = curStart;
      const start = addUtcDays(curStart, -7);
      return {
        start,
        end,
        label: `Previous week ${ymdUtc(start)} – ${ymdUtc(addUtcDays(start, 6))}`,
      };
    }
    if (period === 'month') {
      const y = anchor.getUTCFullYear();
      const m = anchor.getUTCMonth();
      const start = new Date(Date.UTC(y, m - 1, 1, 0, 0, 0, 0));
      const end = new Date(Date.UTC(y, m, 1, 0, 0, 0, 0));
      return { start, end, label: 'Previous month' };
    }
    const y = anchor.getUTCFullYear();
    return {
      start: new Date(Date.UTC(y - 1, 0, 1, 0, 0, 0, 0)),
      end: new Date(Date.UTC(y, 0, 1, 0, 0, 0, 0)),
      label: String(y - 1),
    };
  }

  private personalWhereInRange(
    start: Date,
    end: Date,
  ): Prisma.MemberPersonalWorkoutPlanWhereInput {
    return {
      OR: [
        { endedAt: { gte: start, lt: end } },
        {
          endedAt: null,
          startedAt: { gte: start, lt: end },
          completed: true,
        },
      ],
    };
  }

  private async loadPersonalWorkouts(
    userId: string,
    start: Date,
    end: Date,
  ): Promise<WorkoutRow[]> {
    return this.prisma.memberPersonalWorkoutPlan.findMany({
      where: {
        userId,
        ...this.personalWhereInRange(start, end),
      },
      select: {
        startedAt: true,
        endedAt: true,
        totalVolume: true,
        totalSets: true,
      },
    });
  }

  private async loadGymWorkouts(
    gymId: string,
    gymUserId: string,
    start: Date,
    end: Date,
  ): Promise<WorkoutRow[]> {
    return this.prisma.memberWorkoutPlan.findMany({
      where: {
        gymId,
        gymUserId,
        OR: [
          { endedAt: { gte: start, lt: end } },
          {
            endedAt: null,
            startedAt: { gte: start, lt: end },
            completed: true,
          },
        ],
      },
      select: {
        startedAt: true,
        endedAt: true,
        totalVolume: true,
        totalSets: true,
      },
    });
  }

  private async loadDietConsumptions(
    userId: string,
    gymId: string | null,
    start: Date,
    end: Date,
  ): Promise<ConsumptionRow[]> {
    return this.prisma.dietFoodConsumption.findMany({
      where: {
        userId,
        consumedOn: { gte: start, lt: end },
        ...(gymId ? { gymId } : { gymId: null }),
      },
      select: { consumedOn: true, calories: true },
    });
  }

  private sumConsumptionCalories(rows: ConsumptionRow[]): number {
    return rows.reduce((sum, row) => sum + row.calories, 0);
  }

  private aggregateRows(
    personal: WorkoutRow[],
    gym: WorkoutRow[],
    consumption: ConsumptionRow[] = [],
  ): {
    workoutCount: number;
    totalMinutes: number;
    totalVolume: number;
    totalSets: number;
    activeCaloriesEstimate: number;
  } {
    const rows = [...personal, ...gym];
    let totalMinutes = 0;
    let totalVolume = 0;
    let totalSets = 0;
    for (const w of rows) {
      totalMinutes += minutesBetween(w.startedAt, w.endedAt);
      totalVolume += w.totalVolume ?? 0;
      totalSets += w.totalSets ?? 0;
    }
    const workoutKcal = Math.round(totalMinutes * KCAL_PER_MINUTE_EST);
    const consumedKcal = this.sumConsumptionCalories(consumption);
    return {
      workoutCount: rows.length,
      totalMinutes,
      totalVolume,
      totalSets,
      activeCaloriesEstimate: workoutKcal + consumedKcal,
    };
  }

  private buildDailyMetricMap(
    weekStart: Date,
    personal: WorkoutRow[],
    gym: WorkoutRow[],
    consumption: ConsumptionRow[] = [],
  ) {
    const byDay: Record<
      string,
      { min: number; vol: number; sets: number; kcal: number }
    > = {};
    for (const k of Array.from({ length: 7 }, (_, i) =>
      ymdUtc(addUtcDays(weekStart, i)),
    )) {
      byDay[k] = { min: 0, vol: 0, sets: 0, kcal: 0 };
    }
    const add = (w: WorkoutRow) => {
      const ref = w.endedAt ?? w.startedAt;
      if (!ref) {
        return;
      }
      const key = ymdUtc(startOfUtcDay(ref));
      if (byDay[key] === undefined) {
        return;
      }
      const m = minutesBetween(w.startedAt, w.endedAt);
      byDay[key].min += m;
      byDay[key].vol += w.totalVolume ?? 0;
      byDay[key].sets += w.totalSets ?? 0;
      byDay[key].kcal += Math.round(m * KCAL_PER_MINUTE_EST);
    };
    for (const w of personal) {
      add(w);
    }
    for (const w of gym) {
      add(w);
    }
    for (const row of consumption) {
      const key = ymdUtc(startOfUtcDay(row.consumedOn));
      if (byDay[key] !== undefined) {
        byDay[key].kcal += row.calories;
      }
    }
    const template = emptyWeekDays(weekStart);
    const days = template.map((d) => ({
      ...d,
      minutes: byDay[d.date].min,
      volume: byDay[d.date].vol,
      sets: byDay[d.date].sets,
      calories: byDay[d.date].kcal,
    }));
    return {
      days,
      totalMinutes: days.reduce((a, b) => a + b.minutes, 0),
      totalVolume: days.reduce((a, b) => a + b.volume, 0),
      totalSets: days.reduce((a, b) => a + b.sets, 0),
      totalKcal: days.reduce((a, b) => a + b.calories, 0),
    };
  }

  private async buildAttendanceCalendar(
    userId: string,
    gymId: string | null,
    gymUserId: string | null,
    year: number,
    month: number,
  ): Promise<string[]> {
    const start = new Date(Date.UTC(year, month - 1, 1, 0, 0, 0, 0));
    const end = new Date(Date.UTC(year, month, 1, 0, 0, 0, 0));
    const startKey = ymdUtc(start);
    const endKey = ymdUtc(addUtcDays(end, -1));
    const personal = await this.prisma.memberPersonalWorkoutPlan.findMany({
      where: {
        userId,
        OR: [
          { endedAt: { gte: start, lt: end } },
          {
            endedAt: null,
            startedAt: { gte: start, lt: end },
            completed: true,
          },
        ],
      },
      select: { endedAt: true, startedAt: true },
    });
    const days = new Set<string>();
    for (const w of personal) {
      const t = w.endedAt ?? w.startedAt;
      if (t) {
        const k = ymdUtc(startOfUtcDay(t));
        if (k >= startKey && k <= endKey) {
          days.add(k);
        }
      }
    }
    if (gymId && gymUserId) {
      const gymW = await this.prisma.memberWorkoutPlan.findMany({
        where: {
          gymId,
          gymUserId,
          OR: [
            { endedAt: { gte: start, lt: end } },
            { endedAt: null, startedAt: { gte: start, lt: end } },
          ],
        },
        select: { endedAt: true, startedAt: true },
      });
      for (const w of gymW) {
        const t = w.endedAt ?? w.startedAt;
        if (t) {
          const d = ymdUtc(startOfUtcDay(t));
          if (d >= startKey && d <= endKey) {
            days.add(d);
          }
        }
      }
      const att = await this.prisma.attendanceRecord.findMany({
        where: {
          gymId,
          memberUserId: userId,
          attendedOn: { gte: start, lt: end },
        },
        select: { attendedOn: true },
      });
      for (const a of att) {
        const d = new Date(a.attendedOn);
        const k = ymdUtc(startOfUtcDay(d));
        if (k >= startKey && k <= endKey) {
          days.add(k);
        }
      }
    }
    return [...days].sort();
  }

  private async loadStreakActivityDays(
    userId: string,
    gymId: string | null,
    gymUserId: string | null,
    from: Date,
    to: Date,
  ): Promise<string[]> {
    const days = new Set<string>();
    const personal = await this.prisma.memberPersonalWorkoutPlan.findMany({
      where: {
        userId,
        OR: [
          { endedAt: { gte: from, lte: to } },
          { startedAt: { gte: from, lte: to } },
        ],
      },
      select: { endedAt: true, startedAt: true, completed: true },
    });
    for (const w of personal) {
      const t = w.endedAt ?? w.startedAt;
      if (!t || t < from || t > to) {
        continue;
      }
      if (!w.endedAt && w.completed === false) {
        continue;
      }
      days.add(ymdUtc(startOfUtcDay(t)));
    }
    if (gymId && gymUserId) {
      const gymW = await this.prisma.memberWorkoutPlan.findMany({
        where: {
          gymId,
          gymUserId,
          OR: [
            { endedAt: { gte: from, lte: to } },
            { startedAt: { gte: from, lte: to } },
          ],
        },
        select: { endedAt: true, startedAt: true },
      });
      for (const w of gymW) {
        const t = w.endedAt ?? w.startedAt;
        if (t && t >= from && t <= to) {
          days.add(ymdUtc(startOfUtcDay(t)));
        }
      }
      const att = await this.prisma.attendanceRecord.findMany({
        where: {
          gymId,
          memberUserId: userId,
          attendedOn: { gte: from, lte: to },
        },
        select: { attendedOn: true },
      });
      for (const a of att) {
        const d = new Date(a.attendedOn);
        if (d >= from && d <= to) {
          days.add(ymdUtc(startOfUtcDay(d)));
        }
      }
    }
    return [...days].sort();
  }
}

function longestConsecutiveStreak(sortedYmd: string[]): number {
  if (sortedYmd.length === 0) {
    return 0;
  }
  let best = 1;
  let cur = 1;
  for (let i = 1; i < sortedYmd.length; i++) {
    const prevD = new Date(sortedYmd[i - 1] + 'T12:00:00.000Z');
    const d = new Date(sortedYmd[i] + 'T12:00:00.000Z');
    const diff = Math.round((d.getTime() - prevD.getTime()) / 86400000);
    if (diff === 1) {
      cur += 1;
    } else if (diff > 1) {
      best = Math.max(best, cur);
      cur = 1;
    }
    best = Math.max(best, cur);
  }
  return best;
}
