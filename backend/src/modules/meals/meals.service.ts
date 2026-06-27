import { Injectable, NotFoundException } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type { AddFoodItemDto } from './dto/add-food-item.dto';
import type { CreateMealDto } from './dto/create-meal.dto';
import type { UpdateFoodItemDto } from './dto/update-food-item.dto';
import type { UpdateMealDto } from './dto/update-meal.dto';

@Injectable()
export class MealsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async createMeal(actorUserId: string, dto: CreateMealDto) {
    const member = await this.requireMemberOwnerAccess(
      actorUserId,
      dto.member_id,
    );
    const mealId = randomUUID();

    await this.prisma.$transaction(async (tx) => {
      await tx.$executeRaw(Prisma.sql`
        INSERT INTO member_meals (id, gym_id, gym_user_id, meal_name, meal_time, meal_type, repeat_days)
        VALUES (
          ${mealId},
          ${member.gymId},
          ${dto.member_id},
          ${dto.meal_name.trim()},
          ${dto.meal_time.trim()},
          ${dto.meal_type},
          ${JSON.stringify(dto.repeat_days ?? [])}::jsonb
        )
      `);

      for (const item of dto.food_items) {
        await tx.$executeRaw(Prisma.sql`
          INSERT INTO meal_food_items (id, meal_id, food_name, quantity, calories)
          VALUES (
            ${randomUUID()},
            ${mealId},
            ${item.food_name.trim()},
            ${item.quantity},
            ${item.calories}
          )
        `);
      }
    });

    return this.getMeal(actorUserId, mealId);
  }

  async listMealsByMember(actorUserId: string, gymUserId: string) {
    const member = await this.requireMemberOwnerAccess(actorUserId, gymUserId);
    const rows = await this.prisma.$queryRaw<
      Array<{
        meal_id: string;
        meal_name: string;
        meal_time: string;
        meal_type: string;
        total_calories: number;
        food_items_count: number;
      }>
    >(Prisma.sql`
      SELECT
        m.id AS meal_id,
        m.meal_name,
        m.meal_time,
        m.meal_type,
        COALESCE(SUM(f.calories), 0)::int AS total_calories,
        COUNT(f.id)::int AS food_items_count
      FROM member_meals m
      LEFT JOIN meal_food_items f ON f.meal_id = m.id
      WHERE m.gym_id = ${member.gymId}
        AND m.gym_user_id = ${gymUserId}
      GROUP BY m.id
      ORDER BY m.created_at DESC
    `);
    return rows;
  }

  async getMeal(actorUserId: string, mealId: string) {
    const meal = await this.prisma.$queryRaw<
      Array<{
        meal_id: string;
        gym_id: string;
        meal_name: string;
        meal_time: string;
        meal_type: string;
        repeat_days: Prisma.JsonValue;
      }>
    >(Prisma.sql`
      SELECT id AS meal_id, gym_id, meal_name, meal_time, meal_type, repeat_days
      FROM member_meals
      WHERE id = ${mealId}
      LIMIT 1
    `);
    const row = meal[0];
    if (!row) {
      throw new NotFoundException('Meal not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, row.gym_id);

    const foods = await this.prisma.$queryRaw<
      Array<{
        id: string;
        food_name: string;
        quantity: number;
        calories: number;
      }>
    >(Prisma.sql`
      SELECT id, food_name, quantity, calories
      FROM meal_food_items
      WHERE meal_id = ${mealId}
      ORDER BY created_at ASC
    `);

    return {
      meal_id: row.meal_id,
      meal_name: row.meal_name,
      meal_time: row.meal_time,
      meal_type: row.meal_type,
      repeat_days: Array.isArray(row.repeat_days) ? row.repeat_days : [],
      food_items: foods,
    };
  }

  async addFoodItem(actorUserId: string, mealId: string, dto: AddFoodItemDto) {
    await this.assertMealOwnerAccess(actorUserId, mealId);
    await this.prisma.$executeRaw(Prisma.sql`
      INSERT INTO meal_food_items (id, meal_id, food_name, quantity, calories)
      VALUES (${randomUUID()}, ${mealId}, ${dto.food_name.trim()}, ${dto.quantity}, ${dto.calories})
    `);
    return this.getMeal(actorUserId, mealId);
  }

  async updateFoodItem(
    actorUserId: string,
    foodId: string,
    dto: UpdateFoodItemDto,
  ) {
    const mealRef = await this.prisma.$queryRaw<Array<{ meal_id: string }>>(
      Prisma.sql`SELECT meal_id FROM meal_food_items WHERE id = ${foodId} LIMIT 1`,
    );
    const mealId = mealRef[0]?.meal_id;
    if (!mealId) {
      throw new NotFoundException('Food item not found');
    }
    await this.assertMealOwnerAccess(actorUserId, mealId);

    await this.prisma.$executeRaw(Prisma.sql`
      UPDATE meal_food_items
      SET
        quantity = COALESCE(${dto.quantity ?? null}, quantity),
        calories = COALESCE(${dto.calories ?? null}, calories)
      WHERE id = ${foodId}
    `);
    return this.getMeal(actorUserId, mealId);
  }

  async deleteFoodItem(actorUserId: string, foodId: string) {
    const mealRef = await this.prisma.$queryRaw<Array<{ meal_id: string }>>(
      Prisma.sql`SELECT meal_id FROM meal_food_items WHERE id = ${foodId} LIMIT 1`,
    );
    const mealId = mealRef[0]?.meal_id;
    if (!mealId) {
      throw new NotFoundException('Food item not found');
    }
    await this.assertMealOwnerAccess(actorUserId, mealId);
    await this.prisma.$executeRaw(
      Prisma.sql`DELETE FROM meal_food_items WHERE id = ${foodId}`,
    );
    return { success: true as const };
  }

  async updateMeal(actorUserId: string, mealId: string, dto: UpdateMealDto) {
    await this.assertMealOwnerAccess(actorUserId, mealId);
    await this.prisma.$executeRaw(Prisma.sql`
      UPDATE member_meals
      SET
        meal_name = COALESCE(${dto.meal_name?.trim() ?? null}, meal_name),
        meal_time = COALESCE(${dto.meal_time?.trim() ?? null}, meal_time),
        meal_type = COALESCE(${dto.meal_type ?? null}, meal_type),
        repeat_days = COALESCE(${dto.repeat_days ? JSON.stringify(dto.repeat_days) : null}::jsonb, repeat_days)
      WHERE id = ${mealId}
    `);
    return this.getMeal(actorUserId, mealId);
  }

  async deleteMeal(actorUserId: string, mealId: string) {
    await this.assertMealOwnerAccess(actorUserId, mealId);
    await this.prisma.$executeRaw(
      Prisma.sql`DELETE FROM member_meals WHERE id = ${mealId}`,
    );
    return { success: true as const };
  }

  async searchFoods(actorUserId: string, search: string) {
    const q = search.trim();
    if (!q) {
      return [];
    }
    const ownerGym = await this.prisma.gym.findFirst({
      where: { ownerId: actorUserId },
      select: { id: true },
    });
    if (!ownerGym) {
      return [];
    }

    const rows = await this.prisma.$queryRaw<
      Array<{ food_name: string; default_calories: number }>
    >(Prisma.sql`
      SELECT
        food_name,
        ROUND(AVG(calories))::int AS default_calories
      FROM meal_food_items fi
      JOIN member_meals m ON m.id = fi.meal_id
      WHERE m.gym_id = ${ownerGym.id}
        AND food_name ILIKE ${`%${q}%`}
      GROUP BY food_name
      ORDER BY food_name ASC
      LIMIT 20
    `);
    return rows;
  }

  private async requireMemberOwnerAccess(
    actorUserId: string,
    gymUserId: string,
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: gymUserId },
      select: { id: true, gymId: true, role: true },
    });
    if (!member || member.role !== 'MEMBER') {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);
    return member;
  }

  private async assertMealOwnerAccess(
    actorUserId: string,
    mealId: string,
  ): Promise<void> {
    const row = await this.prisma.$queryRaw<
      Array<{ gym_id: string }>
    >(Prisma.sql`
      SELECT gym_id FROM member_meals WHERE id = ${mealId} LIMIT 1
    `);
    const gymId = row[0]?.gym_id;
    if (!gymId) {
      throw new NotFoundException('Meal not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
  }
}
