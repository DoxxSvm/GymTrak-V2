import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ExpenseCategory, GymRole, SalaryPeriod } from '@prisma/client';

/** Trainer payload when an expense is linked to a trainer (matches GET /trainers list item shape). */
export class ExpenseTrainerSalarySwagger {
  @ApiPropertyOptional({ nullable: true })
  salaryCents: number | null;

  @ApiPropertyOptional({
    nullable: true,
    enum: SalaryPeriod,
    enumName: 'SalaryPeriod',
  })
  salaryPeriod: SalaryPeriod | null;

  @ApiPropertyOptional({ nullable: true, example: '2026-01-01' })
  contractStartsAt: string | null;

  @ApiPropertyOptional({ nullable: true, example: '2026-12-31' })
  contractEndsAt: string | null;

  @ApiPropertyOptional({ nullable: true })
  experience: string | null;

  @ApiPropertyOptional({ nullable: true })
  address: string | null;
}

export class ExpenseTrainerSwagger {
  @ApiProperty()
  gymUserId: string;

  @ApiProperty()
  userId: string;

  @ApiPropertyOptional({ nullable: true })
  fullName: string | null;

  @ApiProperty()
  phone: string;

  @ApiPropertyOptional({ nullable: true })
  email: string | null;

  @ApiPropertyOptional({ nullable: true })
  username: string | null;

  @ApiPropertyOptional({ nullable: true })
  avatarUrl: string | null;

  @ApiProperty()
  gymId: string;

  @ApiProperty({ enum: GymRole, enumName: 'GymRole' })
  role: GymRole;

  @ApiProperty()
  isActive: boolean;

  @ApiPropertyOptional({ nullable: true, example: '1990-05-04' })
  dateOfBirth: string | null;

  @ApiPropertyOptional({ nullable: true })
  gender: string | null;

  @ApiProperty()
  joinedAt: Date;

  @ApiProperty({ type: [String] })
  expertise: string[];

  @ApiPropertyOptional({
    type: ExpenseTrainerSalarySwagger,
    nullable: true,
  })
  salary: ExpenseTrainerSalarySwagger | null;
}

/** Unified expense JSON for list / get / create / update. */
export class ExpenseResponseSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty()
  gymId: string;

  @ApiProperty()
  bill_name: string;

  @ApiProperty({ enum: ExpenseCategory, enumName: 'ExpenseCategory' })
  category: ExpenseCategory;

  @ApiProperty({ example: '2026-04-13' })
  date: string;

  @ApiPropertyOptional({ nullable: true })
  trainer_id: string | null;

  @ApiPropertyOptional({
    type: ExpenseTrainerSwagger,
    nullable: true,
    description:
      'When trainer_id is set, full trainer (GymUser + user + profile) for display.',
  })
  trainer: ExpenseTrainerSwagger | null;

  @ApiPropertyOptional({
    nullable: true,
    enum: ['cash', 'upi', 'card'],
  })
  payment_mode: string | null;

  @ApiPropertyOptional({ nullable: true })
  gst: number | null;

  @ApiProperty({ description: 'Amount in major currency units' })
  amount: number;
}

export class ExpenseListResponseSwagger {
  @ApiProperty()
  total: number;

  @ApiProperty()
  limit: number;

  @ApiProperty()
  offset: number;

  @ApiProperty({ type: [ExpenseResponseSwagger] })
  items: ExpenseResponseSwagger[];

  @ApiProperty({
    description: 'Sum of amount (cents) for the current filter page query (all matching rows).',
  })
  totalAmountCents: number;

  @ApiProperty({
    description:
      'Normalized month-over-month spend delta vs previous UTC month (same category filter), bounded to -100..100. Formula: ((current - last) / max(current, last)) * 100. Defaults to 0 for yearly/custom date ranges.',
    example: 12.5,
  })
  percentageVsLastMonth: number;
}

export class ExpenseDeleteOkSwagger {
  @ApiProperty({ example: true })
  ok: boolean;
}
