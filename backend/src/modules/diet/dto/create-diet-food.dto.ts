import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietFoodUnitType } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
} from 'class-validator';

export class CreateDietFoodDto {
  @ApiProperty({ example: 'Oatmeal' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name: string;

  @ApiPropertyOptional({ example: 150 })
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

  @ApiPropertyOptional({ example: 2, default: 1 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  quantity?: number;

  @ApiPropertyOptional({ description: 'Thumbnail URL' })
  @IsOptional()
  @IsString()
  @MaxLength(2048)
  image_url?: string;
}
