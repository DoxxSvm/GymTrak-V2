import { IsBoolean, IsOptional } from 'class-validator';

export class TrainerPermissionsDto {
  @IsOptional()
  @IsBoolean()
  dashboard?: boolean;

  @IsOptional()
  @IsBoolean()
  payments?: boolean;

  @IsOptional()
  @IsBoolean()
  members?: boolean;

  /** Manage other trainers / staff (sensitive operations). */
  @IsOptional()
  @IsBoolean()
  admin?: boolean;

  @IsOptional()
  @IsBoolean()
  add_clients?: boolean;

  @IsOptional()
  @IsBoolean()
  show_dashboard?: boolean;

  @IsOptional()
  @IsBoolean()
  show_payments?: boolean;

  @IsOptional()
  @IsBoolean()
  show_payment_in_details?: boolean;

  @IsOptional()
  @IsBoolean()
  add_trainer?: boolean;
}
