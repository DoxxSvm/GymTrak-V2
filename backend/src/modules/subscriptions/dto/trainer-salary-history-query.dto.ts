import { IsNotEmpty, IsOptional, IsString } from 'class-validator';

export class TrainerSalaryHistoryQueryDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsString()
  @IsNotEmpty()
  trainer_id: string;

  /** YYYY-MM-DD anchor date; month summary is calculated for this month. */
  @IsOptional()
  @IsString()
  date?: string;
}
