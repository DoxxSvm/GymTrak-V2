import { Type } from 'class-transformer';
import { IsInt, IsNotEmpty, IsOptional, IsString, Min } from 'class-validator';

export class AddSetDto {
  @IsString()
  @IsNotEmpty()
  workout_exercise_id: string;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  set_number: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  reps?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  weight?: number;
}
