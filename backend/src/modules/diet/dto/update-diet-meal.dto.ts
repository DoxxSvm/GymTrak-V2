import { ApiPropertyOptional } from '@nestjs/swagger';
import { DietMealType } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { DietMealFoodItemDto } from './diet-meal-food-item.dto';

export class UpdateDietMealDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(160)
  name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(32)
  time?: string;

  @ApiPropertyOptional({ enum: DietMealType })
  @IsOptional()
  @IsEnum(DietMealType)
  meal_type?: DietMealType;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  repeat_enabled?: boolean;

  @ApiPropertyOptional({ type: [Number] })
  @IsOptional()
  @IsArray()
  @ArrayMaxSize(7)
  @IsInt({ each: true })
  @Min(0, { each: true })
  @Max(6, { each: true })
  repeat_days?: number[];

  @ApiPropertyOptional({
    description: 'When provided, replaces all food lines on the meal',
    type: [DietMealFoodItemDto],
  })
  @IsOptional()
  @ValidateNested({ each: true })
  @Type(() => DietMealFoodItemDto)
  food_items?: DietMealFoodItemDto[];
}
