import { IsOptional, IsString, MaxLength } from 'class-validator';

export class UpdateTrainerWorkoutDto {
  @IsOptional()
  @IsString()
  @MaxLength(160)
  title?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;
}
