import { TrainerLeaveType } from '@prisma/client';
import {
  IsEnum,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  MaxLength,
} from 'class-validator';

export class CreateTrainerLeaveDto {
  @IsString()
  @IsNotEmpty()
  gymId!: string;

  /** Required when gym owner / queue manager creates leave for a trainer (`GymUser.id`). Ignored for trainers (self). */
  @IsOptional()
  @IsString()
  trainerId?: string;

  @IsEnum(TrainerLeaveType)
  leaveType!: TrainerLeaveType;

  @IsString()
  @IsNotEmpty()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  startDate!: string;

  @IsString()
  @IsNotEmpty()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  endDate!: string;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  reason?: string;
}
