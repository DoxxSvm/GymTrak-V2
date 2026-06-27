import {
  IsArray,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

class BatchDetailsDto {
  @IsArray()
  @IsString({ each: true })
  working_days: string[];

  @IsString()
  @IsNotEmpty()
  start_time: string;

  @IsString()
  @IsNotEmpty()
  end_time: string;

  @IsString()
  @IsIn(['male', 'female', 'unisex'])
  gender: 'male' | 'female' | 'unisex';
}

export class CreatePlanCompatDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsString()
  @IsIn(['gym', 'pt', 'batch', 'trial'])
  plan_type: 'gym' | 'pt' | 'batch' | 'trial';

  @IsString()
  @IsNotEmpty()
  plan_name: string;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  duration_days: number;

  @Type(() => Number)
  @IsInt()
  @Min(0)
  price: number;

  @IsOptional()
  @IsString()
  trainer_id?: string;

  @IsOptional()
  @ValidateNested()
  @Type(() => BatchDetailsDto)
  batch_details?: BatchDetailsDto;
}
