import { TrainerLeaveType } from '@prisma/client';
import { IsEnum, IsOptional, IsString, Matches, MaxLength } from 'class-validator';

export class UpdateTrainerLeaveDto {
  @IsOptional()
  @IsEnum(TrainerLeaveType)
  leaveType?: TrainerLeaveType;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  startDate?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  endDate?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  reason?: string;
}
