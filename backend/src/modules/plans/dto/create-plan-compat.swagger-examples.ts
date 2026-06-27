import { PlanType } from '@prisma/client';

/** Request body examples for `POST /plans/compat` — pick one in Swagger “Examples”. */
export const createPlanCompatBodyExamples = {
  GYM_MEMBERSHIP: {
    summary: 'GYM_MEMBERSHIP — gym member',
    description:
      'Catalog plan only: `gymId`, `planType`, `planName`, `durationDays`, `price` (USD, stored as cents).',
    value: {
      gymId: 'gym_cuid_example',
      planType: PlanType.GYM_MEMBERSHIP,
      planName: 'Monthly Unlimited',
      durationDays: 30,
      price: 49.99,
    },
  },
  PT_PLAN: {
    summary: 'PT_PLAN — personal training',
    description:
      'Adds `trainerId`: `GymUser.id` for an active TRAINER at this gym.',
    value: {
      gymId: 'gym_cuid_example',
      planType: PlanType.PT_PLAN,
      planName: 'PT — 12 sessions',
      durationDays: 45,
      price: 299,
      trainerId: 'gym_user_trainer_cuid',
    },
  },
  BATCH_PLAN: {
    summary: 'BATCH_PLAN — batch class',
    description:
      '`trainerId` plus `batch_details`: Monday-indexed weekdays `0`–`6` (Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6), `gender`, and `shifts` (or legacy `start_time` / `end_time`). Times: `HH:mm` or `h:mm AM/PM`.',
    value: {
      gymId: 'gym_cuid_example',
      planType: PlanType.BATCH_PLAN,
      planName: 'Morning Strength Batch',
      durationDays: 30,
      price: 199,
      trainerId: 'gym_user_trainer_cuid',
      batch_details: {
        working_days: [0, 1, 2, 3, 4],
        gender: 'unisex',
        shifts: [
          { start_time: '08:00 AM', end_time: '09:00 AM' },
          { start_time: '06:00 PM', end_time: '07:00 PM' },
        ],
      },
    },
  },
  FREE_TRIAL: {
    summary: 'FREE_TRIAL — trial',
    description: 'Same base fields as gym membership; `price` is often `0`.',
    value: {
      gymId: 'gym_cuid_example',
      planType: PlanType.FREE_TRIAL,
      planName: '7-day trial',
      durationDays: 7,
      price: 0,
    },
  },
} as const;
