import { BadRequestException, Injectable } from '@nestjs/common';
import { GymRole, PaymentStatus, Prisma } from '@prisma/client';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import type { AnalyticsGranularity } from './dto/analytics-range-query.dto';

const MAX_RANGE_MS = 450 * 24 * 60 * 60 * 1000;

function truncUnitSql(g: AnalyticsGranularity): Prisma.Sql {
  const u = g === 'day' ? 'day' : g === 'week' ? 'week' : 'month';
  return Prisma.raw(`'${u}'`);
}

function toIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

@Injectable()
export class AnalyticsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly permissionEngine: PermissionEngineService,
  ) {}

  private parseRange(fromIso: string, toIso: string): { from: Date; to: Date } {
    const from = new Date(fromIso);
    const to = new Date(toIso);
    if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) {
      throw new BadRequestException('Invalid date range');
    }
    if (from > to) {
      throw new BadRequestException('from must be before or equal to to');
    }
    if (to.getTime() - from.getTime() > MAX_RANGE_MS) {
      throw new BadRequestException('Date range cannot exceed 450 days');
    }
    return { from, to };
  }

  /**
   * Single response for dashboard apps: only includes sections the user may view (RBAC).
   */
  async getOverview(
    userId: string,
    gymId: string,
    fromIso: string,
    toIso: string,
    granularity: AnalyticsGranularity,
  ) {
    const range = this.parseRange(fromIso, toIso);
    await this.gymAccess.assertCanManageGym(userId, gymId);
    const eff = await this.permissionEngine.getEffective(userId, gymId);

    const out: {
      gymId: string;
      granularity: AnalyticsGranularity;
      range: { from: string; to: string };
      payments?: Awaited<ReturnType<AnalyticsService['paymentGrowth']>>;
      monthlyRevenue?: Awaited<ReturnType<AnalyticsService['monthlyRevenue']>>;
      members?: Awaited<ReturnType<AnalyticsService['memberGrowth']>>;
      attendance?: Awaited<ReturnType<AnalyticsService['attendanceTrends']>>;
    } = {
      gymId,
      granularity,
      range: { from: fromIso, to: toIso },
    };

    const tasks: Promise<void>[] = [];

    if (eff.effective.payments) {
      tasks.push(
        (async () => {
          const [payments, monthlyRevenue] = await Promise.all([
            this.paymentGrowthInternal(gymId, range, granularity),
            this.monthlyRevenueInternal(gymId, range),
          ]);
          out.payments = payments;
          out.monthlyRevenue = monthlyRevenue;
        })(),
      );
    }

    if (eff.effective.members) {
      tasks.push(
        (async () => {
          out.members = await this.memberGrowthInternal(
            gymId,
            range,
            granularity,
          );
        })(),
      );
    }

    if (eff.effective.dashboard) {
      tasks.push(
        (async () => {
          out.attendance = await this.attendanceTrendsInternal(
            gymId,
            range,
            granularity,
          );
        })(),
      );
    }

    await Promise.all(tasks);
    return out;
  }

  async paymentGrowth(
    userId: string,
    gymId: string,
    fromIso: string,
    toIso: string,
    granularity: AnalyticsGranularity,
  ) {
    await this.permissionEngine.assertOwnerOrPermissionMode(
      userId,
      gymId,
      [PERMISSION_CODES.PAYMENTS],
      'all',
    );
    const range = this.parseRange(fromIso, toIso);
    return this.paymentGrowthInternal(gymId, range, granularity);
  }

  async monthlyRevenue(
    userId: string,
    gymId: string,
    fromIso: string,
    toIso: string,
  ) {
    await this.permissionEngine.assertOwnerOrPermissionMode(
      userId,
      gymId,
      [PERMISSION_CODES.PAYMENTS],
      'all',
    );
    const range = this.parseRange(fromIso, toIso);
    return this.monthlyRevenueInternal(gymId, range);
  }

  async memberGrowth(
    userId: string,
    gymId: string,
    fromIso: string,
    toIso: string,
    granularity: AnalyticsGranularity,
  ) {
    await this.permissionEngine.assertOwnerOrPermissionMode(
      userId,
      gymId,
      [PERMISSION_CODES.MEMBERS],
      'all',
    );
    const range = this.parseRange(fromIso, toIso);
    return this.memberGrowthInternal(gymId, range, granularity);
  }

  async attendanceTrends(
    userId: string,
    gymId: string,
    fromIso: string,
    toIso: string,
    granularity: AnalyticsGranularity,
  ) {
    await this.permissionEngine.assertOwnerOrPermissionMode(
      userId,
      gymId,
      [PERMISSION_CODES.DASHBOARD],
      'all',
    );
    const range = this.parseRange(fromIso, toIso);
    return this.attendanceTrendsInternal(gymId, range, granularity);
  }

  private async paymentGrowthInternal(
    gymId: string,
    range: { from: Date; to: Date },
    granularity: AnalyticsGranularity,
  ) {
    const rows = await this.prisma.$queryRaw<
      Array<{ bucket: Date; currency: string; amount_cents: bigint }>
    >(
      Prisma.sql`
        SELECT date_trunc(${truncUnitSql(granularity)}, COALESCE("completedAt", "createdAt")) AS bucket,
               "currency",
               SUM("amountCents")::bigint AS amount_cents
        FROM "Payment"
        WHERE "gymId" = ${gymId}
          AND "status" = ${PaymentStatus.COMPLETED}
          AND COALESCE("completedAt", "createdAt") >= ${range.from}
          AND COALESCE("completedAt", "createdAt") <= ${range.to}
        GROUP BY 1, "currency"
        ORDER BY 1, 2
      `,
    );

    const series = rows.map((r) => ({
      bucket: toIsoDate(new Date(r.bucket)),
      currency: r.currency,
      amountCents: Number(r.amount_cents),
    }));

    const totalsByCurrency: Record<string, number> = {};
    for (const r of rows) {
      const c = r.currency;
      totalsByCurrency[c] = (totalsByCurrency[c] ?? 0) + Number(r.amount_cents);
    }

    return {
      series,
      totalsByCurrency,
    };
  }

  private async monthlyRevenueInternal(
    gymId: string,
    range: { from: Date; to: Date },
  ) {
    const rows = await this.prisma.$queryRaw<
      Array<{ month: string; currency: string; amount_cents: bigint }>
    >(
      Prisma.sql`
        SELECT to_char(
                 date_trunc('month', COALESCE("completedAt", "createdAt")),
                 'YYYY-MM'
               ) AS month,
               "currency",
               SUM("amountCents")::bigint AS amount_cents
        FROM "Payment"
        WHERE "gymId" = ${gymId}
          AND "status" = ${PaymentStatus.COMPLETED}
          AND COALESCE("completedAt", "createdAt") >= ${range.from}
          AND COALESCE("completedAt", "createdAt") <= ${range.to}
        GROUP BY 1, "currency"
        ORDER BY 1, 2
      `,
    );

    return {
      months: rows.map((r) => ({
        month: r.month,
        currency: r.currency,
        amountCents: Number(r.amount_cents),
      })),
    };
  }

  private async memberGrowthInternal(
    gymId: string,
    range: { from: Date; to: Date },
    granularity: AnalyticsGranularity,
  ) {
    const rows = await this.prisma.$queryRaw<
      Array<{ bucket: Date; new_members: bigint }>
    >(
      Prisma.sql`
        SELECT date_trunc(${truncUnitSql(granularity)}, "joinedAt") AS bucket,
               COUNT(*)::bigint AS new_members
        FROM "GymUser"
        WHERE "gymId" = ${gymId}
          AND "role" = ${GymRole.MEMBER}
          AND "joinedAt" >= ${range.from}
          AND "joinedAt" <= ${range.to}
        GROUP BY 1
        ORDER BY 1
      `,
    );

    const series = rows.map((r) => ({
      bucket: toIsoDate(new Date(r.bucket)),
      newMembers: Number(r.new_members),
    }));
    const totalNewMembers = series.reduce((s, x) => s + x.newMembers, 0);

    return { series, totalNewMembers };
  }

  private async attendanceTrendsInternal(
    gymId: string,
    range: { from: Date; to: Date },
    granularity: AnalyticsGranularity,
  ) {
    const fromDate = range.from.toISOString().slice(0, 10);
    const toDate = range.to.toISOString().slice(0, 10);

    const rows = await this.prisma.$queryRaw<
      Array<{ bucket: Date; check_ins: bigint }>
    >(
      Prisma.sql`
        SELECT date_trunc(${truncUnitSql(granularity)}, "attendedOn"::timestamp) AS bucket,
               COUNT(*)::bigint AS check_ins
        FROM "AttendanceRecord"
        WHERE "gymId" = ${gymId}
          AND "attendedOn" >= ${fromDate}::date
          AND "attendedOn" <= ${toDate}::date
        GROUP BY 1
        ORDER BY 1
      `,
    );

    const series = rows.map((r) => ({
      bucket: toIsoDate(new Date(r.bucket)),
      checkIns: Number(r.check_ins),
    }));
    const totalCheckIns = series.reduce((s, x) => s + x.checkIns, 0);

    return { series, totalCheckIns };
  }
}
