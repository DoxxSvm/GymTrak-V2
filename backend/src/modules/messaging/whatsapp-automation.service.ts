import { Injectable } from '@nestjs/common';
import { InjectQueue } from '@nestjs/bullmq';
import type { JobsOptions } from 'bullmq';
import type { Queue } from 'bullmq';

export const WHATSAPP_QUEUE = 'whatsapp';

export type WhatsAppJobName = 'send-text' | 'scan-expiry-reminders';

/** Default retry + backoff for outbound WhatsApp jobs (async, resilient). */
export const WHATSAPP_SEND_JOB_OPTS: JobsOptions = {
  attempts: 5,
  backoff: { type: 'exponential', delay: 3000 },
  removeOnComplete: 1000,
  removeOnFail: 5000,
};

export type SendTextJob = {
  kind:
    | 'WELCOME'
    | 'PAYMENT_CONFIRMATION'
    | 'EXPIRY_REMINDER_7D'
    | 'EXPIRY_REMINDER_3D'
    | 'POST_EXPIRY';
  gymId: string;
  memberUserId: string;
  paymentId?: string;
  /** Set for expiry scans (precise subscription + template vars). */
  memberSubscriptionId?: string;
};

/**
 * Enqueues async WhatsApp work. Processors apply per-gym template toggles + custom text,
 * then send via Meta or Twilio (see WHATSAPP_PROVIDER).
 */
@Injectable()
export class WhatsAppAutomationService {
  constructor(
    @InjectQueue(WHATSAPP_QUEUE) private readonly whatsappQueue: Queue,
  ) {}

  async enqueueWelcome(gymId: string, memberUserId: string): Promise<void> {
    await this.whatsappQueue.add(
      'send-text',
      {
        kind: 'WELCOME',
        gymId,
        memberUserId,
      } satisfies SendTextJob,
      WHATSAPP_SEND_JOB_OPTS,
    );
  }

  async enqueuePaymentConfirmation(
    gymId: string,
    memberUserId: string,
    paymentId: string,
  ): Promise<void> {
    await this.whatsappQueue.add(
      'send-text',
      {
        kind: 'PAYMENT_CONFIRMATION',
        gymId,
        memberUserId,
        paymentId,
      } satisfies SendTextJob,
      WHATSAPP_SEND_JOB_OPTS,
    );
  }

  /**
   * Manual / ad-hoc expiry reminder (uses same template as 7-day pre-expiry copy).
   */
  async enqueueExpiryReminder(
    gymId: string,
    memberUserId: string,
    memberSubscriptionId?: string,
  ): Promise<void> {
    await this.whatsappQueue.add(
      'send-text',
      {
        kind: 'EXPIRY_REMINDER_7D',
        gymId,
        memberUserId,
        memberSubscriptionId,
      } satisfies SendTextJob,
      WHATSAPP_SEND_JOB_OPTS,
    );
  }
}
