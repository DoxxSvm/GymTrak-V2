import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { InjectQueue } from '@nestjs/bullmq';
import type { Queue } from 'bullmq';
import { WHATSAPP_QUEUE } from './whatsapp-automation.service';

/**
 * Registers a daily repeatable job to scan memberships and enqueue expiry WhatsApp reminders.
 */
@Injectable()
export class WhatsAppSchedulerService implements OnModuleInit {
  private readonly logger = new Logger(WhatsAppSchedulerService.name);

  constructor(
    @InjectQueue(WHATSAPP_QUEUE) private readonly whatsappQueue: Queue,
  ) {}

  async onModuleInit(): Promise<void> {
    try {
      await this.whatsappQueue.add(
        'scan-expiry-reminders',
        {},
        {
          repeat: { pattern: '0 8 * * *' },
          jobId: 'repeat-scan-expiry-reminders',
          removeOnComplete: true,
          attempts: 3,
          backoff: { type: 'exponential', delay: 60_000 },
        },
      );
      this.logger.log(
        'Registered daily WhatsApp automation scan: pre-expiry (7d, 3d), post-expiry follow-up (08:00 UTC cron)',
      );
    } catch (e) {
      this.logger.warn(
        `Could not register repeatable job (Redis unavailable?): ${(e as Error).message}`,
      );
    }
  }
}
