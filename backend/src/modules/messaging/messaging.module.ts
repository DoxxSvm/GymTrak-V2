import { BullModule } from '@nestjs/bullmq';
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaModule } from '../prisma/prisma.module';
import { MessageTemplatesController } from './message-templates.controller';
import { MessageTemplatesService } from './message-templates.service';
import { WhatsAppAutomationController } from './whatsapp-automation.controller';
import { WhatsAppApiService } from './whatsapp-api.service';
import { WhatsAppTestController } from './whatsapp-test.controller';
import { WhatsAppTestService } from './whatsapp-test.service';
import { NoopWhatsAppAutomationService } from './whatsapp-automation.noop.service';
import {
  WHATSAPP_QUEUE,
  WhatsAppAutomationService,
} from './whatsapp-automation.service';
import { WhatsAppProcessor } from './whatsapp.processor';
import { WhatsAppSchedulerService } from './whatsapp-scheduler.service';

const bullDisabled =
  process.env.DISABLE_BULLMQ === 'true' || process.env.DISABLE_BULLMQ === '1';

@Module({
  imports: [
    PrismaModule,
    ...(bullDisabled
      ? []
      : [
          BullModule.forRootAsync({
            imports: [ConfigModule],
            useFactory: (config: ConfigService) => ({
              connection: {
                url:
                  config.get<string>('REDIS_URL') ?? 'redis://127.0.0.1:6379',
              },
            }),
            inject: [ConfigService],
          }),
          BullModule.registerQueue({
            name: WHATSAPP_QUEUE,
            defaultJobOptions: {
              attempts: 5,
              backoff: { type: 'exponential', delay: 3000 },
              removeOnComplete: 1000,
              removeOnFail: 5000,
            },
          }),
        ]),
  ],
  controllers: [
    MessageTemplatesController,
    WhatsAppAutomationController,
    WhatsAppTestController,
  ],
  providers: [
    GymAccessService,
    WhatsAppApiService,
    WhatsAppTestService,
    MessageTemplatesService,
    ...(bullDisabled
      ? [
          {
            provide: WhatsAppAutomationService,
            useClass: NoopWhatsAppAutomationService,
          },
        ]
      : [
          WhatsAppAutomationService,
          WhatsAppProcessor,
          WhatsAppSchedulerService,
        ]),
  ],
  exports: [
    WhatsAppAutomationService,
    MessageTemplatesService,
    WhatsAppApiService,
  ],
})
export class MessagingModule {}
