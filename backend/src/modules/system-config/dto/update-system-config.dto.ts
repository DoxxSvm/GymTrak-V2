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
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';

/** Validated subset stored in `GymSystemConfig.defaultPlanConfig` JSON. */
export class DefaultPlanConfigDto {
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(3650)
  membershipDurationDays?: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(3650)
  ptDurationDays?: number;
}

export class UpdateSystemConfigDto {
  @IsOptional()
  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim().toUpperCase() : value,
  )
  @IsString()
  @Length(3, 3)
  @Matches(/^[A-Z]{3}$/, {
    message: 'currency must be a 3-letter ISO 4217 code (e.g. INR, USD)',
  })
  currency?: string;

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
  @Length(2, 2)
  @Matches(/^[0-9]{2}$/, { message: 'gstStateCode must be 2 digits' })
  gstStateCode?: string | null;

  /** Send `null` to clear plan defaults. */
  @IsOptional()
  @ValidateIf((_, v) => v !== null && v !== undefined)
  @ValidateNested()
  @Type(() => DefaultPlanConfigDto)
  defaultPlanConfig?: DefaultPlanConfigDto | null;
}
