import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietMealType, GymRole } from '@prisma/client';

/** One line on a meal (`food_items[]` in API responses) */
export class DietMealFoodItemResponseSwagger {
  @ApiProperty({ description: 'Meal food line id' })
  id: string;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Linked catalog food id, if any',
  })
  diet_food_id: string | null;

  @ApiProperty()
  name: string;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Weight in kg (snapshot on the line)',
  })
  weight_kg: number | null;

  @ApiProperty()
  calories: number;

  @ApiPropertyOptional({ nullable: true, description: 'Protein (grams)' })
  protein: number | null;

  @ApiPropertyOptional({ nullable: true, description: 'Carbohydrates (grams)' })
  carbs: number | null;

  @ApiPropertyOptional({ nullable: true, description: 'Fat (grams)' })
  fat: number | null;

  @ApiPropertyOptional({
    nullable: true,
    enum: ['KG', 'LITER', 'GRAM'],
    description: 'Serving unit',
  })
  unit_type: string | null;

  @ApiProperty({ description: 'Pieces / servings' })
  quantity: number;
}

/** `GET/PATCH/POST /diet` meal body (snake_case) */
export class DietMealResponseSwagger {
  @ApiProperty()
  id: string;

  @ApiPropertyOptional({
    nullable: true,
    description:
      'GymUser id when gym-scoped; null for user-only personal meals',
  })
  member_id: string | null;

  @ApiProperty()
  name: string;

  @ApiProperty({ example: '07:30 PM', description: 'Display time string' })
  time: string;

  @ApiProperty({ enum: DietMealType, enumName: 'DietMealType' })
  meal_type: DietMealType;

  @ApiProperty()
  repeat_enabled: boolean;

  @ApiProperty({
    type: [Number],
    description: '0 = Monday … 6 = Sunday',
    example: [0, 1, 2, 3, 4],
  })
  repeat_days: number[];

  @ApiProperty({ type: [DietMealFoodItemResponseSwagger] })
  food_items: DietMealFoodItemResponseSwagger[];

  @ApiProperty({
    enum: ['trainer', 'member'],
    description:
      'Creator bucket: `trainer` = OWNER/TRAINER/STAFF; `member` = MEMBER',
  })
  created_by: 'trainer' | 'member';

  @ApiProperty({
    enum: GymRole,
    enumName: 'GymRole',
    description: 'Gym role of the user who created this meal',
  })
  created_by_role: GymRole;

  @ApiProperty({ format: 'date-time' })
  created_at: string;

  @ApiProperty({ format: 'date-time' })
  updated_at: string;
}

/** `GET/POST/PATCH /diet/food` catalog item (snake_case) */
export class DietFoodResponseSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty()
  name: string;

  @ApiPropertyOptional({ nullable: true, description: 'Default weight (kg)' })
  weight_kg: number | null;

  @ApiProperty()
  calories: number;

  @ApiPropertyOptional({ nullable: true, description: 'Protein (grams)' })
  protein: number | null;

  @ApiPropertyOptional({ nullable: true, description: 'Carbohydrates (grams)' })
  carbs: number | null;

  @ApiPropertyOptional({ nullable: true, description: 'Fat (grams)' })
  fat: number | null;

  @ApiPropertyOptional({
    nullable: true,
    enum: ['KG', 'LITER', 'GRAM'],
    description: 'Serving unit',
  })
  unit_type: string | null;

  @ApiProperty()
  quantity: number;

  @ApiPropertyOptional({ nullable: true, description: 'Thumbnail URL' })
  image_url: string | null;

  @ApiProperty({ format: 'date-time' })
  created_at: string;

  @ApiProperty({ format: 'date-time' })
  updated_at: string;
}

/** `DELETE /diet/:mealId` and `DELETE /diet/food/:foodId` */
export class DietMutationSuccessSwagger {
  @ApiProperty({ example: true })
  success: boolean;
}

class DietHistoryDailySummarySwagger {
  @ApiProperty({ example: 2200 })
  target_kcal: number;

  @ApiProperty({ example: 1010 })
  consumed_kcal: number;

  @ApiProperty({ example: 1200, description: 'max(0, target − consumed)' })
  remaining_kcal: number;
}

class DietHistoryMacroSwagger {
  @ApiProperty({ example: 82 })
  protein_g: number;

  @ApiProperty({ example: 145 })
  carbs_g: number;

  @ApiProperty({ example: 34 })
  fat_g: number;
}

class DietHistoryItemSwagger {
  @ApiProperty()
  id: string;

  @ApiPropertyOptional({ nullable: true })
  diet_food_id: string | null;

  @ApiProperty()
  name: string;

  @ApiProperty({ example: '150g' })
  amount_display: string;

  @ApiProperty()
  calories: number;

  @ApiPropertyOptional({ nullable: true })
  image_url: string | null;
}

class DietHistoryMealLogSwagger {
  @ApiProperty({ enum: DietMealType, enumName: 'DietMealType' })
  meal_type: DietMealType;

  @ApiProperty({ example: 'Breakfast' })
  meal_label: string;

  @ApiProperty({ example: '8:30 AM' })
  time: string;

  @ApiProperty()
  total_calories: number;

  @ApiProperty({ type: [DietHistoryItemSwagger] })
  items: DietHistoryItemSwagger[];
}

/** `GET /diet/history` */
export class DietHistoryResponseSwagger {
  @ApiProperty({ example: '2026-02-17' })
  date: string;

  @ApiProperty({ description: 'Authenticated user id (same as JWT `sub`)' })
  user_id: string;

  @ApiProperty({ type: DietHistoryDailySummarySwagger })
  daily_summary: DietHistoryDailySummarySwagger;

  @ApiProperty({ type: DietHistoryMacroSwagger })
  macros: DietHistoryMacroSwagger;

  @ApiProperty({ type: [DietHistoryMealLogSwagger] })
  meal_logs: DietHistoryMealLogSwagger[];

  @ApiProperty({
    type: 'array',
    description:
      'Weekly scheduled meals for this calendar day (`repeat_days` match). Separate from consumed `meal_logs`.',
    items: { type: 'object' },
  })
  recurring_meals: Record<string, unknown>[];

  @ApiProperty({
    description:
      'Totals for `recurring_meals` only — does not affect `macros` or `daily_summary`.',
  })
  recurring_summary: {
    repeat_day_index: number;
    meal_count: number;
    total_calories: number;
    protein_g: number;
    carbs_g: number;
    fat_g: number;
  };
}

/** `POST /diet/food-consume` */
export class FoodConsumeDietResponseSwagger {
  @ApiProperty({ example: true })
  success: boolean;

  @ApiProperty({ example: 2 })
  count: number;

  @ApiProperty({ type: 'array', items: { type: 'string' } })
  ids: string[];
}
