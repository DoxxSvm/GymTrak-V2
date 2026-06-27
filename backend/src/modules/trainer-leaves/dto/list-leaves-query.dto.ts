import { TrainerLeaveStatus } from '@prisma/client';
import { Type } from 'class-transformer';
import {
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
} from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class ListLeavesQueryDto extends GymIdQueryDto {
  @IsOptional()
  @IsEnum(TrainerLeaveStatus)
  status?: TrainerLeaveStatus;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}$/)
  month?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  dateFrom?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  dateTo?: string;

  /** Admin queue: filter by trainer `GymUser.id` */
  @IsOptional()
  @IsString()
  trainerId?: string;

  @IsOptional()
  @IsString()
  @Max(200)
  q?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number = 20;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  offset?: number = 0;
}
