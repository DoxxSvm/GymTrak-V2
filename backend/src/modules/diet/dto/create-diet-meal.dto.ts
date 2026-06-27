import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietMealType } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { DietMealFoodItemDto } from './diet-meal-food-item.dto';

export class CreateDietMealDto {
  @ApiProperty({ description: 'Member GymUser id' })
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @ApiProperty({ example: 'Post-Workout meal' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(160)
  name: string;

  @ApiProperty({ example: '07:30 PM' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(32)
  time: string;

  @ApiProperty({ enum: DietMealType, example: DietMealType.BREAKFAST })
  @IsEnum(DietMealType)
  meal_type: DietMealType;

  @ApiPropertyOptional({ description: 'When true, meal repeats on `repeat_days`' })
  @IsOptional()
  @IsBoolean()
  repeat_enabled?: boolean;

  @ApiPropertyOptional({
    description: '0 = Monday … 6 = Sunday',
    type: [Number],
    example: [0, 1, 2, 3, 4],
  })
  @IsOptional()
  @IsArray()
  @ArrayMinSize(0)
  @ArrayMaxSize(7)
  @IsInt({ each: true })
  @Min(0, { each: true })
  @Max(6, { each: true })
  repeat_days?: number[];

  @ApiPropertyOptional({ type: [DietMealFoodItemDto] })
  @IsOptional()
  @ValidateNested({ each: true })
  @Type(() => DietMealFoodItemDto)
  food_items?: DietMealFoodItemDto[];
}
