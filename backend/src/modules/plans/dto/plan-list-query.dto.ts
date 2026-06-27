import { Type } from 'class-transformer';
import { IsEnum, IsInt, IsOptional, Max, Min } from 'class-validator';
import { PlanType } from '@prisma/client';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class PlanListQueryDto extends GymIdQueryDto {
  @IsOptional()
  @IsEnum(PlanType)
  type?: PlanType;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  offset?: number;
}
