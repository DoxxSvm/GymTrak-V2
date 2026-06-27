import { Body, Controller, Get, Post, Query, UseGuards } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOkResponse,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PaymentAnalyticsQueryDto } from './dto/payment-analytics-query.dto';
import { PaymentListQueryDto } from './dto/payment-list-query.dto';
import { SubscriptionsService } from './subscriptions.service';
import { ReceiveGymPaymentBodyDto } from './dto/receive-gym-payment.dto';
import { receiveGymPaymentBodyExamples } from './dto/receive-gym-payment.swagger-examples';
import { CreateTrainerSalaryPaymentDto } from './dto/create-trainer-salary-payment.dto';
import { TrainerSalaryHistoryQueryDto } from './dto/trainer-salary-history-query.dto';

@ApiTags('Payments')
@ApiBearerAuth()
@Controller('payments')
export class PaymentsController {
  constructor(private readonly subscriptions: SubscriptionsService) {}

  /** Daily series + list for charts (completed payments in range) */
  @Get('analytics')
  analytics(
    @CurrentUser() user: JwtUser,
    @Query() query: PaymentAnalyticsQueryDto,
  ) {
    const range = query.range ?? 'monthly';
    const now = new Date();
    const from =
      query.from ??
      (range === 'weekly'
        ? (() => {
            const d = new Date(now);
            const dow = d.getUTCDay();
            const delta = dow === 0 ? -6 : 1 - dow;
            d.setUTCDate(d.getUTCDate() + delta);
            d.setUTCHours(0, 0, 0, 0);
            return d.toISOString();
          })()
        : range === 'yearly'
          ? new Date(Date.UTC(now.getUTCFullYear() - 2, 0, 1)).toISOString()
          : new Date(Date.UTC(now.getUTCFullYear(), 0, 1)).toISOString());
    const to = query.to ?? now.toISOString();
    return this.subscriptions.paymentAnalytics(
      user.sub,
      query.gymId,
      from,
      to,
      range,
    );
  }

  /** Gym-wide payment history; filter by status (e.g. PENDING) or member */
  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: PaymentListQueryDto) {
    const limit = query.limit ?? 20;
    const page = query.page ?? 1;
    const offset = query.offset ?? (page - 1) * limit;
    return this.subscriptions.listPaymentsForGym(
      user.sub,
      query.gymId,
      query.status,
      query.memberId,
      limit,
      offset,
      query.search,
    );
  }

  /**
   * Receive one payment (flat body) or several (`{ "items": [ ... ] }`, max 50).
   * - `type` omitted or `extend_plan`: new `MemberSubscription`, or extend existing when `member_subscription_id` + `date` are set.
   * - `type: "receive_payment"`: `member_subscription_id` + `gym_plan_id` + optional `date` — updates that row (`startsAt`/`endsAt`/`status`), increments `paidCents` by `amount`, then records `Payment` (no new subscription row).
   */
  @Post()
  @ApiOperation({
    summary: 'Receive member payment',
    description:
      'Requires Bearer auth. **extend_plan** (default): new `MemberSubscription` from `gym_plan_id` + optional `date`, or extend existing when `member_subscription_id` + `date` are set (`extend_days` from expiry, optional `fees` → `priceCents`, optional `amount` → `Payment`). **receive_payment**: `member_subscription_id` + `gym_plan_id` + optional `date` — `startsAt` from `date`, `endsAt` = `startsAt` + plan `durationDays`, `status` from window, `paidCents` += `amount`; then completed `Payment`. Completed payments enqueue **Payment Received** WhatsApp when the gym template is enabled. Use **`items`** (1–50) for batch. Canonical JSON Schema: bundled OpenAPI `ReceiveGymPayment` / `ReceiveGymPaymentsBatchBody`.',
  })
  @ApiBody({
    type: ReceiveGymPaymentBodyDto,
    description:
      'Either a flat payment object or `{ "items": [ ... ] }`. Conditional required fields match `class-validator` on `ReceiveGymPaymentBodyDto` / `ReceiveGymPaymentDto`. Use **Examples** for sample JSON.',
    examples: receiveGymPaymentBodyExamples,
  })
  @ApiOkResponse({
    description:
      'Single body: `extend_plan` → `{ type, memberSubscriptionId }`; `receive_payment` → payment record. Batch: `{ "payments": [...], "count": n }` (mixed shapes per item `type`).',
  })
  receive(@CurrentUser() user: JwtUser, @Body() body: ReceiveGymPaymentBodyDto) {
    if (body.items?.length) {
      return this.subscriptions.receivePaymentsByMemberBatch(user.sub, body.items);
    }
    const kind = body.type ?? 'extend_plan';
    if (kind === 'receive_payment') {
      return this.subscriptions.receivePaymentOnExistingSubscription(user.sub, {
        type: 'receive_payment',
        member_id: body.member_id!,
        gym_plan_id: body.gym_plan_id!,
        amount: body.amount!,
        payment_mode: body.payment_mode!,
        member_subscription_id: body.member_subscription_id!,
        date: body.date,
      });
    }
    return this.subscriptions.receivePaymentByMember(user.sub, {
      type: 'extend_plan',
      member_id: body.member_id!,
      gym_plan_id: body.gym_plan_id!,
      amount: body.amount,
      payment_mode: body.payment_mode,
      date: body.date ?? body.start_date,
      member_subscription_id: body.member_subscription_id,
      fees: body.fees,
      addition_fee: body.addition_fee,
      additional_fee: body.additional_fee,
      selling_price: body.selling_price,
      start_date: body.start_date,
    });
  }

  /** Mobile salary modal: create a trainer salary payment entry. */
  @Post('salary')
  @ApiOperation({
    summary: 'Create trainer/staff salary payment',
    description:
      '`trainer_id` is a `GymUser.id` with role **TRAINER** or **STAFF** at `gymId`.',
  })
  @ApiBody({ type: CreateTrainerSalaryPaymentDto })
  @ApiOkResponse({
    schema: {
      example: {
        success: true,
        payment_id: 'salary_payment_123',
        monthly_salary: 5000,
        paid_amount_this_month: 4000,
        pending_amount: 1000,
      },
    },
  })
  createTrainerSalaryPayment(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateTrainerSalaryPaymentDto,
  ) {
    return this.subscriptions.createTrainerSalaryPayment(user.sub, body);
  }

  /** Salary summary + payment history (for salary card/history screen). */
  @Get('salary/history')
  @ApiOperation({ summary: 'Get trainer salary payment history' })
  @ApiQuery({ name: 'gymId', required: true, type: String })
  @ApiQuery({ name: 'trainer_id', required: true, type: String })
  @ApiQuery({
    name: 'date',
    required: false,
    type: String,
    description: 'YYYY-MM-DD anchor date for month summary',
  })
  @ApiOkResponse({
    schema: {
      example: {
        monthly_salary: 5000,
        paid_amount: 4000,
        pending_amount: 1000,
        payment_history: [
          {
            id: 'salary_payment_123',
            amount: 1500,
            date: '2026-03-12',
            mode: 'upi',
          },
        ],
      },
    },
  })
  trainerSalaryHistory(
    @CurrentUser() user: JwtUser,
    @Query() query: TrainerSalaryHistoryQueryDto,
  ) {
    return this.subscriptions.getTrainerSalaryHistory(user.sub, query);
  }
}
