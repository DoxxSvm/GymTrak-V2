import { Injectable } from '@nestjs/common';
import { OnEvent } from '@nestjs/event-emitter';
import { NotificationEntityType, NotificationType } from '@prisma/client';
import {
  NOTIFICATION_EVENTS,
  type ExpiryAlertPayload,
  type MemberAddedPayload,
  type PaymentReceivedPayload,
  type PlanAssignedPayload,
} from './domain-events';
import { NotificationsService } from './notifications.service';

function moneyLine(amountCents: number, currency: string) {
  const major = (amountCents / 100).toFixed(2);
  return `${currency} ${major}`;
}

@Injectable()
export class NotificationEventsListener {
  constructor(private readonly notifications: NotificationsService) {}

  @OnEvent(NOTIFICATION_EVENTS.MEMBER_ADDED)
  async onMemberAdded(payload: MemberAddedPayload) {
    await this.notifications.createForGymOwner({
      gymId: payload.gymId,
      type: NotificationType.MEMBER_ADDED,
      title: 'New member',
      body: `${payload.memberName} was added to your gym.`,
      entityType: NotificationEntityType.MEMBER,
      entityId: payload.gymUserId,
      actorUserId: payload.actorUserId,
      metadata: {
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
}
