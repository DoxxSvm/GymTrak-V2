import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';
import { BatchPlanGender, PlanType } from '@prisma/client';
import { GymPlanShiftInputDto } from './gym-plan-shift.dto';

/** Body for POST /plans — `gymId` is passed as a query param */
export class CreateGymPlanDto {
  @IsEnum(PlanType)
  type: PlanType;

  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name: string;

  @IsInt()
  @Min(1)
  durationDays: number;

  @IsInt()
  @Min(0)
  priceCents: number;

  @IsOptional()
  @IsString()
  @MaxLength(8)
  currency?: string;

  /// PT_PLAN: required
  @ValidateIf((o: CreateGymPlanDto) => o.type === PlanType.PT_PLAN)
  @IsString()
  @IsNotEmpty()
  trainerGymUserId?: string;

  /// BATCH_PLAN: required (at least one shift)
  @ValidateIf((o: CreateGymPlanDto) => o.type === PlanType.BATCH_PLAN)
  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => GymPlanShiftInputDto)
  shifts?: GymPlanShiftInputDto[];

  /// BATCH_PLAN: which weekdays the batch runs (0 = Sun .. 6 = Sat)
  @ValidateIf((o: CreateGymPlanDto) => o.type === PlanType.BATCH_PLAN)
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(7)
  @IsInt({ each: true })
  @Min(0, { each: true })
  @Max(6, { each: true })
  batchDaysOfWeek?: number[];

  @ValidateIf((o: CreateGymPlanDto) => o.type === PlanType.BATCH_PLAN)
  @IsEnum(BatchPlanGender)
  batchGender?: BatchPlanGender;

  @IsOptional()
  @IsObject()
  metadata?: Record<string, unknown>;

  @IsOptional()
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  batchStartTime?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  batchEndTime?: string;
}
