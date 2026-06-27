import {
  IsArray,
  IsIn,
  IsInt,
  IsOptional,
  IsString,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

class BatchDetailsDto {
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  working_days?: string[];

  @IsOptional()
  @IsString()
  start_time?: string;

  @IsOptional()
  @IsString()
  end_time?: string;

  @IsOptional()
  @IsString()
  @IsIn(['male', 'female', 'unisex'])
  gender?: 'male' | 'female' | 'unisex';
}

export class UpdatePlanCompatDto {
  @IsOptional()
  @IsString()
  @IsIn(['gym', 'pt', 'batch', 'trial'])
  plan_type?: 'gym' | 'pt' | 'batch' | 'trial';

  @IsOptional()
  @IsString()
  plan_name?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  duration_days?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  price?: number;

  @IsOptional()
  @IsString()
  trainer_id?: string;

  @IsOptional()
  @ValidateNested()
  @Type(() => BatchDetailsDto)
  batch_details?: BatchDetailsDto;
}
