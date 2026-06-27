import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietMealType } from '@prisma/client';

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

  @ApiProperty({ description: 'Pieces / servings' })
  quantity: number;
}

/** `GET/PATCH/POST /diet` meal body (snake_case) */
export class DietMealResponseSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty({ description: 'Member GymUser id' })
  member_id: string;

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
