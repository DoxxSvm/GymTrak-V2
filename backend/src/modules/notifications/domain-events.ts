/** Event names for the decoupled notification domain (emit from feature modules). */
export const NOTIFICATION_EVENTS = {
  MEMBER_ADDED: 'gym.member.added',
  PAYMENT_RECEIVED: 'gym.payment.received',
  PLAN_ASSIGNED: 'gym.plan.assigned',
  EXTEND_PLAN: 'gym.plan.extended',
  PLAN_RENEWED: 'gym.plan.renewed',
  EXPIRY_ALERT: 'gym.subscription.expiry-alert',
  ENQUIRY_CREATED: 'gym.enquiry.created',
  ENQUIRY_CONVERTED: 'gym.enquiry.converted',
  TRAINER_SALARY_PAID: 'gym.trainer.salary-paid',
  EXPENSE_CREATED: 'gym.expense.created',
} as const;

export interface MemberAddedPayload {
  gymId: string;
  gymUserId: string;
  memberName: string;
  actorUserId: string;
}

export interface PlanAssignedPayload {
  gymId: string;
  gymUserId: string;
  memberSubscriptionId: string;
  actorUserId: string;
}

export interface ExtendPlanPayload {
  gymId: string;
  gymUserId: string;
  memberName: string;
  memberSubscriptionId: string;
  actorUserId: string;
}

export interface PaymentReceivedPayload {
  gymId: string;
  gymUserId: string;
  paymentId: string;
  amountCents: number;
  currency: string;
  memberSubscriptionId: string | null;
  actorUserId: string;
}

export interface ExpiryAlertPayload {
  gymId: string;
  memberSubscriptionId: string;
  gymUserId: string;
  memberName: string | null;
  /** Pre-expiry: 7 or 3 days before `endsAt`. Omitted for post-expiry. */
  phase: 'pre' | 'post';
  daysBeforeExpiry?: number;
}

export interface EnquiryCreatedPayload {
  gymId: string;
  enquiryId: string;
  leadName: string;
  actorUserId: string;
}

export interface EnquiryConvertedPayload {
  gymId: string;
  enquiryId: string;
  memberGymUserId: string;
  memberName: string;
  actorUserId: string;
}

export interface TrainerSalaryPaidPayload {
  gymId: string;
  trainerGymUserId: string;
  /** TRAINER or STAFF — drives `staff-salary` vs `trainer-salary` deep link screen. */
  payeeRole: 'TRAINER' | 'STAFF';
  trainerName: string;
  salaryPaymentId: string;
  amountCents: number;
  currency: string;
  actorUserId: string;
}

export interface ExpenseCreatedPayload {
  gymId: string;
  expenseId: string;
  amountCents: number;
  currency: string;
  category: string;
  description: string | null;
  trainerGymUserId: string | null;
  trainerName: string | null;
  actorUserId: string;
}
