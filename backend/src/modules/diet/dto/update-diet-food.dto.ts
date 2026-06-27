import { ApiPropertyOptional } from '@nestjs/swagger';
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

export class UpdateDietFoodDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(200)
  name?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  weight_kg?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  calories?: number;

  @ApiPropertyOptional({ description: 'Protein (grams)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  protein?: number;

  @ApiPropertyOptional({ description: 'Carbohydrates (grams)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  carbs?: number;

  @ApiPropertyOptional({ description: 'Fat (grams)' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  fat?: number;

  @ApiPropertyOptional({
    enum: DietFoodUnitType,
    description: 'Serving unit: KG, LITER, or GRAM',
  })
  @IsOptional()
  @IsEnum(DietFoodUnitType)
  unit_type?: DietFoodUnitType;

  @ApiPropertyOptional()
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  quantity?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  image_url?: string;
}
