import { IsBoolean, IsOptional } from 'class-validator';

export class UpdateTrainerPermissionsDto {
  @IsOptional()
  @IsBoolean()
  add_members?: boolean;

  @IsOptional()
  @IsBoolean()
  add_clients?: boolean;

  @IsOptional()
  @IsBoolean()
  view_dashboard?: boolean;

  @IsOptional()
  @IsBoolean()
  show_dashboard?: boolean;

  @IsOptional()
  @IsBoolean()
  view_payments?: boolean;

  @IsOptional()
  @IsBoolean()
  show_payments?: boolean;

  @IsOptional()
  @IsBoolean()
  show_payment_in_details?: boolean;

  @IsOptional()
  @IsBoolean()
  view_member_details?: boolean;

  @IsOptional()
  @IsBoolean()
  add_trainer?: boolean;
}
