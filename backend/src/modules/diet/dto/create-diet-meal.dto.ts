import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { DietMealType } from '@prisma/client';
import { Transform, Type } from 'class-transformer';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsBoolean,
  IsEnum,
  IsIn,
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
  @ApiPropertyOptional({
    description:
      'Target member’s GymUser id when staff creates for someone else. Omit for **your own** meal (JWT `sub`): optional `gymId` query / JWT gym / sole member gym → gym-scoped; otherwise user-scoped personal meal (`gymId` null).',
  })
  @IsOptional()
  @Transform(({ value }) => (value === '' || value == null ? undefined : value))
  @IsString()
  member_id?: string;

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

  @ApiPropertyOptional({
    enum: ['trainer', 'member'],
    example: 'trainer',
    description:
      'Who created this meal: `trainer` (OWNER/TRAINER/STAFF) or `member`. Omit to use the JWT actor’s resolved gym role.',
  })
  @IsOptional()
  @IsIn(['trainer', 'member'])
  created_by?: 'trainer' | 'member';

  @ApiPropertyOptional({
    description: 'When true, meal repeats on `repeat_days`',
  })
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
