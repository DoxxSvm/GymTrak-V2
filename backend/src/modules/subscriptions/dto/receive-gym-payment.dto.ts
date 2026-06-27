import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';

/** One line item (single `POST /payments` body or an element of `items`). */
export class ReceiveGymPaymentDto {
  /**
   * `extend_plan` (default): create a new `MemberSubscription` for `gym_plan_id`, or extend an
   * existing one when `member_subscription_id` is set (`date` → new `endsAt`, optional `fees` / payment).
   * `receive_payment`: update an existing subscription (`member_subscription_id`) and record payment — no new subscription row.
   */
  @ApiPropertyOptional({
    enum: ['extend_plan', 'receive_payment'],
    description:
      'Omit or `extend_plan`: new subscription for `gym_plan_id`, or extend existing when `member_subscription_id` + `date` are set. `receive_payment`: renew window on existing `member_subscription_id` using this plan’s `durationDays` + optional `date`.',
  })
  @IsOptional()
  @IsIn(['extend_plan', 'receive_payment'])
  type?: 'extend_plan' | 'receive_payment';

  @ApiProperty({ description: '`GymUser.id` of the member.' })
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @ApiProperty({
    description:
      '`GymPlan.id`. extend_plan: new subscription for this plan. receive_payment: `durationDays` from this plan sets `endsAt` from `date`-based `startsAt`.',
  })
  @IsString()
  @IsNotEmpty()
  gym_plan_id: string;

  @ApiPropertyOptional({
    minimum: 1,
    description:
      'Required for `receive_payment`. Optional for `extend_plan` when extending an existing subscription (records `Payment` and increments `paidCents`). Minor units (`Payment.amountCents`).',
  })
  @ValidateIf((o) => o.type === 'receive_payment')
  @Type(() => Number)
  @IsInt()
  @Min(1)
  amount?: number;

  @ApiPropertyOptional({ enum: ['cash', 'upi', 'card'] })
  @ValidateIf(
    (o) =>
      o.type === 'receive_payment' ||
      (o.type !== 'receive_payment' &&
        o.member_subscription_id?.trim() &&
        o.amount != null),
  )
  @IsString()
  @IsIn(['cash', 'upi', 'card'])
  payment_mode?: 'cash' | 'upi' | 'card';

  /**
   * Period start (`startsAt`) or extension end (`endsAt`). `YYYY-MM-DD` → UTC midnight; omit → today (UTC); ISO date-time allowed.
   * extend_plan without `member_subscription_id`: new subscription `startsAt`.
   * extend_plan with `member_subscription_id`: new subscription `endsAt` (must be after current expiry).
   * receive_payment: existing subscription window start.
   */
  @ApiPropertyOptional({
    description:
      '`YYYY-MM-DD` (UTC midnight) or ISO date-time. extend_plan (new row): optional `startsAt` (omit = today UTC). extend_plan (existing `member_subscription_id`): required new `endsAt`. receive_payment: optional `startsAt`; `endsAt` = `startsAt` + plan `durationDays`.',
  })
  @IsOptional()
  @IsString()
  date?: string;

  @ApiPropertyOptional({
    description:
      '`MemberSubscription.id` for this member (`gymUserId` must match `member_id`). Required for `receive_payment`. For `extend_plan`, extends that row’s `endsAt` to `date` instead of creating a new subscription.',
  })
  @ValidateIf((o) => o.type === 'receive_payment')
  @IsString()
  @IsNotEmpty()
  member_subscription_id?: string;

  @ApiPropertyOptional({
    minimum: 0,
    description:
      'extend_plan only (current active `member_subscription_id`). Extension fee in minor units added to that subscription `priceCents`. Does not use gym plan catalog price. `amount` is payment only and does not change `priceCents`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  fees?: number;

  @ApiPropertyOptional({
    minimum: 0,
    description:
      'Mobile alias for `fees` on `extend_plan` — only this fee is added to `priceCents`; `amount` is payment only.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  addition_fee?: number;

  @ApiPropertyOptional({
    minimum: 0,
    description: 'Alias for `fees` / `addition_fee` on `extend_plan`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  additional_fee?: number;

  @ApiPropertyOptional({
    minimum: 0,
    description:
      'extend_plan: custom selling price for this subscription (minor units). Sets `MemberSubscription.priceCents` when provided; does not use gym catalog price. Mobile may send instead of `amount` when only updating price/expiry.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  selling_price?: number;

  @ApiPropertyOptional({
    description:
      'Mobile alias for `date` on extend_plan (period start or new expiry depending on flow). Prefer `date`.',
  })
  @IsOptional()
  @IsString()
  start_date?: string;
}

/**
 * `POST /payments` body: either flat fields **or** `{ items: ReceiveGymPaymentDto[] }` (1–50).
 * When `items` is non-empty, flat fields are ignored.
 */
export class ReceiveGymPaymentBodyDto {
  @ApiPropertyOptional({
    type: () => [ReceiveGymPaymentDto],
    minItems: 1,
    maxItems: 50,
    description:
      'Batch: 1–50 entries; when present, top-level flat fields are ignored. Each item follows the same extend_plan / receive_payment rules.',
  })
  @IsOptional()
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(50)
  @ValidateNested({ each: true })
  @Type(() => ReceiveGymPaymentDto)
  items?: ReceiveGymPaymentDto[];

  @ApiPropertyOptional({
    enum: ['extend_plan', 'receive_payment'],
    description:
      'Flat body only. Omit or `extend_plan` with `gym_plan_id` + optional `date`. `receive_payment` with `member_subscription_id`, `gym_plan_id`, optional `date`.',
  })
  @IsOptional()
  @IsIn(['extend_plan', 'receive_payment'])
  type?: 'extend_plan' | 'receive_payment';

  @ApiPropertyOptional({ description: '`GymUser.id` (ignored when `items` is set).' })
  @ValidateIf((o) => !o.items?.length)
  @IsString()
  @IsNotEmpty()
  member_id?: string;

  @ApiPropertyOptional({
    description:
      'Required for flat body without `items` (extend_plan and receive_payment).',
  })
  @ValidateIf((o) => !o.items?.length)
  @IsString()
  @IsNotEmpty()
  gym_plan_id?: string;

  @ApiPropertyOptional({
    minimum: 1,
    description:
      'Required for flat `receive_payment`. Optional for `extend_plan` with `member_subscription_id`. Ignored when `items` is set.',
  })
  @ValidateIf((o) => !o.items?.length && o.type === 'receive_payment')
  @Type(() => Number)
  @IsInt()
  @Min(1)
  amount?: number;

  @ApiPropertyOptional({
    enum: ['cash', 'upi', 'card'],
    description:
      'Required for flat `receive_payment`, or `extend_plan` with `member_subscription_id` when `amount` is set. Ignored when `items` is set.',
  })
  @ValidateIf(
    (o) =>
      !o.items?.length &&
      (o.type === 'receive_payment' ||
        (o.type !== 'receive_payment' &&
          o.member_subscription_id?.trim() &&
          o.amount != null)),
  )
  @IsString()
  @IsIn(['cash', 'upi', 'card'])
  payment_mode?: 'cash' | 'upi' | 'card';

  @ApiPropertyOptional({
    description:
      'Optional period start (ignored when `items` is set). extend_plan / receive_payment: same rules as `ReceiveGymPaymentDto.date`.',
  })
  @IsOptional()
  @IsString()
  date?: string;

  @ApiPropertyOptional({
    description:
      'Flat body. Required for `receive_payment`. For `extend_plan`, extends existing subscription when set.',
  })
  @ValidateIf((o) => !o.items?.length && o.type === 'receive_payment')
  @IsString()
  @IsNotEmpty()
  member_subscription_id?: string;

  @ApiPropertyOptional({
    minimum: 0,
    description:
      'Flat `extend_plan` with `member_subscription_id`. Extra fee added to subscription `priceCents`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  fees?: number;

  @ApiPropertyOptional({
    minimum: 0,
    description: 'Flat `extend_plan` — mobile alias for `fees`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  addition_fee?: number;

  @ApiPropertyOptional({
    minimum: 0,
    description: 'Flat `extend_plan` — alias for `fees`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  additional_fee?: number;

  @ApiPropertyOptional({
    minimum: 0,
    description:
      'Flat `extend_plan`: custom selling price (minor units) — sets subscription `priceCents`.',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  selling_price?: number;

  @ApiPropertyOptional({
    description: 'Flat body — mobile alias for `date`.',
  })
  @IsOptional()
  @IsString()
  start_date?: string;
}
