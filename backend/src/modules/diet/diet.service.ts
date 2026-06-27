import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { DietFoodUnitType, DietMealType, GymRole, Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { GymAccessService } from '../../common/services/gym-access.service';
import type { CreateDietFoodDto } from './dto/create-diet-food.dto';
import type { CreateDietMealDto } from './dto/create-diet-meal.dto';
import type { DietMealFoodItemDto } from './dto/diet-meal-food-item.dto';
import type { DietHistoryQueryDto } from './dto/diet-history-query.dto';
import type { ListDietFoodQueryDto } from './dto/list-diet-food-query.dto';
import type { ListDietMealsQueryDto } from './dto/list-diet-meals-query.dto';
import type {
  ConsumeDietFoodItemDto,
  FoodConsumeDietDto,
} from './dto/food-consume.dto';
import type { UpdateDietFoodDto } from './dto/update-diet-food.dto';
import type { UpdateDietMealDto } from './dto/update-diet-meal.dto';

const dietMealFoodLinesInclude = {
  orderBy: { sortOrder: 'asc' as const },
  include: {
    dietFood: {
      select: {
        protein: true,
        carbs: true,
        fat: true,
        unitType: true,
      },
    },
  },
};

@Injectable()
export class DietService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async createMeal(
    actorUserId: string,
    dto: CreateDietMealDto,
    gymId?: string,
    jwtGymId?: string | null,
  ) {
    const targetMemberGymUserId = dto.member_id?.trim();

    if (targetMemberGymUserId) {
      const resolved = await this.gymAccess.resolveGymIdForCatalogWrite(
        actorUserId,
        gymId,
        jwtGymId ?? null,
      );
      const member = await this.prisma.gymUser.findFirst({
        where: {
          id: targetMemberGymUserId,
          gymId: resolved,
          role: GymRole.MEMBER,
          isActive: true,
        },
        select: { id: true, userId: true },
      });
      if (!member) {
        throw new NotFoundException('Member not found in this gym');
      }
      return this.insertDietMealForMember(actorUserId, dto, resolved, member);
    }

    const resolvedGym = await this.resolveGymIdForMemberSelfMeal(
      actorUserId,
      gymId,
      jwtGymId,
    );
    if (resolvedGym) {
      const { gymUserId } = await this.gymAccess.assertMemberAtGym(
        actorUserId,
        resolvedGym,
      );
      return this.insertDietMealForMember(actorUserId, dto, resolvedGym, {
        id: gymUserId,
        userId: actorUserId,
      });
    }
    return this.insertDietMealForMember(actorUserId, dto, null, {
      id: null,
      userId: actorUserId,
    });
  }

  private async insertDietMealForMember(
    actorUserId: string,
    dto: CreateDietMealDto,
    resolvedGymId: string | null,
    member: { id: string | null; userId: string },
  ) {
    const createdByRole = await this.resolveDietCreatedByRole(
      actorUserId,
      resolvedGymId,
      dto.created_by,
    );
    const repeatDays = this.normalizeRepeatDays(dto.repeat_days);
    const lineRows = await this.buildFoodLineRows(
      resolvedGymId,
      dto.food_items ?? [],
      member.userId,
    );

    const data: Prisma.DietMealUncheckedCreateInput = {
      userId: member.userId,
      gymId: resolvedGymId,
      gymUserId: member.id,
      name: dto.name.trim(),
      mealTime: dto.time.trim(),
      mealType: dto.meal_type,
      repeatEnabled: dto.repeat_enabled ?? false,
      repeatDays,
      createdByRole,
      foodLines: { create: lineRows },
    };
    const meal = await this.prisma.dietMeal.create({
      data,
      include: { foodLines: dietMealFoodLinesInclude },
    });
    return this.mealToApi(meal);
  }

  /** Gym meals: manager access. Personal meals (`gymId` null): owner only; query `gymId` must be omitted. */
  private async assertDietMealRowAccess(
    actorUserId: string,
    gymIdFromQuery: string | undefined,
    row: { gymId: string | null; userId: string },
  ): Promise<void> {
    if (row.gymId == null) {
      if (row.userId !== actorUserId) {
        throw new ForbiddenException('You can only access your own meals');
      }
      if (gymIdFromQuery?.trim()) {
        throw new BadRequestException('gymId does not match this resource');
      }
      return;
    }
    await this.gymAccess.assertManageGymForEntity(
      actorUserId,
      gymIdFromQuery,
      row.gymId,
    );
  }

  /**
   * Self-create (omit `member_id`): gym from query, then JWT default gym, then sole active member gym; else `null` = user-scoped personal meal (`gymId` / `gymUserId` null).
   */
  private async resolveGymIdForMemberSelfMeal(
    actorUserId: string,
    gymIdFromQuery?: string,
    jwtGymId?: string | null,
  ): Promise<string | null> {
    const fromQuery = gymIdFromQuery?.trim();
    if (fromQuery) {
      return fromQuery;
    }
    const fromJwt = jwtGymId?.trim();
    if (fromJwt) {
      return fromJwt;
    }

    const rows = await this.prisma.gymUser.findMany({
      where: {
        userId: actorUserId,
        role: GymRole.MEMBER,
        isActive: true,
      },
      select: { gymId: true },
    });
    const gymIds = [...new Set(rows.map((r) => r.gymId))];
    if (gymIds.length === 1) {
      return gymIds[0]!;
    }
    return null;
  }

  async listMeals(actorUserId: string, query: ListDietMealsQueryDto) {
    let gymWhere: Prisma.DietMealWhereInput;

    if (query.member_id) {
      const m = await this.prisma.gymUser.findFirst({
        where: {
          id: query.member_id,
          role: GymRole.MEMBER,
        },
        select: { id: true, gymId: true },
      });
      if (!m) {
        throw new NotFoundException('Member not found');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, m.gymId);
      gymWhere = { gymId: m.gymId, gymUserId: query.member_id };
    } else {
      const trimmedGym = query.gymId?.trim();
      if (trimmedGym) {
        try {
          await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
          gymWhere = { gymId: trimmedGym };
        } catch (e) {
          if (!(e instanceof ForbiddenException)) {
            throw e;
          }
          await this.gymAccess.assertMemberAtGym(actorUserId, trimmedGym);
          gymWhere = { userId: actorUserId, gymId: trimmedGym };
        }
      } else {
        try {
          const scope = await this.gymAccess.resolveGymScopeForManageRead(
            actorUserId,
            undefined,
          );
          gymWhere =
            scope.mode === 'single'
              ? { gymId: scope.gymId }
              : { gymId: { in: scope.gymIds } };
        } catch (e) {
          if (e instanceof BadRequestException) {
            throw e;
          }
          if (!(e instanceof ForbiddenException)) {
            throw e;
          }
          gymWhere = { userId: actorUserId };
        }
      }
    }

    const createdByFilter = this.dietMealCreatedByWhere(query.created_by);

    const rows = await this.prisma.dietMeal.findMany({
      where:
        createdByFilter == null
          ? gymWhere
          : { AND: [gymWhere, createdByFilter] },
      orderBy: { createdAt: 'desc' },
      include: { foodLines: dietMealFoodLinesInclude },
      take: 200,
    });
    return rows.map((m) => this.mealToApi(m));
  }

  private dietMealCreatedByWhere(
    createdBy?: string,
  ): Prisma.DietMealWhereInput | null {
    const v = createdBy?.trim().toLowerCase();
    if (!v || v === 'all') {
      return null;
    }
    if (v === 'member') {
      return { createdByRole: GymRole.MEMBER };
    }
    if (v === 'trainer') {
      return {
        createdByRole: {
          in: [GymRole.OWNER, GymRole.TRAINER, GymRole.STAFF],
        },
      };
    }
    return null;
  }

  async getMeal(actorUserId: string, mealId: string, gymId?: string) {
    const row = await this.prisma.dietMeal.findFirst({
      where: { id: mealId },
      select: { gymId: true, userId: true },
    });
    if (!row) {
      throw new NotFoundException('Meal not found');
    }
    await this.assertDietMealRowAccess(actorUserId, gymId, row);
    const meal = await this.prisma.dietMeal.findFirstOrThrow({
      where: { id: mealId, gymId: row.gymId },
      include: { foodLines: dietMealFoodLinesInclude },
    });
    return this.mealToApi(meal);
  }

  async updateMeal(
    actorUserId: string,
    mealId: string,
    dto: UpdateDietMealDto,
    gymId?: string,
  ) {
    const existing = await this.prisma.dietMeal.findFirst({
      where: { id: mealId },
      select: { gymId: true, userId: true },
    });
    if (!existing) {
      throw new NotFoundException('Meal not found');
    }
    await this.assertDietMealRowAccess(actorUserId, gymId, existing);

    const hasScalar =
      dto.name !== undefined ||
      dto.time !== undefined ||
      dto.meal_type !== undefined ||
      dto.created_by !== undefined ||
      dto.repeat_enabled !== undefined ||
      dto.repeat_days !== undefined;
    const hasFood = dto.food_items !== undefined;
    if (!hasScalar && !hasFood) {
      throw new BadRequestException('No fields to update');
    }

    await this.prisma.$transaction(async (tx) => {
      if (hasFood) {
        await tx.dietMealFoodLine.deleteMany({ where: { mealId } });
        const rows = await this.buildFoodLineRows(
          existing.gymId,
          dto.food_items!,
          existing.userId,
        );
        if (rows.length) {
          await tx.dietMealFoodLine.createMany({
            data: rows.map((r) => ({
              ...r,
              mealId,
            })),
          });
        }
      }

      const mealUpdate: Prisma.DietMealUpdateInput = {
        ...(dto.name !== undefined ? { name: dto.name.trim() } : {}),
        ...(dto.time !== undefined ? { mealTime: dto.time.trim() } : {}),
        ...(dto.meal_type !== undefined ? { mealType: dto.meal_type } : {}),
        ...(dto.repeat_enabled !== undefined
          ? { repeatEnabled: dto.repeat_enabled }
          : {}),
        ...(dto.repeat_days !== undefined
          ? { repeatDays: this.normalizeRepeatDays(dto.repeat_days) }
          : {}),
      };
      if (dto.created_by !== undefined) {
        mealUpdate.createdByRole = await this.resolveDietCreatedByRole(
          actorUserId,
          existing.gymId,
          dto.created_by,
        );
      }

      await tx.dietMeal.update({
        where: { id: mealId },
        data: mealUpdate,
      });
    });

    const meal = await this.prisma.dietMeal.findFirstOrThrow({
      where: { id: mealId, gymId: existing.gymId },
      include: { foodLines: dietMealFoodLinesInclude },
    });
    return this.mealToApi(meal);
  }

  async deleteMeal(actorUserId: string, mealId: string, gymId?: string) {
    const existing = await this.prisma.dietMeal.findFirst({
      where: { id: mealId },
      select: { gymId: true, userId: true },
    });
    if (!existing) {
      throw new NotFoundException('Meal not found');
    }
    await this.assertDietMealRowAccess(actorUserId, gymId, existing);
    const r = await this.prisma.dietMeal.deleteMany({
      where: { id: mealId, gymId: existing.gymId },
    });
    if (r.count === 0) {
      throw new NotFoundException('Meal not found');
    }
    return { success: true as const };
  }

  /**
   * With `gymId` (query or JWT for writes): store `gymId` + `userId` = actor and require gym manage access.
   * Without `gymId` query: personal catalog — `gymId: null`, `userId` = actor (JWT `gymId` ignored for this split).
   */
  async createFood(
    actorUserId: string,
    dto: CreateDietFoodDto,
    gymIdFromQuery?: string,
    _jwtGymId?: string | null,
  ) {
    const trimmedGym = gymIdFromQuery?.trim();
    if (trimmedGym) {
      await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
      const row = await this.prisma.dietFood.create({
        data: this.buildFoodCreateData(actorUserId, trimmedGym, dto),
      });
      return this.foodToApi(row);
    }
    const row = await this.prisma.dietFood.create({
      data: this.buildFoodCreateData(actorUserId, null, dto),
    });
    return this.foodToApi(row);
  }

  async listFoods(actorUserId: string, query: ListDietFoodQueryDto) {
    const trimmedGym = query.gymId?.trim();
    const search = query.search?.trim();

    const base: Prisma.DietFoodWhereInput = {
      userId: actorUserId,
      ...(search
        ? { name: { contains: search, mode: 'insensitive' } }
        : undefined),
    };

    let where: Prisma.DietFoodWhereInput;
    if (trimmedGym) {
      await this.gymAccess.assertCanManageGym(actorUserId, trimmedGym);
      where = { ...base, gymId: trimmedGym };
    } else {
      where = { ...base, gymId: null };
    }

    const rows = await this.prisma.dietFood.findMany({
      where,
      orderBy: { name: 'asc' },
      take: 500,
    });
    return rows.map((f) => this.foodToApi(f));
  }

  async getFood(actorUserId: string, foodId: string, gymIdFromQuery?: string) {
    const row = await this.prisma.dietFood.findFirst({
      where: { id: foodId },
    });
    if (!row) {
      throw new NotFoundException('Food not found');
    }
    this.assertDietFoodOwner(row, actorUserId);
    const q = gymIdFromQuery?.trim();
    if (q) {
      if (row.gymId !== q) {
        throw new BadRequestException('gymId does not match this food');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, q);
    } else if (row.gymId != null) {
      throw new BadRequestException(
        'This food is gym-scoped; pass gymId as a query parameter',
      );
    }
    return this.foodToApi(row);
  }

  async updateFood(
    actorUserId: string,
    foodId: string,
    dto: UpdateDietFoodDto,
    gymId?: string,
  ) {
    const has =
      dto.name !== undefined ||
      dto.weight_kg !== undefined ||
      dto.calories !== undefined ||
      dto.protein !== undefined ||
      dto.carbs !== undefined ||
      dto.fat !== undefined ||
      dto.unit_type !== undefined ||
      dto.quantity !== undefined ||
      dto.image_url !== undefined;
    if (!has) {
      throw new BadRequestException('No fields to update');
    }

    const existing = await this.prisma.dietFood.findFirst({
      where: { id: foodId },
    });
    if (!existing) {
      throw new NotFoundException('Food not found');
    }
    this.assertDietFoodOwner(existing, actorUserId);
    const gq = gymId?.trim();
    if (gq) {
      if (existing.gymId !== gq) {
        throw new BadRequestException('gymId does not match this food');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, gq);
    } else if (existing.gymId != null) {
      throw new BadRequestException(
        'This food is gym-scoped; pass gymId as a query parameter',
      );
    }

    const row = await this.prisma.dietFood.update({
      where: { id: foodId },
      data: {
        ...(dto.name !== undefined ? { name: dto.name.trim() } : {}),
        ...(dto.weight_kg !== undefined
          ? {
              weightKg:
                dto.weight_kg != null
                  ? new Prisma.Decimal(dto.weight_kg)
                  : null,
            }
          : {}),
        ...(dto.calories !== undefined ? { calories: dto.calories } : {}),
        ...(dto.protein !== undefined
          ? {
              protein:
                dto.protein != null ? new Prisma.Decimal(dto.protein) : null,
            }
          : {}),
        ...(dto.carbs !== undefined
          ? {
              carbs: dto.carbs != null ? new Prisma.Decimal(dto.carbs) : null,
            }
          : {}),
        ...(dto.fat !== undefined
          ? { fat: dto.fat != null ? new Prisma.Decimal(dto.fat) : null }
          : {}),
        ...(dto.unit_type !== undefined ? { unitType: dto.unit_type } : {}),
        ...(dto.quantity !== undefined ? { quantity: dto.quantity } : {}),
        ...(dto.image_url !== undefined
          ? { imageUrl: dto.image_url?.trim() ?? null }
          : {}),
      },
    });
    return this.foodToApi(row);
  }

  async deleteFood(
    actorUserId: string,
    foodId: string,
    gymIdFromQuery?: string,
  ) {
    const existing = await this.prisma.dietFood.findFirst({
      where: { id: foodId },
      select: { gymId: true, userId: true },
    });
    if (!existing) {
      throw new NotFoundException('Food not found');
    }
    this.assertDietFoodOwner(
      { userId: existing.userId, gymId: existing.gymId },
      actorUserId,
    );
    const gq = gymIdFromQuery?.trim();
    if (gq) {
      if (existing.gymId !== gq) {
        throw new BadRequestException('gymId does not match this food');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, gq);
    } else if (existing.gymId != null) {
      throw new BadRequestException(
        'This food is gym-scoped; pass gymId as a query parameter',
      );
    }
    const r = await this.prisma.dietFood.deleteMany({
      where: {
        id: foodId,
        userId: actorUserId,
        gymId: existing.gymId,
      },
    });
    if (r.count === 0) {
      throw new NotFoundException('Food not found');
    }
    return { success: true as const };
  }

  async recordFoodConsumption(
    actorUserId: string,
    dto: FoodConsumeDietDto,
    gymIdFromQuery?: string,
    memberIdFromQuery?: string,
  ) {
    const subject = await this.resolveDietLogSubjectForWrite(
      actorUserId,
      gymIdFromQuery,
      memberIdFromQuery,
    );
    const consumedOn = dto.consumed_on
      ? this.parseYmd(dto.consumed_on, 'consumed_on')
      : this.utcDateOnly(new Date());
    const consumedAt = dto.consumed_at ? new Date(dto.consumed_at) : new Date();
    if (Number.isNaN(consumedAt.getTime())) {
      throw new BadRequestException(
        'consumed_at must be a valid ISO date-time',
      );
    }

    const createRows: Prisma.DietFoodConsumptionCreateManyInput[] = [];
    for (const item of dto.items) {
      const row = await this.buildConsumptionLine(
        subject.rowGymId,
        subject.subjectUserId,
        dto.meal_type,
        consumedOn,
        consumedAt,
        item,
      );
      createRows.push(row);
    }

    const created = await this.prisma.$transaction(
      createRows.map((data) =>
        this.prisma.dietFoodConsumption.create({ data }),
      ),
    );
    return {
      success: true as const,
      count: created.length,
      ids: created.map((c) => c.id),
    };
  }

  /**
   * Sum consumed kcal + macro grams for one calendar day (same resolution as diet history).
   */
  async summarizeDailyConsumption(
    userId: string,
    options?: { gymId?: string; dateYmd?: string },
  ): Promise<{
    caloriesConsumed: number;
    proteinG: number;
    carbsG: number;
    fatG: number;
  }> {
    const dateStr = options?.dateYmd?.trim() || this.todayYmdUtc();
    const consumedOn = this.parseYmd(dateStr, 'date');
    const gymIdFilter = options?.gymId?.trim();

    const rows = await this.prisma.dietFoodConsumption.findMany({
      where: {
        userId,
        consumedOn,
        ...(gymIdFilter ? { gymId: gymIdFilter } : {}),
      },
      include: {
        dietFood: {
          select: { protein: true, carbs: true, fat: true },
        },
      },
    });

    const mealLineMacrosByDietFoodId = await this.mealLineMacrosByDietFoodId(
      userId,
      gymIdFilter,
      rows.map((r) => r.dietFoodId).filter(Boolean) as string[],
    );

    let caloriesConsumed = 0;
    let proteinG = 0;
    let carbsG = 0;
    let fatG = 0;
    for (const r of rows) {
      caloriesConsumed += r.calories;
      const macros = this.resolveConsumptionLineMacros({
        proteinG: r.proteinG,
        carbsG: r.carbsG,
        fatG: r.fatG,
        quantity: r.quantity,
        dietFood: r.dietFood,
        mealLine: r.dietFoodId
          ? mealLineMacrosByDietFoodId.get(r.dietFoodId) ?? null
          : null,
      });
      proteinG += macros.protein;
      carbsG += macros.carbs;
      fatG += macros.fat;
    }

    return { caloriesConsumed, proteinG, carbsG, fatG };
  }

  /**
   * Daily Diet History: calorie summary, macro totals, and meals grouped by time of log.
   */
  async getDietHistory(actorUserId: string, query: DietHistoryQueryDto) {
    const dateStr = query.date?.trim() || this.todayYmdUtc();
    const consumedOn = this.parseYmd(dateStr, 'date');
    const targetKcal = query.target_kcal ?? 2000;
    const gymIdFilter = query.gymId?.trim();

    if (gymIdFilter) {
      await this.gymAccess.assertMemberAtGym(actorUserId, gymIdFilter);
    }

    let tz = 'Asia/Kolkata';
    if (gymIdFilter) {
      const gym = await this.prisma.gym.findFirst({
        where: { id: gymIdFilter },
        select: { timezone: true },
      });
      tz = this.resolveDietDisplayTimeZone(gym?.timezone);
    }

    const rows = await this.prisma.dietFoodConsumption.findMany({
      where: {
        userId: actorUserId,
        consumedOn,
        ...(gymIdFilter ? { gymId: gymIdFilter } : {}),
      },
      orderBy: [{ consumedAt: 'asc' }, { id: 'asc' }],
      include: {
        dietFood: {
          select: { protein: true, carbs: true, fat: true },
        },
      },
    });

    const mealLineMacrosByDietFoodId = await this.mealLineMacrosByDietFoodId(
      actorUserId,
      gymIdFilter,
      rows.map((r) => r.dietFoodId).filter(Boolean) as string[],
    );

    let totalCal = 0;
    let protein = 0;
    let carbs = 0;
    let fat = 0;
    for (const r of rows) {
      totalCal += r.calories;
      const macros = this.resolveConsumptionLineMacros({
        proteinG: r.proteinG,
        carbsG: r.carbsG,
        fatG: r.fatG,
        quantity: r.quantity,
        dietFood: r.dietFood,
        mealLine: r.dietFoodId
          ? mealLineMacrosByDietFoodId.get(r.dietFoodId) ?? null
          : null,
      });
      protein += macros.protein;
      carbs += macros.carbs;
      fat += macros.fat;
    }
    const remainingKcal = Math.max(0, targetKcal - totalCal);

    const mealGroups = new Map<
      string,
      { mealType: DietMealType; tMs: number; at: Date; lineIds: string[] }
    >();
    for (const r of rows) {
      const tMs = Math.floor(r.consumedAt.getTime() / 60_000) * 60_000;
      const key = `${r.mealType}|${tMs}`;
      const existing = mealGroups.get(key);
      if (!existing) {
        mealGroups.set(key, {
          mealType: r.mealType,
          tMs,
          at: r.consumedAt,
          lineIds: [r.id],
        });
      } else {
        existing.lineIds.push(r.id);
      }
    }

    const groupsSorted = [...mealGroups.values()].sort((a, b) => a.tMs - b.tMs);
    const byId = new Map(rows.map((r) => [r.id, r]));
    const mealLogs = groupsSorted.map((g) => {
      const lines = g.lineIds
        .map((id) => byId.get(id))
        .filter((x): x is NonNullable<typeof x> => x != null);
      let groupKcal = 0;
      const items = lines.map((line) => {
        groupKcal += line.calories;
        const macros = this.resolveConsumptionLineMacros({
          proteinG: line.proteinG,
          carbsG: line.carbsG,
          fatG: line.fatG,
          quantity: line.quantity,
          dietFood: line.dietFood,
          mealLine: line.dietFoodId
            ? mealLineMacrosByDietFoodId.get(line.dietFoodId) ?? null
            : null,
        });
        return {
          id: line.id,
          diet_food_id: line.dietFoodId,
          name: line.name,
          amount_display: this.amountDisplay(line),
          calories: line.calories,
          protein_g: round1(macros.protein),
          carbs_g: round1(macros.carbs),
          fat_g: round1(macros.fat),
          image_url: line.imageUrl,
        };
      });
      return {
        meal_type: g.mealType,
        meal_label: dietMealTypeToLabel(g.mealType),
        time: this.formatTime12h(g.at, tz),
        total_calories: groupKcal,
        items,
      };
    });

    const recurringBlock = await this.buildRecurringMealsForHistoryDate(
      actorUserId,
      gymIdFilter,
      consumedOn,
    );

    return {
      date: dateStr,
      user_id: actorUserId,
      daily_summary: {
        target_kcal: targetKcal,
        consumed_kcal: totalCal,
        remaining_kcal: remainingKcal,
      },
      macros: {
        protein_g: round1(protein),
        carbs_g: round1(carbs),
        fat_g: round1(fat),
      },
      meal_logs: mealLogs,
      recurring_meals: recurringBlock.recurring_meals,
      recurring_summary: recurringBlock.recurring_summary,
    };
  }

  /**
   * Weekly scheduled meals for `date` (`repeat_days`: 0 = Monday … 6 = Sunday).
   * Separate from consumed `meal_logs` — does not change `macros` / `daily_summary`.
   */
  private async buildRecurringMealsForHistoryDate(
    userId: string,
    gymId: string | undefined,
    consumedOn: Date,
  ): Promise<{
    recurring_meals: Array<Record<string, unknown>>;
    recurring_summary: {
      repeat_day_index: number;
      meal_count: number;
      total_calories: number;
      protein_g: number;
      carbs_g: number;
      fat_g: number;
    };
  }> {
    const repeatDayIndex = this.utcDateToRepeatDayIndex(consumedOn);
    const gymIdFilter = gymId?.trim();

    const rows = await this.prisma.dietMeal.findMany({
      where: {
        userId,
        repeatEnabled: true,
        repeatDays: { has: repeatDayIndex },
        ...(gymIdFilter ? { gymId: gymIdFilter } : {}),
      },
      include: { foodLines: dietMealFoodLinesInclude },
      orderBy: [{ mealTime: 'asc' }, { createdAt: 'asc' }],
    });

    let summaryCal = 0;
    let summaryProtein = 0;
    let summaryCarbs = 0;
    let summaryFat = 0;

    const recurring_meals = rows.map((m) => {
      const entry = this.recurringMealForHistory(m);
      summaryCal += entry.total_calories as number;
      const macros = entry.macros as {
        protein_g: number;
        carbs_g: number;
        fat_g: number;
      };
      summaryProtein += macros.protein_g;
      summaryCarbs += macros.carbs_g;
      summaryFat += macros.fat_g;
      return entry;
    });

    return {
      recurring_meals,
      recurring_summary: {
        repeat_day_index: repeatDayIndex,
        meal_count: recurring_meals.length,
        total_calories: summaryCal,
        protein_g: round1(summaryProtein),
        carbs_g: round1(summaryCarbs),
        fat_g: round1(summaryFat),
      },
    };
  }

  private recurringMealForHistory(m: DietMealPayload): Record<string, unknown> {
    let totalCal = 0;
    let protein = 0;
    let carbs = 0;
    let fat = 0;
    const food_items = m.foodLines.map((f) => {
      const item = this.foodLineToApi(f);
      totalCal += item.calories;
      protein += item.protein ?? 0;
      carbs += item.carbs ?? 0;
      fat += item.fat ?? 0;
      return item;
    });
    return {
      meal_id: m.id,
      name: m.name,
      time: this.normalizeMealTimeDisplay(m.mealTime),
      meal_type: m.mealType,
      meal_label: dietMealTypeToLabel(m.mealType),
      repeat_enabled: m.repeatEnabled,
      repeat_days: m.repeatDays,
      created_by: this.dietCreatedByBucket(m.createdByRole),
      created_by_role: m.createdByRole,
      food_items,
      total_calories: totalCal,
      macros: {
        protein_g: round1(protein),
        carbs_g: round1(carbs),
        fat_g: round1(fat),
      },
    };
  }

  /** `repeat_days` index for a UTC calendar date (0 = Monday … 6 = Sunday). */
  private utcDateToRepeatDayIndex(d: Date): number {
    const jsDay = d.getUTCDay();
    return jsDay === 0 ? 6 : jsDay - 1;
  }

  /**
   * Who is being logged, and optional gym id stored on the row (only if `gymId` in query).
   * History is read by JWT user id; writes use `userId` on each row.
   */
  private async resolveDietLogSubjectForWrite(
    actorUserId: string,
    gymIdFromQuery: string | undefined,
    memberIdFromQuery: string | undefined,
  ): Promise<{
    subjectUserId: string;
    rowGymId: string | null;
  }> {
    const rowGymId = gymIdFromQuery?.trim() || null;
    if (memberIdFromQuery?.trim()) {
      const m = await this.prisma.gymUser.findFirst({
        where: {
          id: memberIdFromQuery.trim(),
          role: GymRole.MEMBER,
          isActive: true,
        },
        select: { userId: true, gymId: true },
      });
      if (!m) {
        throw new NotFoundException('Member not found');
      }
      await this.gymAccess.assertCanManageGym(actorUserId, m.gymId);
      return { subjectUserId: m.userId, rowGymId };
    }
    return { subjectUserId: actorUserId, rowGymId };
  }

  private todayYmdUtc(): string {
    const d = new Date();
    return [
      d.getUTCFullYear(),
      String(d.getUTCMonth() + 1).padStart(2, '0'),
      String(d.getUTCDate()).padStart(2, '0'),
    ].join('-');
  }

  private parseYmd(s: string, field: string): Date {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) {
      throw new BadRequestException(`${field} must be YYYY-MM-DD`);
    }
    const [y, m, d] = s.split('-').map((x) => parseInt(x, 10));
    if (m < 1 || m > 12 || d < 1 || d > 31) {
      throw new BadRequestException(`${field} is not a valid calendar date`);
    }
    return new Date(Date.UTC(y, m - 1, d));
  }

  private utcDateOnly(d: Date): Date {
    return new Date(
      Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()),
    );
  }

  private async buildConsumptionLine(
    gymId: string | null,
    subjectUserId: string,
    mealType: DietMealType,
    consumedOn: Date,
    consumedAt: Date,
    item: ConsumeDietFoodItemDto,
  ): Promise<Prisma.DietFoodConsumptionCreateManyInput> {
    const quantity = item.quantity ?? 1;
    let name: string | null = item.name?.trim() ?? null;
    let weightKg: Prisma.Decimal | null =
      item.weight_kg != null ? new Prisma.Decimal(item.weight_kg) : null;
    const dietFoodId = item.diet_food_id?.trim() ?? null;
    let imageUrl: string | null = null;
    let catalogProtein: Prisma.Decimal | null = null;
    let catalogCarbs: Prisma.Decimal | null = null;
    let catalogFat: Prisma.Decimal | null = null;
    let mealLineMacros: {
      protein: Prisma.Decimal | null;
      carbs: Prisma.Decimal | null;
      fat: Prisma.Decimal | null;
    } | null = null;

    if (dietFoodId) {
      const cat = await this.prisma.dietFood.findFirst({
        where: { id: dietFoodId },
        select: {
          gymId: true,
          userId: true,
          name: true,
          weightKg: true,
          imageUrl: true,
          protein: true,
          carbs: true,
          fat: true,
        },
      });
      if (!cat) {
        throw new BadRequestException(`Invalid diet_food_id: ${dietFoodId}`);
      }
      if (cat.gymId != null) {
        if (!gymId || cat.gymId !== gymId) {
          throw new BadRequestException(
            'Gym catalog items require a matching `gymId` query (or the food is from another gym)',
          );
        }
      } else if (cat.userId !== subjectUserId) {
        throw new BadRequestException(
          'diet_food_id is not in this user’s personal catalog',
        );
      }
      if (!name) {
        name = cat.name;
      }
      if (item.weight_kg == null && cat.weightKg != null) {
        weightKg = cat.weightKg;
      }
      imageUrl = cat.imageUrl;
      catalogProtein = cat.protein;
      catalogCarbs = cat.carbs;
      catalogFat = cat.fat;

      const mealLine = await this.prisma.dietMealFoodLine.findFirst({
        where: {
          dietFoodId,
          userId: subjectUserId,
          meal: { gymId: gymId ?? null },
        },
        orderBy: { meal: { updatedAt: 'desc' } },
        select: { protein: true, carbs: true, fat: true },
      });
      mealLineMacros = mealLine;
    }
    if (!name) {
      throw new BadRequestException(
        'Each item needs `name` or a valid `diet_food_id`',
      );
    }

    const resolvedMacros = this.resolveConsumptionLineMacros({
      proteinG:
        item.protein_g != null
          ? new Prisma.Decimal(item.protein_g)
          : item.protein != null
            ? new Prisma.Decimal(item.protein)
            : null,
      carbsG:
        item.carbs_g != null
          ? new Prisma.Decimal(item.carbs_g)
          : item.carbs != null
            ? new Prisma.Decimal(item.carbs)
            : null,
      fatG:
        item.fat_g != null
          ? new Prisma.Decimal(item.fat_g)
          : item.fat != null
            ? new Prisma.Decimal(item.fat)
            : null,
      quantity,
      dietFood: {
        protein: catalogProtein,
        carbs: catalogCarbs,
        fat: catalogFat,
      },
      mealLine: mealLineMacros,
    });

    const portionLabel = item.portion_label?.trim() || null;
    return {
      userId: subjectUserId,
      gymId,
      dietFoodId,
      name,
      weightKg,
      calories: item.calories,
      quantity,
      portionLabel,
      proteinG: new Prisma.Decimal(resolvedMacros.protein),
      carbsG: new Prisma.Decimal(resolvedMacros.carbs),
      fatG: new Prisma.Decimal(resolvedMacros.fat),
      mealType,
      consumedOn,
      consumedAt,
      imageUrl: imageUrl ?? null,
    };
  }

  private async mealLineMacrosByDietFoodId(
    userId: string,
    gymId: string | undefined,
    dietFoodIds: string[],
  ): Promise<
    Map<
      string,
      {
        protein: Prisma.Decimal | null;
        carbs: Prisma.Decimal | null;
        fat: Prisma.Decimal | null;
      }
    >
  > {
    if (!dietFoodIds.length) {
      return new Map();
    }
    const lines = await this.prisma.dietMealFoodLine.findMany({
      where: {
        dietFoodId: { in: dietFoodIds },
        userId,
        meal: gymId ? { gymId } : { gymId: null },
      },
      orderBy: { meal: { updatedAt: 'desc' } },
      select: {
        dietFoodId: true,
        protein: true,
        carbs: true,
        fat: true,
      },
    });
    const map = new Map<
      string,
      {
        protein: Prisma.Decimal | null;
        carbs: Prisma.Decimal | null;
        fat: Prisma.Decimal | null;
      }
    >();
    for (const line of lines) {
      if (!line.dietFoodId || map.has(line.dietFoodId)) {
        continue;
      }
      map.set(line.dietFoodId, {
        protein: line.protein,
        carbs: line.carbs,
        fat: line.fat,
      });
    }
    return map;
  }

  /**
   * Macros on a consumption row: explicit values win; else scheduled meal line;
   * else diet food catalog; scaled by consumption `quantity`.
   */
  private resolveConsumptionLineMacros(line: {
    proteinG: Prisma.Decimal | null;
    carbsG: Prisma.Decimal | null;
    fatG: Prisma.Decimal | null;
    quantity: number;
    dietFood?: {
      protein: Prisma.Decimal | null;
      carbs: Prisma.Decimal | null;
      fat: Prisma.Decimal | null;
    } | null;
    mealLine?: {
      protein: Prisma.Decimal | null;
      carbs: Prisma.Decimal | null;
      fat: Prisma.Decimal | null;
    } | null;
  }): { protein: number; carbs: number; fat: number } {
    const qty = line.quantity ?? 1;
    const resolveOne = (
      stored: Prisma.Decimal | null,
      mealLine: Prisma.Decimal | null | undefined,
      catalog: Prisma.Decimal | null | undefined,
    ): number => {
      if (stored != null) {
        return Number(stored);
      }
      if (mealLine != null) {
        return Number(mealLine) * qty;
      }
      if (catalog != null) {
        return Number(catalog) * qty;
      }
      return 0;
    };
    return {
      protein: resolveOne(
        line.proteinG,
        line.mealLine?.protein,
        line.dietFood?.protein,
      ),
      carbs: resolveOne(line.carbsG, line.mealLine?.carbs, line.dietFood?.carbs),
      fat: resolveOne(line.fatG, line.mealLine?.fat, line.dietFood?.fat),
    };
  }

  private amountDisplay(line: {
    portionLabel: string | null;
    weightKg: Prisma.Decimal | null;
    quantity: number;
  }): string {
    if (line.portionLabel) {
      return line.portionLabel;
    }
    if (line.weightKg != null) {
      const g = Math.round(Number(line.weightKg) * 1000);
      return `${g}g`;
    }
    return `${line.quantity} pcs`;
  }

  private resolveDietDisplayTimeZone(gymTimezone: string | null | undefined): string {
    const tz = gymTimezone?.trim();
    if (!tz || tz === 'UTC') {
      return 'Asia/Kolkata';
    }
    return tz;
  }

  /** Scheduled meal time string → `07:30 AM` (local wall-clock, zero-padded). */
  private normalizeMealTimeDisplay(raw: string): string {
    const trimmed = raw.trim();
    if (!trimmed) {
      return trimmed;
    }

    const m12 = /^(\d{1,2}):(\d{2})\s*(AM|PM)$/i.exec(trimmed);
    if (m12) {
      const hour = Math.min(12, Math.max(1, parseInt(m12[1], 10)));
      const minute = m12[2];
      const ampm = m12[3].toUpperCase();
      return `${String(hour).padStart(2, '0')}:${minute} ${ampm}`;
    }

    const m24 = /^(\d{1,2}):(\d{2})(?::(\d{2}))?$/.exec(trimmed);
    if (m24) {
      const hour24 = Math.min(23, Math.max(0, parseInt(m24[1], 10)));
      const minute = m24[2];
      const hour12 = hour24 % 12 || 12;
      const ampm = hour24 >= 12 ? 'PM' : 'AM';
      return `${String(hour12).padStart(2, '0')}:${minute} ${ampm}`;
    }

    return trimmed;
  }

  private formatTime12h(d: Date, timeZone: string): string {
    return d.toLocaleTimeString('en-US', {
      timeZone,
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  }

  private assertDietFoodOwner(
    row: { userId: string; gymId?: string | null },
    actorUserId: string,
  ): void {
    if (row.userId !== actorUserId) {
      throw new ForbiddenException(
        'You can only access your own diet food entries',
      );
    }
  }

  private normalizeRepeatDays(days?: number[]): number[] {
    if (!days?.length) {
      return [];
    }
    const uniq = [...new Set(days)].filter((d) => d >= 0 && d <= 6);
    return uniq.sort((a, b) => a - b);
  }

  private async buildFoodLineRows(
    gymId: string | null,
    items: DietMealFoodItemDto[],
    lineUserId: string,
  ): Promise<
    Array<{
      userId: string;
      name: string;
      weightKg: Prisma.Decimal | null;
      calories: number;
      protein: Prisma.Decimal | null;
      carbs: Prisma.Decimal | null;
      fat: Prisma.Decimal | null;
      unitType: DietFoodUnitType | null;
      quantity: number;
      dietFoodId: string | null;
      sortOrder: number;
    }>
  > {
    const out: Array<{
      userId: string;
      name: string;
      weightKg: Prisma.Decimal | null;
      calories: number;
      protein: Prisma.Decimal | null;
      carbs: Prisma.Decimal | null;
      fat: Prisma.Decimal | null;
      unitType: DietFoodUnitType | null;
      quantity: number;
      dietFoodId: string | null;
      sortOrder: number;
    }> = [];
    let order = 0;
    for (const item of items) {
      let name = item.name?.trim();
      let weightKg: Prisma.Decimal | null =
        item.weight_kg != null ? new Prisma.Decimal(item.weight_kg) : null;
      let protein: Prisma.Decimal | null =
        item.protein != null ? new Prisma.Decimal(item.protein) : null;
      let carbs: Prisma.Decimal | null =
        item.carbs != null ? new Prisma.Decimal(item.carbs) : null;
      let fat: Prisma.Decimal | null =
        item.fat != null ? new Prisma.Decimal(item.fat) : null;
      let unitType: DietFoodUnitType | null = item.unit_type ?? null;
      const calories = item.calories;
      const quantity = item.quantity;
      const dietFoodId = item.diet_food_id ?? null;

      if (dietFoodId) {
        const cat = await this.prisma.dietFood.findFirst({
          where: { id: dietFoodId },
        });
        if (!cat) {
          throw new BadRequestException(`Invalid diet_food_id: ${dietFoodId}`);
        }
        if (!name) {
          name = cat.name;
        }
        if (item.weight_kg == null && cat.weightKg != null) {
          weightKg = cat.weightKg;
        }
        if (item.protein == null && cat.protein != null) {
          protein = cat.protein;
        }
        if (item.carbs == null && cat.carbs != null) {
          carbs = cat.carbs;
        }
        if (item.fat == null && cat.fat != null) {
          fat = cat.fat;
        }
        if (item.unit_type == null && cat.unitType != null) {
          unitType = cat.unitType;
        }
      }

      if (!name) {
        throw new BadRequestException(
          'Each food item needs `name` or a valid `diet_food_id`',
        );
      }

      out.push({
        userId: lineUserId,
        name,
        weightKg,
        calories,
        protein,
        carbs,
        fat,
        unitType,
        quantity,
        dietFoodId,
        sortOrder: order++,
      });
    }
    return out;
  }

  private mealToApi(m: DietMealPayload): Record<string, unknown> {
    return {
      id: m.id,
      member_id: m.gymUserId ?? null,
      name: m.name,
      time: m.mealTime,
      meal_type: m.mealType,
      repeat_enabled: m.repeatEnabled,
      repeat_days: m.repeatDays,
      food_items: m.foodLines.map((f) => this.foodLineToApi(f)),
      created_by: this.dietCreatedByBucket(m.createdByRole),
      created_by_role: m.createdByRole,
      created_at: m.createdAt.toISOString(),
      updated_at: m.updatedAt.toISOString(),
    };
  }

  private dietCreatedByBucket(role: string): 'trainer' | 'member' {
    return role === GymRole.MEMBER ? 'member' : 'trainer';
  }

  private async resolveDietCreatedByRole(
    actorUserId: string,
    gymId: string | null,
    createdBy?: string,
  ): Promise<GymRole> {
    const resolved = await this.gymAccess.resolveActorGymRole(
      actorUserId,
      gymId,
    );
    const bucket = createdBy?.trim().toLowerCase();
    if (!bucket) {
      return resolved;
    }
    if (bucket === 'member') {
      return GymRole.MEMBER;
    }
    if (bucket === 'trainer') {
      if (
        resolved === GymRole.OWNER ||
        resolved === GymRole.TRAINER ||
        resolved === GymRole.STAFF
      ) {
        return resolved;
      }
      return GymRole.TRAINER;
    }
    return resolved;
  }

  private foodLineToApi(f: DietMealFoodLinePayload) {
    const catalog = f.dietFood;
    const protein =
      f.protein ?? catalog?.protein ?? null;
    const carbs = f.carbs ?? catalog?.carbs ?? null;
    const fat = f.fat ?? catalog?.fat ?? null;
    const unitType = f.unitType ?? catalog?.unitType ?? null;
    return {
      id: f.id,
      diet_food_id: f.dietFoodId,
      name: f.name,
      weight_kg: f.weightKg != null ? Number(f.weightKg) : null,
      calories: f.calories,
      protein: protein != null ? Number(protein) : null,
      carbs: carbs != null ? Number(carbs) : null,
      fat: fat != null ? Number(fat) : null,
      unit_type: unitType,
      quantity: f.quantity,
    };
  }

  private buildFoodCreateData(
    userId: string,
    gymId: string | null,
    dto: CreateDietFoodDto,
  ): Prisma.DietFoodUncheckedCreateInput {
    return {
      gymId,
      userId,
      name: dto.name.trim(),
      weightKg:
        dto.weight_kg != null ? new Prisma.Decimal(dto.weight_kg) : null,
      calories: dto.calories,
      protein: dto.protein != null ? new Prisma.Decimal(dto.protein) : null,
      carbs: dto.carbs != null ? new Prisma.Decimal(dto.carbs) : null,
      fat: dto.fat != null ? new Prisma.Decimal(dto.fat) : null,
      unitType: dto.unit_type ?? null,
      quantity: dto.quantity ?? 1,
      imageUrl: dto.image_url?.trim() ?? null,
    };
  }

  private foodToApi(f: {
    id: string;
    name: string;
    weightKg: Prisma.Decimal | null;
    calories: number;
    protein: Prisma.Decimal | null;
    carbs: Prisma.Decimal | null;
    fat: Prisma.Decimal | null;
    unitType: string | null;
    quantity: number;
    imageUrl: string | null;
    createdAt: Date;
    updatedAt: Date;
  }) {
    return {
      id: f.id,
      name: f.name,
      weight_kg: f.weightKg != null ? Number(f.weightKg) : null,
      calories: f.calories,
      protein: f.protein != null ? Number(f.protein) : null,
      carbs: f.carbs != null ? Number(f.carbs) : null,
      fat: f.fat != null ? Number(f.fat) : null,
      unit_type: f.unitType,
      quantity: f.quantity,
      image_url: f.imageUrl,
      created_at: f.createdAt.toISOString(),
      updated_at: f.updatedAt.toISOString(),
    };
  }
}

function round1(n: number): number {
  return Math.round(n * 10) / 10;
}

function dietMealTypeToLabel(m: DietMealType): string {
  const map: Record<DietMealType, string> = {
    [DietMealType.BREAKFAST]: 'Breakfast',
    [DietMealType.LUNCH]: 'Lunch',
    [DietMealType.DINNER]: 'Dinner',
    [DietMealType.SNACK]: 'Snack',
  };
  return map[m] ?? m;
}

type DietMealFoodLinePayload = {
  id: string;
  dietFoodId: string | null;
  name: string;
  weightKg: Prisma.Decimal | null;
  calories: number;
  protein: Prisma.Decimal | null;
  carbs: Prisma.Decimal | null;
  fat: Prisma.Decimal | null;
  unitType: DietFoodUnitType | null;
  quantity: number;
  dietFood?: {
    protein: Prisma.Decimal | null;
    carbs: Prisma.Decimal | null;
    fat: Prisma.Decimal | null;
    unitType: DietFoodUnitType | null;
  } | null;
};

type DietMealPayload = {
  id: string;
  gymUserId: string | null;
  name: string;
  mealTime: string;
  mealType: DietMealType;
  repeatEnabled: boolean;
  repeatDays: number[];
  createdByRole: string;
  createdAt: Date;
  updatedAt: Date;
  foodLines: DietMealFoodLinePayload[];
};
