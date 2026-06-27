import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  AuditAction,
  AuditEntityType,
  ExpenseCategory,
  GymRole,
  Prisma,
} from '@prisma/client';
import {
  paymentMethodFromApi,
  paymentMethodToApi,
} from '../../common/utils/payment-method.util';
import {
  monthUtcRange,
  utcDateRangeInclusive,
  yearUtcRange,
} from '../../common/utils/month-utc-range';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { AuditService } from '../audit/audit.service';
import { SaasEntitlementsService } from '../saas/saas-entitlements.service';
import type { CreateExpenseDto } from './dto/create-expense.dto';
import type { UpdateExpenseDto } from './dto/update-expense.dto';
import { GSTCalculate } from 'src/common/utils/gst-calculate';

function mapExpenseCategorySlug(
  slug?: string | null,
): ExpenseCategory | undefined {
  if (!slug?.trim()) {
    return undefined;
  }
  const s = slug.trim().toLowerCase().replace(/[- ]/g, '_');
  const map: Record<string, ExpenseCategory> = {
    rent: ExpenseCategory.RENT,
    utility: ExpenseCategory.UTILITIES,
    utilities: ExpenseCategory.UTILITIES,
    equipment: ExpenseCategory.EQUIPMENT,
    maintenance: ExpenseCategory.MAINTENANCE,
    supplies: ExpenseCategory.SUPPLIES,
    salary: ExpenseCategory.SALARY,
    marketing: ExpenseCategory.MARKETING,
    software: ExpenseCategory.SOFTWARE,
    other: ExpenseCategory.OTHER,
  };
  return map[s];
}

function resolveExpenseCategory(raw: string): ExpenseCategory | undefined {
  const t = raw.trim();
  if ((Object.values(ExpenseCategory) as string[]).includes(t)) {
    return t as ExpenseCategory;
  }
  return mapExpenseCategorySlug(t);
}

/** Columns needed for the public expense JSON (no joins). */
const expensePublicSelect = {
  id: true,
  gymId: true,
  description: true,
  category: true,
  occurredOn: true,
  method: true,
  gstPercent: true,
  amountCents: true,
  trainerGymUserId: true,
} as const;

type ExpensePublicRow = Prisma.ExpenseGetPayload<{
  select: typeof expensePublicSelect;
}>;

@Injectable()
export class ExpensesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly saas: SaasEntitlementsService,
    private readonly audit: AuditService,
  ) { }

  async list(
    actorUserId: string,
    gymId: string,
    category: ExpenseCategory | undefined,
    month: string | undefined,
    dateFrom: string | undefined,
    dateTo: string | undefined,
    limit: number,
    offset: number,
    filter?: 'this_month' | 'last_month' | 'yearly',
    format?: 'simple' | 'default',
    sortBy: 'occurredOn' | 'createdAt' | 'amountCents' | 'category' = 'occurredOn',
    sortOrder: 'asc' | 'desc' = 'desc',
    year?: number,
  ) {
    await this.saas.assertExpenses(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const now = new Date();
    let effMonth: string | undefined = month;
    let effYear: number | undefined;

    if (filter === 'this_month') {
      effMonth = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, '0')}`;
    } else if (filter === 'last_month') {
      const cy = now.getUTCFullYear();
      const cm = now.getUTCMonth() + 1;
      if (cm === 1) {
        effMonth = `${cy - 1}-12`;
      } else {
        effMonth = `${cy}-${String(cm - 1).padStart(2, '0')}`;
      }
    } else if (filter === 'yearly') {
      effYear = year ?? now.getUTCFullYear();
      effMonth = undefined;
    }

    const where: Prisma.ExpenseWhereInput = { gymId };
    if (category) {
      where.category = category;
    }

    if (dateFrom || dateTo) {
      if (!dateFrom?.trim() || !dateTo?.trim()) {
        throw new BadRequestException(
          'Both dateFrom and dateTo are required (YYYY-MM-DD)',
        );
      }
      const { start, endExclusive } = utcDateRangeInclusive(
        dateFrom.trim(),
        dateTo.trim(),
      );
      if (start.getTime() >= endExclusive.getTime()) {
        throw new BadRequestException('dateFrom must be on or before dateTo');
      }
      where.occurredOn = { gte: start, lt: endExclusive };
    } else if (effYear != null) {
      const { start, endExclusive } = yearUtcRange(effYear);
      where.occurredOn = { gte: start, lt: endExclusive };
    } else if (effMonth) {
      const [y, m] = effMonth.split('-').map(Number);
      if (!y || !m) {
        throw new BadRequestException('Invalid month parameter');
      }
      const { start, endExclusive } = monthUtcRange(y, m);
      where.occurredOn = { gte: start, lt: endExclusive };
    }

    const dir = sortOrder === 'asc' ? 'asc' : 'desc';
    const primary = sortBy ?? 'occurredOn';
    const orderBy: Prisma.ExpenseOrderByWithRelationInput[] = [
      { [primary]: dir },
      { id: dir },
    ];

    const [total, totalAmountCents, items] = await Promise.all([
      this.prisma.expense.count({ where }),
      this.prisma.expense.aggregate({
        where,
        _sum: { amountCents: true },
        _count: { _all: true },
      }),
      this.prisma.expense.findMany({
        where,
        orderBy,
        take: limit,
        skip: offset,
        select: expensePublicSelect,
      }),
    ]);

    const rows = items.map((e) => this.toPublicExpense(e));

    if (format === 'simple') {
      return rows;
    }

    return {
      total,
      limit,
      offset,
      items: rows,
      totalAmountCents: totalAmountCents._sum.amountCents ?? 0,
    };
  }

  async monthlySummary(
    actorUserId: string,
    gymId: string,
    year: number,
    month1to12: number,
  ) {
    await this.saas.assertExpenseAnalytics(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const { start, endExclusive } = monthUtcRange(year, month1to12);

    const rows = await this.prisma.expense.groupBy({
      by: ['category'],
      where: {
        gymId,
        occurredOn: { gte: start, lt: endExclusive },
      },
      _sum: { amountCents: true },
      _count: { _all: true },
    });

    const byCategory = {} as Record<
      ExpenseCategory,
      { totalCents: number; count: number }
    >;
    for (const c of Object.values(ExpenseCategory)) {
      byCategory[c] = { totalCents: 0, count: 0 };
    }
    let grandTotalCents = 0;
    let totalCount = 0;
    for (const r of rows) {
      const sum = r._sum.amountCents ?? 0;
      const cnt = r._count._all;
      byCategory[r.category] = { totalCents: sum, count: cnt };
      grandTotalCents += sum;
      totalCount += cnt;
    }

    return {
      gymId,
      year,
      month: month1to12,
      range: {
        start: start.toISOString(),
        endExclusive: endExclusive.toISOString(),
      },
      currency: 'INR',
      grandTotalCents,
      expenseCount: totalCount,
      byCategory,
    };
  }

  async getOne(actorUserId: string, gymId: string, expenseId: string) {
    await this.saas.assertExpenses(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.expense.findFirst({
      where: { id: expenseId, gymId },
      select: expensePublicSelect,
    });
    if (!row) {
      throw new NotFoundException('Expense not found');
    }
    return this.toPublicExpense(row);
  }

  async create(actorUserId: string, dto: CreateExpenseDto) {
    await this.saas.assertExpenses(dto.gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, dto.gymId);

    const occurredRaw = dto.date?.trim();
    if (!occurredRaw) {
      throw new BadRequestException('date is required');
    }
    const occurredOn = new Date(occurredRaw);
    if (Number.isNaN(occurredOn.getTime())) {
      throw new BadRequestException('Invalid date');
    }

    const category = resolveExpenseCategory(dto.category);
    if (!category) {
      throw new BadRequestException('Invalid category');
    }

    const description = dto.bill_name?.trim() || null;

    let trainerGymUserId: string | null = null;
    if (dto.trainer_id?.trim()) {
      const tr = await this.prisma.gymUser.findFirst({
        where: {
          id: dto.trainer_id.trim(),
          gymId: dto.gymId,
          role: GymRole.TRAINER,
        },
        select: { id: true },
      });
      if (!tr) {
        throw new NotFoundException('Trainer not found at this gym');
      }
      trainerGymUserId = tr.id;
    }

    const method = dto.payment_mode
      ? paymentMethodFromApi(dto.payment_mode)
      : undefined;

    const row = await this.prisma.expense.create({
      data: {
        gymId: dto.gymId,
        amountCents: GSTCalculate(dto.amount, dto.gst ?? 0).totalAmount,
        currency: 'INR',
        category,
        description,
        occurredOn,
        method,
        gstPercent: new Prisma.Decimal(dto.gst ?? 0),
        trainerGymUserId,
        recordedByUserId: actorUserId,
      },
      select: expensePublicSelect,
    });

    await this.audit.log({
      gymId: dto.gymId,
      actorUserId,
      action: AuditAction.EXPENSE_CREATED,
      entityType: AuditEntityType.EXPENSE,
      entityId: row.id,
      metadata: {
        amountCents: row.amountCents,
        category: row.category,
        method: row.method,
      },
    });

    return this.toPublicExpense(row);
  }

  async update(
    actorUserId: string,
    gymId: string,
    expenseId: string,
    dto: UpdateExpenseDto,
  ) {
    await this.saas.assertExpenses(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const existing = await this.prisma.expense.findFirst({
      where: { id: expenseId, gymId },
      select: { id: true },
    });
    if (!existing) {
      throw new NotFoundException('Expense not found');
    }

    const data: Prisma.ExpenseUpdateInput = {};
    if (dto.amountCents != null) {
      data.amountCents = dto.amountCents;
    } else if (dto.amount != null) {
      data.amountCents = Math.round(dto.amount);
    }
    if (dto.currency != null) {
      data.currency = dto.currency.trim();
    }
    if (dto.category != null) {
      data.category = dto.category;
    } else if (dto.categorySlug != null) {
      const mapped = mapExpenseCategorySlug(dto.categorySlug);
      if (!mapped) {
        throw new BadRequestException('Invalid categorySlug');
      }
      data.category = mapped;
    }
    if (dto.title !== undefined) {
      data.description = dto.title?.trim() || null;
    } else if (dto.description !== undefined) {
      data.description = dto.description?.trim() || null;
    }
    const occurredRaw = dto.occurredOn ?? dto.date;
    if (occurredRaw != null) {
      const trimmed = occurredRaw.trim();
      if (!trimmed) {
        throw new BadRequestException('Invalid occurredOn / date');
      }
      const d = new Date(trimmed);
      if (Number.isNaN(d.getTime())) {
        throw new BadRequestException('Invalid occurredOn / date');
      }
      data.occurredOn = d;
    }
    if (dto.payment_mode !== undefined) {
      if (dto.payment_mode === null) {
        data.method = null;
      } else {
        data.method = paymentMethodFromApi(dto.payment_mode);
      }
    }
    if (dto.gstPercent !== undefined) {
      data.gstPercent =
        dto.gstPercent === null
          ? null
          : new Prisma.Decimal(dto.gstPercent);
    }
    if (dto.trainer_id !== undefined) {
      const tid = dto.trainer_id?.trim() ?? '';
      if (!tid) {
        data.trainerGymUser = { disconnect: true };
      } else {
        const tr = await this.prisma.gymUser.findFirst({
          where: {
            id: tid,
            gymId,
            role: GymRole.TRAINER,
          },
          select: { id: true },
        });
        if (!tr) {
          throw new NotFoundException('Trainer not found at this gym');
        }
        data.trainerGymUser = { connect: { id: tr.id } };
      }
    }

    const row = await this.prisma.expense.update({
      where: { id: expenseId },
      data,
      select: expensePublicSelect,
    });

    return this.toPublicExpense(row);
  }

  async remove(actorUserId: string, gymId: string, expenseId: string) {
    await this.saas.assertExpenses(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const existing = await this.prisma.expense.findFirst({
      where: { id: expenseId, gymId },
      select: { id: true, amountCents: true, category: true },
    });
    if (!existing) {
      throw new NotFoundException('Expense not found');
    }

    await this.audit.log({
      gymId,
      actorUserId,
      action: AuditAction.EXPENSE_DELETED,
      entityType: AuditEntityType.EXPENSE,
      entityId: expenseId,
      metadata: {
        amountCents: existing.amountCents,
        category: existing.category,
      },
    });

    await this.prisma.expense.delete({ where: { id: expenseId } });
    return { ok: true };
  }

  private toPublicExpense(row: ExpensePublicRow) {
    return {
      id: row.id,
      gymId: row.gymId,
      bill_name: row.description?.trim() ?? '',
      category: row.category,
      date: row.occurredOn.toISOString().slice(0, 10),
      trainer_id: row.trainerGymUserId,
      payment_mode:
        row.method != null ? paymentMethodToApi(row.method) : null,
      gst: row.gstPercent != null ? Number(row.gstPercent) : null,
      amount: row.amountCents,
    };
  }
}
