import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
} from 'class-validator';

export class DietMealFoodItemDto {
  @ApiPropertyOptional({
    description: 'Catalog food id from `GET /diet/food`; when set, defaults name/weight/calories from catalog unless overridden',
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

  @ApiProperty({ example: 2, description: 'Pieces / servings (UI “Pics”)' })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  quantity: number;
}
