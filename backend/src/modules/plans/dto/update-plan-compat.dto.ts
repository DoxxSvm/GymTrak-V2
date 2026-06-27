import { PlanType } from '@prisma/client';
import { Transform, Type } from 'class-transformer';
import {
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Min,
  ValidateNested,
} from 'class-validator';
import {
  BatchDetailsCompatDto,
  normalizeCompatPlanType,
} from './create-plan-compat.dto';

export class UpdatePlanCompatDto {
  @IsOptional()
  @Transform(({ obj }) =>
    normalizeCompatPlanType(obj.planType ?? obj.plan_type),
  )
  @IsEnum(PlanType)
  planType?: PlanType;

  @IsOptional()
  @Transform(({ obj }) => obj.planName ?? obj.plan_name)
  @IsString()
  @IsNotEmpty()
  planName?: string;

  @IsOptional()
  @Transform(({ obj }) => obj.durationDays ?? obj.duration_days)
  @Type(() => Number)
  @IsInt()
  @Min(1)
  durationDays?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  price?: number;

  @IsOptional()
  @Transform(({ obj }) => obj.trainerId ?? obj.trainer_id)
  @IsString()
  trainerId?: string;

  @IsOptional()
  @ValidateNested()
  @Type(() => BatchDetailsCompatDto)
  batch_details?: BatchDetailsCompatDto;
}
