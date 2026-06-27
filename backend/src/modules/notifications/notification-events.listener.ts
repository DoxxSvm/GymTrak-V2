import { Injectable } from '@nestjs/common';
import { OnEvent } from '@nestjs/event-emitter';
import {
  GymRole,
  NotificationEntityType,
  NotificationType,
} from '@prisma/client';
import {
  NOTIFICATION_EVENTS,
  type EnquiryConvertedPayload,
  type EnquiryCreatedPayload,
  type ExpiryAlertPayload,
  type MemberAddedPayload,
  type PaymentReceivedPayload,
  type ExtendPlanPayload,
  type ExpenseCreatedPayload,
  type PlanAssignedPayload,
  type TrainerSalaryPaidPayload,
} from './domain-events';
import { PrismaService } from '../prisma/prisma.service';
import { NotificationsService } from './notifications.service';

function moneyLine(amountCents: number, currency: string) {
  const major = amountCents.toString();
  return `${currency} ${major}`;
}

@Injectable()
export class NotificationEventsListener {
  constructor(
    private readonly notifications: NotificationsService,
    private readonly prisma: PrismaService,
  ) {}

  @OnEvent(NOTIFICATION_EVENTS.MEMBER_ADDED)
  async onMemberAdded(payload: MemberAddedPayload) {
    const memberLabel = payload.memberName?.trim() || 'New member';

    const [actor, actorGymUser] = await Promise.all([
      this.prisma.user.findUnique({
        where: { id: payload.actorUserId },
        select: { fullName: true, phone: true },
      }),
      this.prisma.gymUser.findFirst({
        where: { gymId: payload.gymId, userId: payload.actorUserId },
        select: { role: true },
      }),
    ]);

    const actorLabel =
      actor?.fullName?.trim() || actor?.phone?.trim() || 'A team member';

    const role = actorGymUser?.role;
    const rolePhrase =
      role === GymRole.TRAINER
        ? `trainer '${actorLabel}'`
        : role === GymRole.OWNER
          ? `gym owner '${actorLabel}'`
          : role === GymRole.STAFF
            ? `staff '${actorLabel}'`
            : `'${actorLabel}'`;

    const body = `'${memberLabel}' was added as a new member by ${rolePhrase}.`;

    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.MEMBER_ADDED,
      title: `New member — ${memberLabel}`,
      body,
      entityType: NotificationEntityType.MEMBER,
      entityId: payload.gymUserId,
      actorUserId: payload.actorUserId,
      metadata: {
        memberName: memberLabel,
        gymUserId: payload.gymUserId,
        actorName: actorLabel,
        deepLink: {
          screen: 'member',
          params: {
            gymId: payload.gymId,
            gymUserId: payload.gymUserId,
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.EXTEND_PLAN)
  async onExtendPlan(payload: ExtendPlanPayload) {
    const memberLabel = payload.memberName?.trim() || 'Member';

    const [actor, actorGymUser] = await Promise.all([
      this.prisma.user.findUnique({
        where: { id: payload.actorUserId },
        select: { fullName: true, phone: true },
      }),
      this.prisma.gymUser.findFirst({
        where: { gymId: payload.gymId, userId: payload.actorUserId },
        select: { role: true },
      }),
    ]);

    const actorLabel =
      actor?.fullName?.trim() || actor?.phone?.trim() || 'A team member';
    const role = actorGymUser?.role;
    const rolePhrase =
      role === GymRole.TRAINER
        ? `trainer '${actorLabel}'`
        : role === GymRole.OWNER
          ? `gym owner '${actorLabel}'`
          : role === GymRole.STAFF
            ? `staff '${actorLabel}'`
            : `'${actorLabel}'`;

    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.PLAN_ASSIGNED,
      title: `Plan extended — ${memberLabel}`,
      body: `'${memberLabel}' had their membership plan extended by ${rolePhrase}.`,
      entityType: NotificationEntityType.MEMBER_SUBSCRIPTION,
      entityId: payload.memberSubscriptionId,
      actorUserId: payload.actorUserId,
      metadata: {
        memberName: memberLabel,
        gymUserId: payload.gymUserId,
        deepLink: {
          screen: 'subscription',
          params: {
            gymId: payload.gymId,
            gymUserId: payload.gymUserId,
            memberSubscriptionId: payload.memberSubscriptionId,
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.PLAN_ASSIGNED)
  async onPlanAssigned(payload: PlanAssignedPayload) {
    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.PLAN_ASSIGNED,
      title: 'Plan assigned',
      body: 'A membership plan was assigned to a member.',
      entityType: NotificationEntityType.MEMBER_SUBSCRIPTION,
      entityId: payload.memberSubscriptionId,
      actorUserId: payload.actorUserId,
      metadata: {
        deepLink: {
          screen: 'subscription',
          params: {
            gymId: payload.gymId,
            gymUserId: payload.gymUserId,
            memberSubscriptionId: payload.memberSubscriptionId,
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.PAYMENT_RECEIVED)
  async onPaymentReceived(payload: PaymentReceivedPayload) {
    const line = moneyLine(payload.amountCents, payload.currency);
    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.PAYMENT_RECEIVED,
      title: 'Payment received',
      body: `Payment of ${line} was recorded.`,
      entityType: NotificationEntityType.PAYMENT,
      entityId: payload.paymentId,
      actorUserId: payload.actorUserId,
      metadata: {
        deepLink: {
          screen: 'payment',
          params: {
            gymId: payload.gymId,
            gymUserId: payload.gymUserId,
            paymentId: payload.paymentId,
            ...(payload.memberSubscriptionId
              ? { memberSubscriptionId: payload.memberSubscriptionId }
              : {}),
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.ENQUIRY_CREATED)
  async onEnquiryCreated(payload: EnquiryCreatedPayload) {
    const label = payload.leadName?.trim() || 'New lead';
    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.ENQUIRY,
      title: 'New enquiry',
      body: `Enquiry received for ${label}.`,
      entityType: NotificationEntityType.ENQUIRY,
      entityId: payload.enquiryId,
      actorUserId: payload.actorUserId,
      metadata: {
        deepLink: {
          screen: 'enquiry',
          params: {
            gymId: payload.gymId,
            enquiryId: payload.enquiryId,
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.ENQUIRY_CONVERTED)
  async onEnquiryConverted(payload: EnquiryConvertedPayload) {
    const name = payload.memberName?.trim() || 'Member';
    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.ENQUIRY,
      title: 'Enquiry converted',
      body: `Enquiry converted — ${name} was added as a member.`,
      entityType: NotificationEntityType.MEMBER,
      entityId: payload.memberGymUserId,
      actorUserId: payload.actorUserId,
      metadata: {
        deepLink: {
          screen: 'member',
          params: {
            gymId: payload.gymId,
            gymUserId: payload.memberGymUserId,
            enquiryId: payload.enquiryId,
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.EXPIRY_ALERT)
  async onExpiryAlert(payload: ExpiryAlertPayload) {
    const name = payload.memberName?.trim() || 'A member';
    const scanDayKey = new Date().toISOString().slice(0, 10);

    let body: string;
    let title: string;
    let dedupeKey: string;

    if (payload.phase === 'post') {
      title = 'Membership ended';
      body = `${name}'s plan just ended — consider a follow-up.`;
      dedupeKey = `expiry-post:${payload.memberSubscriptionId}:${scanDayKey}`;
    } else {
      const d = payload.daysBeforeExpiry ?? 0;
      const dayLabel = d === 1 ? 'tomorrow' : d > 0 ? `in ${d} days` : 'soon';
      title = 'Membership expiring';
      body = `${name}'s plan ends ${dayLabel}.`;
      dedupeKey = `expiry:${payload.memberSubscriptionId}:${d}:${scanDayKey}`;
    }

    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.EXPIRY_ALERT,
      title,
      body,
      entityType: NotificationEntityType.MEMBER_SUBSCRIPTION,
      entityId: payload.memberSubscriptionId,
      dedupeKey,
      metadata: {
        deepLink: {
          screen: 'expiry',
          params: {
            gymId: payload.gymId,
            gymUserId: payload.gymUserId,
            memberSubscriptionId: payload.memberSubscriptionId,
          },
        },
      },
    });
  }

  @OnEvent(NOTIFICATION_EVENTS.TRAINER_SALARY_PAID)
  async onTrainerSalaryPaid(payload: TrainerSalaryPaidPayload) {
    const payeeLabel = payload.trainerName?.trim() || 'Team member';
    const { actorLabel, rolePhrase } = await this.actorRolePhrase(
      payload.gymId,
      payload.actorUserId,
    );
    const line = moneyLine(payload.amountCents, payload.currency);
    const screen = this.salaryScreenForPayeeRole(payload.payeeRole);

    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.PAYMENT,
      title: `Salary paid — ${payeeLabel}`,
      body: `${line} salary paid to '${payeeLabel}' by ${rolePhrase}.`,
      entityType: NotificationEntityType.TRAINER,
      entityId: payload.trainerGymUserId,
      actorUserId: payload.actorUserId,
      metadata: {
        memberName: payeeLabel,
        gymUserId: payload.trainerGymUserId,
        payeeRole: payload.payeeRole,
        actorName: actorLabel,
        salaryPaymentId: payload.salaryPaymentId,
        amountCents: payload.amountCents,
        screen,
        deepLink: {
          screen,
          params: {
            gymId: payload.gymId,
            gymUserId: payload.trainerGymUserId,
            salaryPaymentId: payload.salaryPaymentId,
          },
        },
      },
    });
  }

  private salaryScreenForPayeeRole(role: 'TRAINER' | 'STAFF'): string {
    return role === GymRole.STAFF ? 'staff-salary' : 'trainer-salary';
  }

  @OnEvent(NOTIFICATION_EVENTS.EXPENSE_CREATED)
  async onExpenseCreated(payload: ExpenseCreatedPayload) {
    const { actorLabel, rolePhrase } = await this.actorRolePhrase(
      payload.gymId,
      payload.actorUserId,
    );
    const line = moneyLine(payload.amountCents, payload.currency);
    const categoryLabel = this.expenseCategoryLabel(payload.category);
    const billLabel = payload.description?.trim();
    const subjectLabel =
      payload.trainerName?.trim() || billLabel || categoryLabel;
    const title = `Expense — ${subjectLabel}`;
    const trainerPart = payload.trainerName
      ? ` for '${payload.trainerName.trim()}'`
      : billLabel
        ? ` ('${billLabel}')`
        : '';
    const body = `${line} ${categoryLabel} expense${trainerPart} recorded by ${rolePhrase}.`;

    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.INFO,
      title,
      body,
      entityType: payload.trainerGymUserId
        ? NotificationEntityType.TRAINER
        : undefined,
      entityId: payload.trainerGymUserId ?? undefined,
      actorUserId: payload.actorUserId,
      metadata: {
        expenseId: payload.expenseId,
        category: payload.category,
        memberName: subjectLabel,
        ...(payload.trainerGymUserId
          ? { gymUserId: payload.trainerGymUserId }
          : {}),
        actorName: actorLabel,
        amountCents: payload.amountCents,
        deepLink: {
          screen: 'expense',
          params: {
            gymId: payload.gymId,
            expenseId: payload.expenseId,
            ...(payload.trainerGymUserId
              ? { gymUserId: payload.trainerGymUserId }
              : {}),
          },
        },
      },
    });
  }

  private expenseCategoryLabel(category: string): string {
    const labels: Record<string, string> = {
      RENT: 'Rent',
      UTILITIES: 'Utilities',
      EQUIPMENT: 'Equipment',
      MAINTENANCE: 'Maintenance',
      SUPPLIES: 'Supplies',
      SALARY: 'Salary',
      MARKETING: 'Marketing',
      SOFTWARE: 'Software',
      OTHER: 'Other',
    };
    return labels[category] ?? category;
  }

  private async actorRolePhrase(
    gymId: string,
    actorUserId: string,
  ): Promise<{ actorLabel: string; rolePhrase: string }> {
    const [actor, actorGymUser] = await Promise.all([
      this.prisma.user.findUnique({
        where: { id: actorUserId },
        select: { fullName: true, phone: true },
      }),
      this.prisma.gymUser.findFirst({
        where: { gymId, userId: actorUserId },
        select: { role: true },
      }),
    ]);
    const actorLabel =
      actor?.fullName?.trim() || actor?.phone?.trim() || 'A team member';
    const role = actorGymUser?.role;
    const rolePhrase =
      role === GymRole.TRAINER
        ? `trainer '${actorLabel}'`
        : role === GymRole.OWNER
          ? `gym owner '${actorLabel}'`
          : role === GymRole.STAFF
            ? `staff '${actorLabel}'`
            : `'${actorLabel}'`;
    return { actorLabel, rolePhrase };
  }
}
