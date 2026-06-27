import {
  ArrayMinSize,
  IsArray,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

class CreateFoodItemDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  food_name: string;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  quantity: number;

  @Type(() => Number)
  @IsInt()
  @Min(0)
  calories: number;
}

export class CreateMealDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  meal_name: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(16)
  meal_time: string;

  @IsString()
  @IsIn(['breakfast', 'lunch', 'dinner', 'snack'])
  meal_type: 'breakfast' | 'lunch' | 'dinner' | 'snack';

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  repeat_days?: string[];

  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => CreateFoodItemDto)
  food_items: CreateFoodItemDto[];
}
