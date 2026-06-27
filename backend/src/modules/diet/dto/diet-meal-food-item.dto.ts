import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietFoodUnitType } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  IsEnum,
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
} from 'class-validator';

export class DietMealFoodItemDto {
  @ApiPropertyOptional({
    description:
      'Catalog food id from `GET /diet/food`; when set, defaults name/weight/calories from catalog unless overridden',
  })
  @IsOptional()
  @IsString()
  diet_food_id?: string;

  @ApiPropertyOptional({ example: 'Oatmeal' })
  @IsOptional()
  @IsString()
  @MaxLength(200)
  name?: string;

  @ApiPropertyOptional({ example: 150, description: 'Weight in kg (display)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  weight_kg?: number;

  @ApiProperty({ example: 100 })
  @Type(() => Number)
  @IsInt()
  @Min(0)
  calories: number;

  @ApiPropertyOptional({ example: 12.5, description: 'Protein (grams)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  protein?: number;

  @ApiPropertyOptional({ example: 45, description: 'Carbohydrates (grams)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  carbs?: number;

  @ApiPropertyOptional({ example: 3.2, description: 'Fat (grams)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  fat?: number;

  @ApiPropertyOptional({
    enum: DietFoodUnitType,
    example: DietFoodUnitType.GRAM,
    description: 'Serving unit: KG, LITER, or GRAM',
  })
  @IsOptional()
  @IsEnum(DietFoodUnitType)
  unit_type?: DietFoodUnitType;

  @ApiProperty({ example: 2, description: 'Pieces / servings (UI “Pics”)' })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  quantity: number;
}
