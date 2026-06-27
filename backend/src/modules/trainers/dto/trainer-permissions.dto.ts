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

  @IsOptional()
  @IsBoolean()
  leaveCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  leaveRead?: boolean;

  @IsOptional()
  @IsBoolean()
  leaveUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  leaveDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  leaveApprove?: boolean;

  @IsOptional()
  @IsBoolean()
  leaveReject?: boolean;

  @IsOptional()
  @IsBoolean()
  productCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  productRead?: boolean;

  @IsOptional()
  @IsBoolean()
  productUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  productDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  dashboardView?: boolean;

  @IsOptional()
  @IsBoolean()
  dashboardNotifications?: boolean;

  @IsOptional()
  @IsBoolean()
  dashboardPaymentsWidget?: boolean;

  @IsOptional()
  @IsBoolean()
  dashboardAnalytics?: boolean;

  @IsOptional()
  @IsBoolean()
  clientRead?: boolean;

  @IsOptional()
  @IsBoolean()
  clientCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  clientUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  clientDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  clientDetailsRead?: boolean;

  @IsOptional()
  @IsBoolean()
  clientDetailsUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  clientDetailsDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  subscriptionRead?: boolean;

  @IsOptional()
  @IsBoolean()
  subscriptionCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  subscriptionRenew?: boolean;

  @IsOptional()
  @IsBoolean()
  subscriptionUpgrade?: boolean;

  @IsOptional()
  @IsBoolean()
  subscriptionFreeze?: boolean;

  @IsOptional()
  @IsBoolean()
  paymentRead?: boolean;

  @IsOptional()
  @IsBoolean()
  paymentCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  paymentUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  paymentDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  invoiceGenerate?: boolean;

  @IsOptional()
  @IsBoolean()
  invoiceShare?: boolean;

  @IsOptional()
  @IsBoolean()
  attendanceRead?: boolean;

  @IsOptional()
  @IsBoolean()
  biometricCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  biometricDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  biometricBlock?: boolean;

  @IsOptional()
  @IsBoolean()
  workoutAssign?: boolean;

  @IsOptional()
  @IsBoolean()
  dietAssign?: boolean;

  @IsOptional()
  @IsBoolean()
  progressTrack?: boolean;

  @IsOptional()
  @IsBoolean()
  leadRead?: boolean;

  @IsOptional()
  @IsBoolean()
  leadCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  leadUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  leadDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  leadConvert?: boolean;

  @IsOptional()
  @IsBoolean()
  planRead?: boolean;

  @IsOptional()
  @IsBoolean()
  planCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  planUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  planDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  planClientsView?: boolean;

  @IsOptional()
  @IsBoolean()
  trainerRead?: boolean;

  @IsOptional()
  @IsBoolean()
  trainerCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  trainerUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  trainerDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  trainerCredentialsManage?: boolean;

  @IsOptional()
  @IsBoolean()
  trainerPermissionsAssign?: boolean;

  @IsOptional()
  @IsBoolean()
  salaryRead?: boolean;

  @IsOptional()
  @IsBoolean()
  salaryCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  salaryUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  salaryDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  expenseRead?: boolean;

  @IsOptional()
  @IsBoolean()
  expenseCreate?: boolean;

  @IsOptional()
  @IsBoolean()
  expenseUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  expenseDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  gymRead?: boolean;

  @IsOptional()
  @IsBoolean()
  gymUpdate?: boolean;

  @IsOptional()
  @IsBoolean()
  gymDelete?: boolean;

  @IsOptional()
  @IsBoolean()
  broadcastWhatsapp?: boolean;

  @IsOptional()
  @IsBoolean()
  broadcastMessage?: boolean;

  @IsOptional()
  @IsBoolean()
  qrView?: boolean;
}
