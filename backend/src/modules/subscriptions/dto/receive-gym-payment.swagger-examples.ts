/** Request body examples for `POST /payments` — use the **Examples** dropdown in Swagger. */
export const receiveGymPaymentBodyExamples = {
  extend_plan: {
    summary: 'extend_plan (default) — new subscription only',
    description:
      'Omit `member_subscription_id`. `date` optional → new `MemberSubscription.startsAt`.',
    value: {
      type: 'extend_plan',
      member_id: 'gymuser_member_cuid_example',
      gym_plan_id: 'gymplan_cuid_example',
      date: '2026-05-01',
    },
  },
  extend_plan_existing: {
    summary: 'extend_plan — extend existing subscription to date',
    description:
      '`member_subscription_id` + required `date` (new `endsAt`). `extend_days` computed from current expiry. Optional `fees` added to `priceCents`. Optional `amount` + `payment_mode` records payment.',
    value: {
      type: 'extend_plan',
      member_id: 'gymuser_member_cuid_example',
      member_subscription_id: 'membersubscription_cuid_example',
      gym_plan_id: 'gymplan_cuid_example',
      amount: 6000,
      payment_mode: 'cash',
      fees: 500,
      date: '2026-06-25',
    },
  },
  receive_payment: {
    summary: 'receive_payment — renew window on existing subscription + payment',
    description:
      '`startsAt` from optional `date` (omit = today UTC). `endsAt` = `startsAt` + `gym_plan_id` plan `durationDays`. `status` from window; `paidCents` += `amount`.',
    value: {
      type: 'receive_payment',
      member_id: 'gymuser_member_cuid_example',
      member_subscription_id: 'membersubscription_cuid_example',
      gym_plan_id: 'gymplan_cuid_example',
      amount: 2000,
      payment_mode: 'card',
      date: '2026-05-01',
    },
  },
};
