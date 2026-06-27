import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietMealType } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsDateString,
  IsEnum,
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';

export class ConsumeDietFoodItemDto {
  @ApiPropertyOptional({
    description:
      'Catalog food from `GET /diet/food?gymId=`; sets defaults when set',
  })
  @IsOptional()
  @IsString()
  diet_food_id?: string;

  @ApiPropertyOptional({ example: 'Grilled Breast' })
  @IsOptional()
  @IsString()
  @MaxLength(200)
  name?: string;

  @ApiPropertyOptional({ example: 0.15, description: 'Portion weight in kg' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  weight_kg?: number;

  @ApiProperty({
    example: 180,
    description: 'Kcal for this line (per serving logic on client)',
  })
  @Type(() => Number)
  @IsInt()
  @Min(0)
  calories: number;

  @ApiPropertyOptional({
    example: 1,
    default: 1,
    description: 'Servings / pieces',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  quantity?: number;

  @ApiPropertyOptional({
    example: '1 bowl',
    description: 'Free-text portion when not weight-based',
  })
  @IsOptional()
  @IsString()
  @MaxLength(64)
  portion_label?: string;

  @ApiPropertyOptional({
    example: 12.5,
    description: 'Protein in g (optional; sums into history)',
  })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  protein_g?: number;

  @ApiPropertyOptional({
    description: 'Alias for `protein_g` (same as meal `food_items.protein`).',
  })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  protein?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  carbs_g?: number;

  @ApiPropertyOptional({
    description: 'Alias for `carbs_g` (same as meal `food_items.carbs`).',
  })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  carbs?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  fat_g?: number;

  @ApiPropertyOptional({
    description: 'Alias for `fat_g` (same as meal `food_items.fat`).',
  })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  fat?: number;
}

export class FoodConsumeDietDto {
  @ApiProperty({ enum: DietMealType, example: DietMealType.BREAKFAST })
  @IsEnum(DietMealType)
  meal_type: DietMealType;

  @ApiPropertyOptional({
    example: '2026-04-24',
    description:
      'Local calendar day (YYYY-MM-DD). Defaults to today in UTC when omitted',
  })
  @IsOptional()
  @IsDateString()
  consumed_on?: string;

  @ApiPropertyOptional({
    format: 'date-time',
    description:
      'When this meal was logged; defaults to now. Used for grouping and clock time on Diet History',
  })
  @IsOptional()
  @IsDateString()
  consumed_at?: string;

  @ApiProperty({ type: [ConsumeDietFoodItemDto] })
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(200)
  @ValidateNested({ each: true })
  @Type(() => ConsumeDietFoodItemDto)
  items: ConsumeDietFoodItemDto[];
}
