import { GymRole } from '@prisma/client';
import { Transform, Type } from 'class-transformer';
import {
  IsEnum,
  IsBoolean,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  Min,
  IsIn,
} from 'class-validator';

export class TrainerListQueryDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsOptional()
  @IsString()
  @IsIn(['TRAINER', 'STAFF'])
  role?: string;

  @IsOptional()
  @IsString()
  q?: string;

  @IsOptional()
  @Transform(({ value }) => value === true || value === 'true' || value === '1')
  @IsBoolean()
  includeInactive?: boolean;

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
