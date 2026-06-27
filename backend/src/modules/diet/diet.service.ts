import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { DietMealType, GymRole, Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { GymAccessService } from '../../common/services/gym-access.service';
import type { CreateDietFoodDto } from './dto/create-diet-food.dto';
import type { CreateDietMealDto } from './dto/create-diet-meal.dto';
import type { DietMealFoodItemDto } from './dto/diet-meal-food-item.dto';
import type { ListDietFoodQueryDto } from './dto/list-diet-food-query.dto';
import type { ListDietMealsQueryDto } from './dto/list-diet-meals-query.dto';
import type { UpdateDietFoodDto } from './dto/update-diet-food.dto';
import type { UpdateDietMealDto } from './dto/update-diet-meal.dto';

@Injectable()
export class DietService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async createMeal(actorUserId: string, gymId: string, dto: CreateDietMealDto) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const member = await this.prisma.gymUser.findFirst({
      where: {
        id: dto.member_id,
        gymId,
        role: GymRole.MEMBER,
        isActive: true,
      },
      select: { id: true },
    });
    if (!member) {
      throw new NotFoundException('Member not found in this gym');
    }

    const repeatDays = this.normalizeRepeatDays(dto.repeat_days);
    const lineRows = await this.buildFoodLineRows(gymId, dto.food_items ?? []);

    const meal = await this.prisma.dietMeal.create({
      data: {
        gymId,
        gymUserId: member.id,
        name: dto.name.trim(),
        mealTime: dto.time.trim(),
        mealType: dto.meal_type,
        repeatEnabled: dto.repeat_enabled ?? false,
        repeatDays,
        foodLines: { create: lineRows },
      },
      include: { foodLines: { orderBy: { sortOrder: 'asc' } } },
    });
    return this.mealToApi(meal);
  }

  async listMeals(actorUserId: string, query: ListDietMealsQueryDto) {
    await this.gymAccess.assertCanManageGym(actorUserId, query.gymId);
    if (query.member_id) {
      const m = await this.prisma.gymUser.findFirst({
        where: {
          id: query.member_id,
          gymId: query.gymId,
          role: GymRole.MEMBER,
        },
        select: { id: true },
      });
      if (!m) {
        throw new NotFoundException('Member not found in this gym');
      }
    }

    const rows = await this.prisma.dietMeal.findMany({
      where: {
        gymId: query.gymId,
        ...(query.member_id ? { gymUserId: query.member_id } : {}),
      },
      orderBy: { createdAt: 'desc' },
      include: { foodLines: { orderBy: { sortOrder: 'asc' } } },
      take: 200,
    });
    return rows.map((m) => this.mealToApi(m));
  }

  async getMeal(actorUserId: string, gymId: string, mealId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const meal = await this.prisma.dietMeal.findFirst({
      where: { id: mealId, gymId },
      include: { foodLines: { orderBy: { sortOrder: 'asc' } } },
    });
    if (!meal) {
      throw new NotFoundException('Meal not found');
    }
    return this.mealToApi(meal);
  }

  async updateMeal(
    actorUserId: string,
    gymId: string,
    mealId: string,
    dto: UpdateDietMealDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const existing = await this.prisma.dietMeal.findFirst({
      where: { id: mealId, gymId },
    });
    if (!existing) {
      throw new NotFoundException('Meal not found');
    }

    const hasScalar =
      dto.name !== undefined ||
      dto.time !== undefined ||
      dto.meal_type !== undefined ||
      dto.repeat_enabled !== undefined ||
      dto.repeat_days !== undefined;
    const hasFood = dto.food_items !== undefined;
    if (!hasScalar && !hasFood) {
      throw new BadRequestException('No fields to update');
    }

    await this.prisma.$transaction(async (tx) => {
      if (hasFood) {
        await tx.dietMealFoodLine.deleteMany({ where: { mealId } });
        const rows = await this.buildFoodLineRows(gymId, dto.food_items!);
        if (rows.length) {
          await tx.dietMealFoodLine.createMany({
            data: rows.map((r) => ({ ...r, mealId })),
          });
        }
      }

      await tx.dietMeal.update({
        where: { id: mealId },
        data: {
          ...(dto.name !== undefined ? { name: dto.name.trim() } : {}),
          ...(dto.time !== undefined ? { mealTime: dto.time.trim() } : {}),
          ...(dto.meal_type !== undefined ? { mealType: dto.meal_type } : {}),
          ...(dto.repeat_enabled !== undefined
            ? { repeatEnabled: dto.repeat_enabled }
            : {}),
          ...(dto.repeat_days !== undefined
            ? { repeatDays: this.normalizeRepeatDays(dto.repeat_days) }
            : {}),
        },
      });
    });

    const meal = await this.prisma.dietMeal.findFirstOrThrow({
      where: { id: mealId, gymId },
      include: { foodLines: { orderBy: { sortOrder: 'asc' } } },
    });
    return this.mealToApi(meal);
  }

  async deleteMeal(actorUserId: string, gymId: string, mealId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const r = await this.prisma.dietMeal.deleteMany({
      where: { id: mealId, gymId },
    });
    if (r.count === 0) {
      throw new NotFoundException('Meal not found');
    }
    return { success: true as const };
  }

  async createFood(actorUserId: string, gymId: string, dto: CreateDietFoodDto) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.dietFood.create({
      data: {
        gymId,
        name: dto.name.trim(),
        weightKg:
          dto.weight_kg != null ? new Prisma.Decimal(dto.weight_kg) : null,
        calories: dto.calories,
        quantity: dto.quantity ?? 1,
        imageUrl: dto.image_url?.trim() ?? null,
      },
    });
    return this.foodToApi(row);
  }

  async listFoods(actorUserId: string, query: ListDietFoodQueryDto) {
    await this.gymAccess.assertCanManageGym(actorUserId, query.gymId);
    const search = query.search?.trim();
    const rows = await this.prisma.dietFood.findMany({
      where: {
        gymId: query.gymId,
        ...(search
          ? { name: { contains: search, mode: 'insensitive' } }
          : undefined),
      },
      orderBy: { name: 'asc' },
      take: 500,
    });
    return rows.map((f) => this.foodToApi(f));
  }

  async getFood(actorUserId: string, gymId: string, foodId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.dietFood.findFirst({
      where: { id: foodId, gymId },
    });
    if (!row) {
      throw new NotFoundException('Food not found');
    }
    return this.foodToApi(row);
  }

  async updateFood(
    actorUserId: string,
    gymId: string,
    foodId: string,
    dto: UpdateDietFoodDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const has =
      dto.name !== undefined ||
      dto.weight_kg !== undefined ||
      dto.calories !== undefined ||
      dto.quantity !== undefined ||
      dto.image_url !== undefined;
    if (!has) {
      throw new BadRequestException('No fields to update');
    }

    const existing = await this.prisma.dietFood.findFirst({
      where: { id: foodId, gymId },
    });
    if (!existing) {
      throw new NotFoundException('Food not found');
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
        ...(dto.quantity !== undefined ? { quantity: dto.quantity } : {}),
        ...(dto.image_url !== undefined
          ? { imageUrl: dto.image_url?.trim() ?? null }
          : {}),
      },
    });
    return this.foodToApi(row);
  }

  async deleteFood(actorUserId: string, gymId: string, foodId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const r = await this.prisma.dietFood.deleteMany({
      where: { id: foodId, gymId },
    });
    if (r.count === 0) {
      throw new NotFoundException('Food not found');
    }
    return { success: true as const };
  }

  private normalizeRepeatDays(days?: number[]): number[] {
    if (!days?.length) {
      return [];
    }
    const uniq = [...new Set(days)].filter((d) => d >= 0 && d <= 6);
    return uniq.sort((a, b) => a - b);
  }

  private async buildFoodLineRows(
    gymId: string,
    items: DietMealFoodItemDto[],
  ): Promise<
    Array<{
      name: string;
      weightKg: Prisma.Decimal | null;
      calories: number;
      quantity: number;
      dietFoodId: string | null;
      sortOrder: number;
    }>
  > {
    const out: Array<{
      name: string;
      weightKg: Prisma.Decimal | null;
      calories: number;
      quantity: number;
      dietFoodId: string | null;
      sortOrder: number;
    }> = [];
    let order = 0;
    for (const item of items) {
      let name = item.name?.trim();
      let weightKg: Prisma.Decimal | null =
        item.weight_kg != null ? new Prisma.Decimal(item.weight_kg) : null;
      const calories = item.calories;
      const quantity = item.quantity;
      const dietFoodId = item.diet_food_id ?? null;

      if (dietFoodId) {
        const cat = await this.prisma.dietFood.findFirst({
          where: { id: dietFoodId, gymId },
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
      }

      if (!name) {
        throw new BadRequestException(
          'Each food item needs `name` or a valid `diet_food_id`',
        );
      }

      out.push({
        name,
        weightKg,
        calories,
        quantity,
        dietFoodId,
        sortOrder: order++,
      });
    }
    return out;
  }

  private mealToApi(
    m: DietMealPayload,
  ): Record<string, unknown> {
    return {
      id: m.id,
      member_id: m.gymUserId,
      name: m.name,
      time: m.mealTime,
      meal_type: m.mealType,
      repeat_enabled: m.repeatEnabled,
      repeat_days: m.repeatDays,
      food_items: m.foodLines.map((f) => ({
        id: f.id,
        diet_food_id: f.dietFoodId,
        name: f.name,
        weight_kg: f.weightKg != null ? Number(f.weightKg) : null,
        calories: f.calories,
        quantity: f.quantity,
      })),
      created_at: m.createdAt.toISOString(),
      updated_at: m.updatedAt.toISOString(),
    };
  }

  private foodToApi(f: {
    id: string;
    name: string;
    weightKg: Prisma.Decimal | null;
    calories: number;
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
      quantity: f.quantity,
      image_url: f.imageUrl,
      created_at: f.createdAt.toISOString(),
      updated_at: f.updatedAt.toISOString(),
    };
  }
}

type DietMealPayload = {
  id: string;
  gymUserId: string;
  name: string;
  mealTime: string;
  mealType: DietMealType;
  repeatEnabled: boolean;
  repeatDays: number[];
  createdAt: Date;
  updatedAt: Date;
  foodLines: Array<{
    id: string;
    dietFoodId: string | null;
    name: string;
    weightKg: Prisma.Decimal | null;
    calories: number;
    quantity: number;
  }>;
};
