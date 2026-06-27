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
    format: 'date-time',
    nullable: true,
    description: 'GymUser.membershipEndsAt',
  })
  expiry_date: Date | null;

  @ApiProperty({ description: 'Avatar URL or empty string' })
  profile_image: string;
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
  @ApiProperty()
  plan_name: string;

  @ApiProperty({ type: String, format: 'date-time' })
  start_date: Date;

  @ApiProperty({ type: String, format: 'date-time' })
  expiry_date: Date;

  @ApiProperty()
  remaining_days: number;

  @ApiProperty()
  amount_paid: number;

  @ApiProperty()
  amount_pending: number;
}

export class MemberDetailSubscriptionSwagger {
  @ApiProperty({ type: MemberSubscriptionStatsSwagger })
  stats: MemberSubscriptionStatsSwagger;

  @ApiPropertyOptional({
    type: MemberCurrentSubscriptionSwagger,
    nullable: true,
  })
  current_subscription: MemberCurrentSubscriptionSwagger | null;
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

  @ApiProperty({ type: String, format: 'date-time' })
  createdAt: Date;

  @ApiPropertyOptional({ nullable: true })
  avatarUrl: string | null;
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

  @ApiProperty({ type: MemberSubscriptionStatsSwagger })
  stats: MemberSubscriptionStatsSwagger;

  @ApiPropertyOptional({
    type: MemberCurrentSubscriptionSwagger,
    nullable: true,
  })
  current_subscription: MemberCurrentSubscriptionSwagger | null;
}
