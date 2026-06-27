import { IsOptional } from 'class-validator';
import { CreatePlanCompatDto } from './create-plan-compat.dto';

export class ValidatePlanDto extends CreatePlanCompatDto {
  @IsOptional()
  override gymId: string;
}
