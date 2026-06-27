import { Transform, Type } from 'class-transformer';
import {
  IsBoolean,
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  Length,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';

/** Flutter owner settings default plan (stored inside GymSystemConfig.defaultPlanConfig JSON). */
export class GymDefaultPlanDto {
  @IsOptional()
  @IsString()
  @MaxLength(120)
  name?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(120)
  durationMonths?: number;

  @IsOptional()
  @IsInt()
  @Min(0)
  priceCents?: number;
}

/** Body for PATCH/POST /gyms/:gymId/config — matches legacy Express API shape. */
export class UpdateGymConfigDto {
  @IsOptional()
  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim().toUpperCase() : value,
  )
  @IsString()
  @Length(3, 3)
  @Matches(/^[A-Z]{3}$/)
  currencyCode?: string;

  @IsOptional()
  @IsBoolean()
  gstEnabled?: boolean;

  @IsOptional()
  @ValidateIf((_, v) => v !== null && v !== undefined)
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  @Max(100)
  gstRatePercent?: number | null;

  @IsOptional()
  @IsBoolean()
  gstInclusive?: boolean;

  @IsOptional()
  @ValidateIf((_, v) => v !== null && v !== undefined)
  @IsString()
  @MaxLength(8)
  gstStateCode?: string | null;

  @IsOptional()
  @ValidateNested()
  @Type(() => GymDefaultPlanDto)
  defaultPlan?: GymDefaultPlanDto;
}
