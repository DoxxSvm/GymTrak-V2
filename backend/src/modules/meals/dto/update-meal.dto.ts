import {
  IsArray,
  IsIn,
  IsOptional,
  IsString,
  MaxLength,
} from 'class-validator';

export class UpdateMealDto {
  @IsOptional()
  @IsString()
  @MaxLength(120)
  meal_name?: string;

  @IsOptional()
  @IsString()
  @MaxLength(16)
  meal_time?: string;

  @IsOptional()
  @IsString()
  @IsIn(['breakfast', 'lunch', 'dinner', 'snack'])
  meal_type?: 'breakfast' | 'lunch' | 'dinner' | 'snack';

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  repeat_days?: string[];
}
