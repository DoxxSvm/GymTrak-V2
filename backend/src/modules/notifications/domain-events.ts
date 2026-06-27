/** Event names for the decoupled notification domain (emit from feature modules). */
export const NOTIFICATION_EVENTS = {
  MEMBER_ADDED: 'gym.member.added',
  PAYMENT_RECEIVED: 'gym.payment.received',
  PLAN_ASSIGNED: 'gym.plan.assigned',
  EXPIRY_ALERT: 'gym.subscription.expiry-alert',
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
