import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

/** Matches `GET /members` list card (`members[]`). */
export class MemberListSummarySwagger {
  @ApiProperty({ description: 'GymUser id' })
  id: string;

  @ApiProperty()
  name: string;

  @ApiProperty()
  phone: string;

  @ApiProperty({
    enum: ['lead', 'inactive', 'expired', 'expiring', 'active'],
    description: 'Member lifecycle at gym',
  })
  status: string;

  @ApiProperty({ description: 'Latest subscription plan name, if any' })
  plan_name: string;

  @ApiPropertyOptional({
    type: String,
    format: 'date',
    nullable: true,
    description: 'Latest membership expiry (`YYYY-MM-DD`, UTC calendar day)',
  })
  expiry_date: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Primary active `MemberSubscription.id`.',
  })
  member_subscription_id: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Total due on primary active plan (`price_cents`).',
  })
  price_cents: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description:
      'Custom subscription selling price on primary active plan (`MemberSubscription.priceCents`).',
  })
  selling_price: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Paid toward primary active plan (`paid_cents`).',
  })
  paid_cents: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Balance on primary active plan.',
  })
  amount_pending: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Extension fees on primary active plan.',
  })
  extension_fees_total: number | null;

  @ApiProperty({ description: 'Avatar URL or empty string' })
  profile_image: string;

  @ApiPropertyOptional({
    description:
      'First token of `fullName` (no separate DB column)',
  })
  first_name: string;

  @ApiPropertyOptional({
    description: 'Remainder of `fullName` after first word',
  })
  last_name: string;

  @ApiPropertyOptional({
    nullable: true,
    description:
      '`User.address` when set; otherwise legacy `GymUser.notes` line `Address: …`',
  })
  address: string | null;

  @ApiPropertyOptional({
    type: String,
    format: 'date',
    nullable: true,
    description: '`GymUser.dateOfBirth` (YYYY-MM-DD)',
  })
  dob: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description:
      '`User.aadhaar_number` when set; otherwise legacy `notes` line `Aadhaar: …`',
  })
  aadhaar_number: string | null;

  @ApiPropertyOptional({ nullable: true, description: 'Gym-scoped ICE name' })
  emergency_name: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'ICE phone (stored on `GymUser`)',
  })
  emergency_contact_phone: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'GymUser.notes / staff-visible note block',
  })
  notes: string | null;

  @ApiPropertyOptional({
    type: Number,
    nullable: true,
    description: 'Age in full years (`User.ageYears` or derived from DOB)',
  })
  age: number | null;
}

export class MemberSubscriptionStatsSwagger {
  @ApiProperty()
  active_subscription: number;

  @ApiProperty()
  pending_payment: number;

  @ApiProperty()
  overdue: number;
}

export class MemberCurrentSubscriptionSwagger {
  @ApiProperty({ description: '`MemberSubscription.id`' })
  member_subscription_id: string;

  @ApiProperty()
  plan_name: string;

  @ApiProperty({
    type: String,
    format: 'date',
    description: 'Period start (`YYYY-MM-DD`, UTC calendar day)',
  })
  start_date: string;

  @ApiProperty({
    type: String,
    format: 'date',
    description: 'Period end (`YYYY-MM-DD`, UTC calendar day)',
  })
  expiry_date: string;

  @ApiProperty()
  remaining_days: number;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Gym catalog base price (`GymPlan.priceCents`) — unchanged on extend.',
  })
  catalog_plan_price: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Deprecated alias for `catalog_plan_price`.',
  })
  plan_price: number | null;

  @ApiProperty({
    description:
      'Total amount due for this subscription (`MemberSubscription.priceCents`, includes extension fees).',
  })
  price_cents: number;

  @ApiProperty({
    description:
      'Custom subscription selling price (`MemberSubscription.priceCents`).',
  })
  selling_price: number;

  @ApiProperty({
    description: 'Sum of extension fees recorded via `extend_plan` (from history).',
  })
  extension_fees_total: number;

  @ApiProperty({
    description: 'Deprecated alias for `extension_fees_total`.',
  })
  extension_fee_total: number;

  @ApiProperty({ description: 'Same as `paid_cents`.' })
  amount_paid: number;

  @ApiProperty({ description: 'Sum of completed payments toward this subscription.' })
  paid_cents: number;

  @ApiProperty()
  amount_pending: number;

  @ApiPropertyOptional({
    nullable: true,
    description: '`GymPlan.id` when subscription uses a gym plan.',
  })
  gym_plan_id: string | null;
}

export class MemberFreezeSubscriptionSwagger extends MemberCurrentSubscriptionSwagger {
  @ApiProperty({
    description: 'Freeze window start (`YYYY-MM-DD`, UTC calendar day)',
  })
  freeze_start_date: string;

  @ApiProperty({
    description: 'Freeze window end (`YYYY-MM-DD`, UTC calendar day)',
  })
  freeze_end_date: string;

  @ApiProperty({ description: 'Freeze length in days' })
  duration_days: number;
}

export class MemberDetailSubscriptionSwagger {
  @ApiProperty({ type: MemberSubscriptionStatsSwagger })
  stats: MemberSubscriptionStatsSwagger;

  @ApiProperty({
    type: [MemberCurrentSubscriptionSwagger],
    description:
      'All in-window non-frozen periods (`startsAt` ≤ now ≤ `endsAt`), most recently ending first.',
  })
  current_subscriptions: MemberCurrentSubscriptionSwagger[];

  @ApiPropertyOptional({
    type: MemberCurrentSubscriptionSwagger,
    nullable: true,
    description:
      'Deprecated — first entry of `current_subscriptions` (latest `endsAt`).',
  })
  current_subscription: MemberCurrentSubscriptionSwagger | null;

  @ApiProperty({
    type: [MemberCurrentSubscriptionSwagger],
    description:
      'Future-dated periods (`startsAt` > now), earliest start first.',
  })
  upcoming_subscriptions: MemberCurrentSubscriptionSwagger[];

  @ApiProperty({
    type: [MemberCurrentSubscriptionSwagger],
    description:
      'Past periods (`now` > `endsAt`), most recently ended first; `remaining_days` is always 0.',
  })
  expired_subscriptions: MemberCurrentSubscriptionSwagger[];

  @ApiProperty({
    type: [MemberFreezeSubscriptionSwagger],
    description:
      'Subscriptions with `status` `FROZEN` and a future `endsAt`; includes freeze window dates.',
  })
  freeze_subscriptions: MemberFreezeSubscriptionSwagger[];
}

/** Stored `User.wellness` JSON (BMI + maintenance snapshot). */
export class MemberWellnessSnapshotSwagger {
  @ApiProperty({ example: 22.5 })
  bmi: number;

  @ApiProperty({
    enum: ['underweight', 'normal', 'overweight', 'obese'],
  })
  bmiCategory: string;

  @ApiPropertyOptional({
    nullable: true,
    type: Number,
    description:
      'Mifflin–St Jeor × activity; null when `age`, `gender`, or `activityLevel` is missing',
  })
  maintenanceCalories: number | null;
}

export class MemberDetailUserSwagger {
  @ApiProperty()
  id: string;

  @ApiPropertyOptional({ nullable: true })
  fullName: string | null;

  @ApiProperty()
  phone: string;

  @ApiPropertyOptional({ nullable: true })
  email: string | null;

  @ApiPropertyOptional({ nullable: true, type: Number })
  heightCm: number | null;

  @ApiPropertyOptional({ nullable: true, type: Number })
  weightKg: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description: '`User.activityLevel` — `LOW` | `MODERATE` | `HIGH`',
  })
  activityLevel: string | null;

  @ApiPropertyOptional({ nullable: true })
  fitnessGoal: string | null;

  @ApiPropertyOptional({
    nullable: true,
    type: MemberWellnessSnapshotSwagger,
    description: 'Stored wellness snapshot (`User.wellness`)',
  })
  wellness: MemberWellnessSnapshotSwagger | null;

  @ApiProperty({ type: String, format: 'date-time' })
  createdAt: Date;

  @ApiPropertyOptional({ nullable: true })
  avatarUrl: string | null;

  @ApiProperty({ description: 'Alias of `avatarUrl` for list/detail clients' })
  profile_image: string;

  @ApiPropertyOptional({
    type: Number,
    nullable: true,
    description: 'Full years — `User.ageYears` or from gym `dateOfBirth`',
  })
  age: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description:
      'Residential address (`User.address`; legacy `notes` `Address:` line when column empty)',
  })
  address: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description:
      'Indian national ID — `User.aadhaar_number` (12 digits), else legacy `notes` line',
  })
  aadhaar_number: string | null;
}

export class MemberDetailContactSwagger {
  @ApiProperty()
  phone: string;

  @ApiPropertyOptional({ nullable: true })
  telUri: string | null;

  @ApiPropertyOptional({ nullable: true })
  whatsappUrl: string | null;
}

export class MemberDetailTabsSwagger {
  @ApiProperty({ description: 'Relative path with gymId query' })
  subscriptions: string;

  @ApiProperty({
    description: 'Attendance month summary (`/attendance/summary`)',
  })
  attendance: string;

  @ApiProperty({
    description: 'Attendance paginated history (`/attendance/history`)',
  })
  attendance_history: string;

  @ApiProperty()
  payments: string;
}

export class MemberDetailAttendanceLinksSwagger {
  @ApiProperty()
  summary: string;

  @ApiProperty()
  history: string;
}

export class MemberDetailAttendanceSwagger {
  @ApiProperty()
  lifetime_check_ins: number;

  @ApiPropertyOptional({ nullable: true })
  last_check_in_at: string | null;

  @ApiPropertyOptional({ nullable: true })
  last_attended_on: string | null;

  @ApiProperty({ type: MemberDetailAttendanceLinksSwagger })
  links: MemberDetailAttendanceLinksSwagger;
}

export class MemberDetailPaymentSummarySwagger {
  @ApiProperty({
    description:
      'Sum of completed payments in the current UTC calendar year (major currency units)',
  })
  paidThisYear: number;

  @ApiProperty({
    description:
      'Total subscription balance owed: sum of max(0, price − paid) over non-canceled member subscriptions',
  })
  outstandingAmount: number;
}

export class MemberDetailPaymentHistoryItemSwagger {
  @ApiProperty({
    description:
      'Linked `GymPlan.name` or legacy `SubscriptionPlan.name`; empty if payment has no subscription link',
  })
  gymPlanName: string;

  @ApiProperty({ description: 'Display label e.g. Cash, UPI, Card' })
  paymentMethod: string;

  @ApiProperty({
    description: 'Transaction date YYYY-MM-DD (UTC) from completedAt or createdAt',
  })
  date: string;

  @ApiPropertyOptional({
    nullable: true,
    description:
      'Staff/trainer who recorded payment when available (reserved; null until audit is wired)',
  })
  receivedBy: string | null;

  @ApiProperty({ description: 'Amount in major currency units' })
  amount: number;
}

/** `GET /members/:memberId` response body. */
export class MemberDetailResponseSwagger {
  @ApiProperty()
  gymUserId: string;

  @ApiProperty()
  gymId: string;

  @ApiProperty({
    enum: ['lead', 'inactive', 'expired', 'expiring', 'active'],
  })
  lifecycleStatus: string;

  @ApiProperty()
  isLead: boolean;

  @ApiProperty()
  isActive: boolean;

  @ApiPropertyOptional({ type: String, format: 'date-time', nullable: true })
  membershipEndsAt: Date | null;

  @ApiProperty({ type: String, format: 'date-time' })
  joinedAt: Date;

  @ApiPropertyOptional({ nullable: true })
  notes: string | null;

  @ApiPropertyOptional({ nullable: true })
  emergencyContactName: string | null;

  @ApiPropertyOptional({ nullable: true })
  emergencyContactPhone: string | null;

  @ApiPropertyOptional({ type: String, format: 'date', nullable: true })
  dateOfBirth: Date | null;

  @ApiPropertyOptional({ nullable: true })
  gender: string | null;

  @ApiPropertyOptional({
    type: Number,
    nullable: true,
    description: 'Full years — `User.ageYears` or from `dateOfBirth`',
  })
  age: number | null;

  @ApiProperty({ description: 'Same as `user.avatarUrl` (list / client alias)' })
  profile_image: string;

  @ApiProperty({ type: MemberListSummarySwagger })
  summary: MemberListSummarySwagger;

  @ApiProperty({ type: MemberDetailSubscriptionSwagger })
  subscription: MemberDetailSubscriptionSwagger;

  @ApiProperty({ type: MemberDetailUserSwagger })
  user: MemberDetailUserSwagger;

  @ApiProperty({ type: MemberDetailContactSwagger })
  contact: MemberDetailContactSwagger;

  @ApiProperty({ type: MemberDetailTabsSwagger })
  tabs: MemberDetailTabsSwagger;

  @ApiProperty({ type: MemberDetailAttendanceSwagger })
  attendance: MemberDetailAttendanceSwagger;

  @ApiProperty({ type: MemberDetailPaymentSummarySwagger })
  paymentSummary: MemberDetailPaymentSummarySwagger;

  @ApiProperty({
    type: [MemberDetailPaymentHistoryItemSwagger],
    description: 'Latest 50 completed payments for this member at the gym',
  })
  paymentHistory: MemberDetailPaymentHistoryItemSwagger[];
}

/** Basic gym context on `GET /members/:memberId/profile?gymId=`. */
export class MemberProfileGymSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty()
  name: string;

  @ApiProperty()
  slug: string;

  @ApiPropertyOptional({ nullable: true })
  address: string | null;

  @ApiProperty()
  timezone: string;

  @ApiPropertyOptional({ nullable: true })
  gstin: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Public logo URL (`Gym.logoUrl`)',
  })
  logo_url: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Decimal string (`Gym.latitude`)',
  })
  latitude: string | null;

  @ApiPropertyOptional({
    nullable: true,
    description: 'Decimal string (`Gym.longitude`)',
  })
  longitude: string | null;
}

/** Nested subscription block on member profile (gym-scoped). */
export class MemberProfileSubscriptionSwagger extends MemberDetailSubscriptionSwagger {
  @ApiProperty({
    type: [MemberCurrentSubscriptionSwagger],
    description:
      'Alias for `expired_subscriptions` — past periods, most recently ended first.',
  })
  past_subscriptions: MemberCurrentSubscriptionSwagger[];
}

/** `GET /members/:memberId/profile` response body (legacy flat card). */
export class MemberProfileCardSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty()
  name: string;

  @ApiProperty()
  phone: string;

  @ApiPropertyOptional({ nullable: true })
  gender: string | null;

  @ApiPropertyOptional({ type: String, format: 'date', nullable: true })
  dob: Date | null;

  @ApiProperty({ type: String, format: 'date-time' })
  join_date: Date;

  @ApiProperty({
    enum: ['lead', 'inactive', 'expired', 'expiring', 'active'],
  })
  status: string;

  @ApiProperty()
  profile_image: string;

  @ApiPropertyOptional({
    type: Number,
    nullable: true,
    description: '`User.ageYears` or derived from `dob` / gym `dateOfBirth`',
  })
  age: number | null;

  @ApiPropertyOptional({ nullable: true, type: Number })
  heightCm: number | null;

  @ApiPropertyOptional({ nullable: true, type: Number })
  weightKg: number | null;

  @ApiPropertyOptional({
    nullable: true,
    description: '`User.activityLevel` — `LOW` | `MODERATE` | `HIGH`',
  })
  activityLevel: string | null;

  @ApiPropertyOptional({ nullable: true })
  fitnessGoal: string | null;

  @ApiPropertyOptional({
    nullable: true,
    type: MemberWellnessSnapshotSwagger,
    description:
      'Live BMI + maintenance snapshot from height, weight, age, gender, and activityLevel',
  })
  wellness: MemberWellnessSnapshotSwagger | null;

  @ApiPropertyOptional({
    nullable: true,
    type: Number,
    description:
      'User-provided maintenance kcal/day — persisted and returned on profile GET/PATCH/PUT',
  })
  maintenanceCalories?: number | null;

  @ApiPropertyOptional({
    nullable: true,
    type: Number,
    description:
      'Legacy mobile typo alias — same value as `maintenanceCalories` / `wellness.maintenanceCalories`',
  })
  maintenanceCaleries?: number | null;

  @ApiProperty({ type: MemberSubscriptionStatsSwagger })
  stats: MemberSubscriptionStatsSwagger;

  @ApiProperty({ type: [MemberCurrentSubscriptionSwagger] })
  current_subscriptions: MemberCurrentSubscriptionSwagger[];

  @ApiPropertyOptional({
    type: MemberCurrentSubscriptionSwagger,
    nullable: true,
  })
  current_subscription: MemberCurrentSubscriptionSwagger | null;

  @ApiProperty({ type: [MemberCurrentSubscriptionSwagger] })
  upcoming_subscriptions: MemberCurrentSubscriptionSwagger[];

  @ApiProperty({ type: [MemberCurrentSubscriptionSwagger] })
  expired_subscriptions: MemberCurrentSubscriptionSwagger[];

  @ApiProperty({ type: [MemberFreezeSubscriptionSwagger] })
  freeze_subscriptions: MemberFreezeSubscriptionSwagger[];

  @ApiPropertyOptional({
    type: MemberProfileGymSwagger,
    description: 'Present when `gymId` query is set.',
  })
  gym?: MemberProfileGymSwagger;

  @ApiPropertyOptional({
    type: MemberProfileSubscriptionSwagger,
    description:
      'Grouped subscription data when `gymId` is set (includes `past_subscriptions`).',
  })
  subscription?: MemberProfileSubscriptionSwagger;

  @ApiPropertyOptional({
    type: [MemberCurrentSubscriptionSwagger],
    description:
      'Alias for `expired_subscriptions` when `gymId` is set (past subscription list).',
  })
  past_subscriptions?: MemberCurrentSubscriptionSwagger[];
}

export class MemberListPaginationSwagger {
  @ApiProperty()
  page: number;

  @ApiProperty()
  total_pages: number;

  @ApiProperty()
  total_records: number;
}

export class MemberListStatsSwagger {
  @ApiProperty()
  active: number;

  @ApiProperty()
  inactive: number;

  @ApiProperty()
  expired: number;

  @ApiProperty()
  total_members: number;
}

/** `GET /members` — paged list; `members` and `items` mirror `GET /members/{memberId}` detail. */
export class MemberListResponseSwagger {
  @ApiProperty({ type: [MemberDetailResponseSwagger] })
  members: MemberDetailResponseSwagger[];

  @ApiProperty({ type: MemberListPaginationSwagger })
  pagination: MemberListPaginationSwagger;

  @ApiProperty({ type: MemberListStatsSwagger })
  stats: MemberListStatsSwagger;

  @ApiProperty()
  total: number;

  @ApiProperty()
  limit: number;

  @ApiProperty()
  offset: number;

  @ApiProperty({ type: [MemberDetailResponseSwagger] })
  items: MemberDetailResponseSwagger[];
}
