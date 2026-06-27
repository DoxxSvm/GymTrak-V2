import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';
import { BatchPlanGender, PlanType } from '@prisma/client';
import { GymPlanShiftInputDto } from './gym-plan-shift.dto';

export class UpdateGymPlanDto {
  @IsOptional()
  @IsEnum(PlanType)
  type?: PlanType;

  @IsOptional()
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  durationDays?: number;

  @IsOptional()
  @IsInt()
  @Min(0)
  priceCents?: number;

  @IsOptional()
  @IsString()
  @MaxLength(8)
  currency?: string;

  @IsOptional()
  @IsBoolean()
  isActive?: boolean;

  @ValidateIf(
    (o: UpdateGymPlanDto) =>
      o.type === PlanType.PT_PLAN || o.type === PlanType.BATCH_PLAN,
  )
  @IsOptional()
  @IsString()
  trainerGymUserId?: string | null;

  @ValidateIf((o: UpdateGymPlanDto) => o.type === PlanType.BATCH_PLAN)
  @IsOptional()
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => GymPlanShiftInputDto)
  shifts?: GymPlanShiftInputDto[];

  @ValidateIf((o: UpdateGymPlanDto) => o.type === PlanType.BATCH_PLAN)
  @IsOptional()
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(7)
  @IsInt({ each: true })
  @Min(0, { each: true })
  @Max(6, { each: true })
  batchDaysOfWeek?: number[];

  @ValidateIf((o: UpdateGymPlanDto) => o.type === PlanType.BATCH_PLAN)
  @IsOptional()
  @IsEnum(BatchPlanGender)
  batchGender?: BatchPlanGender;

  @IsOptional()
  @IsObject()
  metadata?: Record<string, unknown>;
}
