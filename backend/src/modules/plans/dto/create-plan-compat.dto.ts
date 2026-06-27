import { PlanType } from '@prisma/client';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import {
  ArrayMinSize,
  IsArray,
  IsDefined,
  IsEnum,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Min,
  ValidateIf,
  ValidateNested,
  IsInt,
} from 'class-validator';

export class BatchShiftCompatDto {
  @ApiProperty({
    example: '08:00 AM',
    description: '`HH:mm` or 12h with AM/PM',
  })
  @Transform(({ obj }) => obj.start_time ?? obj.startTime)
  @IsString()
  @IsNotEmpty()
  start_time: string;

  @ApiProperty({ example: '09:00 AM' })
  @Transform(({ obj }) => obj.end_time ?? obj.endTime)
  @IsString()
  @IsNotEmpty()
  end_time: string;
}

export class BatchDetailsCompatDto {
  @ApiProperty({
    example: [0, 1, 2, 3, 4],
    isArray: true,
    description:
      'Weekdays the batch runs: Monday-indexed integers `0`–`6` (Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6) or strings like `mon`',
  })
  @IsArray()
  @ArrayMinSize(1)
  working_days: (string | number)[];

  @ApiProperty({
    example: 'unisex',
    description:
      '`male`, `female`, `unisex`/`any`, `mixed` (maps to Prisma `BatchPlanGender`)',
  })
  @IsString()
  @IsNotEmpty()
  gender: string;

  @ApiPropertyOptional({
    type: [BatchShiftCompatDto],
    description:
      'One or more time windows; required for batch unless using legacy `start_time`/`end_time`',
  })
  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => BatchShiftCompatDto)
  shifts?: BatchShiftCompatDto[];

  @ApiPropertyOptional({
    example: '08:00 AM',
    description: 'Legacy single shift when `shifts` is omitted',
  })
  @IsOptional()
  @IsString()
  start_time?: string;

  @ApiPropertyOptional({ example: '09:00 AM' })
  @IsOptional()
  @IsString()
  end_time?: string;
}

export function normalizeCompatPlanType(raw: unknown): PlanType | undefined {
  if (raw == null || typeof raw !== 'string') {
    return undefined;
  }
  const s = raw.trim();
  if (!s) {
    return undefined;
  }
  const legacy: Record<string, PlanType> = {
    gym: PlanType.GYM_MEMBERSHIP,
    pt: PlanType.PT_PLAN,
    batch: PlanType.BATCH_PLAN,
    trial: PlanType.FREE_TRIAL,
  };
  const lower = s.toLowerCase();
  if (lower in legacy) {
    return legacy[lower];
  }
  if (Object.values(PlanType).includes(s as PlanType)) {
    return s as PlanType;
  }
  return undefined;
}

export class CreatePlanCompatDto {
  @ApiProperty({ example: 'gym_cuid_example' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @ApiProperty({
    enum: PlanType,
    enumName: 'PlanType',
    example: PlanType.GYM_MEMBERSHIP,
    description:
      'Also accepts legacy `plan_type`: `gym`, `pt`, `batch`, `trial`. Alias `plan_type` is transformed to this field.',
  })
  @Transform(({ obj }) =>
    normalizeCompatPlanType(obj.planType ?? obj.plan_type),
  )
  @IsEnum(PlanType)
  planType: PlanType;

  @ApiProperty({
    example: 'Monthly Unlimited',
    description: 'Alias: `plan_name`',
  })
  @Transform(({ obj }) => obj.planName ?? obj.plan_name)
  @IsString()
  @IsNotEmpty()
  planName: string;

  @ApiProperty({
    example: 30,
    description: 'Billing period length in days. Alias: `duration_days`',
  })
  @Transform(({ obj }) => obj.durationDays ?? obj.duration_days)
  @Type(() => Number)
  @IsInt()
  @Min(1)
  durationDays: number;

  @ApiProperty({
    example: 49.99,
    description: 'Amount in USD; stored as cents. Use `0` for free trial.',
  })
  @Type(() => Number)
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  price: number;

  @ApiPropertyOptional({
    example: 'gym_user_trainer_cuid',
    description:
      'Required for `PT_PLAN` and `BATCH_PLAN` (`GymUser.id`, trainer at this gym). Alias: `trainer_id`',
  })
  @ValidateIf(
    (o) =>
      o.planType === PlanType.PT_PLAN || o.planType === PlanType.BATCH_PLAN,
  )
  @Transform(({ obj }) => obj.trainerId ?? obj.trainer_id)
  @IsString()
  @IsNotEmpty()
  trainerId?: string;

  @ApiPropertyOptional({
    type: BatchDetailsCompatDto,
    description: 'Required when `planType` is `BATCH_PLAN`',
  })
  @ValidateIf((o) => o.planType === PlanType.BATCH_PLAN)
  @IsDefined()
  @ValidateNested()
  @Type(() => BatchDetailsCompatDto)
  batch_details?: BatchDetailsCompatDto;
}
