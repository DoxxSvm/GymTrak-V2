import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class AddWorkoutDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsString()
  @IsNotEmpty()
  title: string;

  @IsOptional()
  @IsString()
  @MaxLength(4000)
  description?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  trainer_name?: string;
}
