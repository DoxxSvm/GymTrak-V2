import { Injectable } from '@nestjs/common';

/**
 * No-op stand-in when `DISABLE_BULLMQ=true` (no Redis). Keeps Members/Subscriptions
 * injectable without BullMQ workers.
 */
@Injectable()
export class NoopWhatsAppAutomationService {
  async enqueueWelcome(gymId: string, memberUserId: string): Promise<void> {
    void gymId;
    void memberUserId;
  }

  async enqueuePaymentConfirmation(
    gymId: string,
    memberUserId: string,
    paymentId: string,
  ): Promise<void> {
    void gymId;
    void memberUserId;
    void paymentId;
  }

  async enqueueExpiryReminder(
    gymId: string,
    memberUserId: string,
    memberSubscriptionId?: string,
  ): Promise<void> {
    void gymId;
    void memberUserId;
    void memberSubscriptionId;
  }
}
