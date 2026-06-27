import { OnWorkerEvent, Processor, WorkerHost } from '@nestjs/bullmq';
import { Logger } from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { InjectQueue } from '@nestjs/bullmq';
import type { Job, Queue } from 'bullmq';
import {
  MemberSubscriptionStatus,
  MessageTemplateKind,
  PaymentStatus,
} from '@prisma/client';
import type { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { defaultBodyForKind, interpolate } from './template-copy';
import { MessageTemplatesService } from './message-templates.service';
import { formatPaymentAmount } from './whatsapp-templates';
import type { SendTextJob } from './whatsapp-automation.service';
import {
  WHATSAPP_QUEUE,
  WHATSAPP_SEND_JOB_OPTS,
} from './whatsapp-automation.service';
import { WhatsAppApiService } from './whatsapp-api.service';
import { NOTIFICATION_EVENTS } from '../notifications/domain-events';

/** Days before `endsAt` (UTC calendar day) to enqueue pre-expiry WhatsApp messages. */
const PRE_EXPIRY_OFFSET_DAYS = [7, 3] as const;

const OFFSET_TO_TEMPLATE_KIND: Record<
  number,
  MessageTemplateKind
> = {
  7: MessageTemplateKind.EXPIRY_REMINDER_7D,
  3: MessageTemplateKind.EXPIRY_REMINDER_3D,
};

function memberGymWhere(
  gymId: string,
  memberUserId: string,
): Prisma.GymUserWhereInput {
  return {
    gymId,
    userId: memberUserId,
  };
}

@Processor(WHATSAPP_QUEUE)
export class WhatsAppProcessor extends WorkerHost {
  private readonly logger = new Logger(WhatsAppProcessor.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly templates: MessageTemplatesService,
    private readonly whatsapp: WhatsAppApiService,
    private readonly events: EventEmitter2,
    @InjectQueue(WHATSAPP_QUEUE) private readonly whatsappQueue: Queue,
  ) {
    super();
  }

  async process(job: Job<SendTextJob | Record<string, never>>): Promise<void> {
    if (job.name === 'scan-expiry-reminders') {
      await this.scanPreExpiryReminders();
      await this.scanPostExpiryFollowUps();
      return;
    }
    if (job.name !== 'send-text') {
      this.logger.warn(`Unknown job name: ${job.name}`);
      return;
    }
    await this.sendTextJob(job.data as SendTextJob);
  }

  private async sendTextJob(data: SendTextJob): Promise<void> {
    const kindMap: Record<SendTextJob['kind'], MessageTemplateKind> = {
      WELCOME: MessageTemplateKind.WELCOME,
      PAYMENT_CONFIRMATION: MessageTemplateKind.PAYMENT_CONFIRMATION,
      EXPIRY_REMINDER_7D: MessageTemplateKind.EXPIRY_REMINDER_7D,
      EXPIRY_REMINDER_3D: MessageTemplateKind.EXPIRY_REMINDER_3D,
      POST_EXPIRY: MessageTemplateKind.POST_EXPIRY,
    };
    const templateKind = kindMap[data.kind];

    if (!(await this.templates.isTemplateEnabled(data.gymId, templateKind))) {
      this.logger.debug(
        `Skip WhatsApp ${data.kind}: automation disabled for gym ${data.gymId}`,
      );
      return;
    }

    const gym = await this.prisma.gym.findUnique({
      where: { id: data.gymId },
      select: { id: true, name: true },
    });
    if (!gym) {
      return;
    }

    const template = await this.prisma.gymMessageTemplate.findUnique({
      where: {
        gymId_kind: { gymId: data.gymId, kind: templateKind },
      },
    });

    const member = await this.prisma.user.findUnique({
      where: { id: data.memberUserId },
      select: { phone: true, fullName: true },
    });
    if (!member?.phone) {
      return;
    }

    const bodyTemplate =
      template?.overrideBody?.trim() || defaultBodyForKind(templateKind);

    const vars: Record<string, string> = {
      gymName: gym.name,
      memberName: member.fullName?.trim() || 'Member',
      amount: '',
      currency: 'INR',
      expiryDate: '',
      expiredOn: '',
      daysRemaining: '',
    };

    let paymentPlanLabel = 'Gym membership';

    if (data.kind === 'PAYMENT_CONFIRMATION' && data.paymentId) {
      const pay = await this.prisma.payment.findFirst({
        where: {
          id: data.paymentId,
          gymId: data.gymId,
          memberUserId: data.memberUserId,
          status: PaymentStatus.COMPLETED,
        },
        include: {
          memberSubscription: {
            select: {
              gymPlan: { select: { name: true } },
              plan: { select: { name: true } },
            },
          },
        },
      });
      if (!pay) {
        return;
      }
      vars.amount = pay.amountCents.toString();
      vars.currency = pay.currency;
      paymentPlanLabel =
        pay.memberSubscription?.gymPlan?.name?.trim() ||
        pay.memberSubscription?.plan?.name?.trim() ||
        pay.description?.trim() ||
        paymentPlanLabel;
    }

    if (
      data.kind === 'EXPIRY_REMINDER_7D' ||
      data.kind === 'EXPIRY_REMINDER_3D'
    ) {
      const sub = await this.resolveSubscriptionForExpiry(
        data.gymId,
        data.memberUserId,
        data.memberSubscriptionId,
      );
      if (!sub) {
        return;
      }
      const days =
        data.kind === 'EXPIRY_REMINDER_7D'
          ? '7'
          : data.kind === 'EXPIRY_REMINDER_3D'
            ? '3'
            : '';
      vars.expiryDate = sub.endsAt.toISOString().slice(0, 10);
      vars.daysRemaining = days;
    }

    if (data.kind === 'POST_EXPIRY') {
      const sub = data.memberSubscriptionId
        ? await this.prisma.memberSubscription.findFirst({
            where: {
              id: data.memberSubscriptionId,
              gymUser: memberGymWhere(data.gymId, data.memberUserId),
            },
          })
        : null;
      if (!sub) {
        return;
      }
      vars.expiredOn = sub.endsAt.toISOString().slice(0, 10);
      vars.expiryDate = vars.expiredOn;
    }

    const text = interpolate(bodyTemplate, vars);

    if (data.kind === 'WELCOME') {
      const hasCustom = Boolean(template?.overrideBody?.trim());
      if (hasCustom) {
        await this.whatsapp.sendText(member.phone, text);
      } else {
        await this.whatsapp.sendOnboardingTemplate(member.phone, {
          gymName: gym.name,
          fallbackText: text,
        });
      }
      return;
    }

    if (data.kind === 'PAYMENT_CONFIRMATION') {
      await this.whatsapp.sendPaymentConfirmationTemplate(member.phone, {
        memberName: vars.memberName,
        amountFormatted: formatPaymentAmount(
          Number(vars.amount),
          vars.currency,
        ),
        planLabel: paymentPlanLabel,
        fallbackText: text,
      });
      return;
    }

    await this.whatsapp.sendText(member.phone, text);
  }

  private async resolveSubscriptionForExpiry(
    gymId: string,
    memberUserId: string,
    memberSubscriptionId: string | undefined,
  ) {
    if (memberSubscriptionId) {
      return this.prisma.memberSubscription.findFirst({
        where: {
          id: memberSubscriptionId,
          gymUser: memberGymWhere(gymId, memberUserId),
          status: {
            in: [
              MemberSubscriptionStatus.ACTIVE,
              MemberSubscriptionStatus.SCHEDULED,
            ],
          },
        },
      });
    }
    return this.prisma.memberSubscription.findFirst({
      where: {
        gymUser: memberGymWhere(gymId, memberUserId),
        isCurrentSubscription: true,
        status: {
          in: [
            MemberSubscriptionStatus.ACTIVE,
            MemberSubscriptionStatus.SCHEDULED,
            MemberSubscriptionStatus.FROZEN,
          ],
        },
      },
      orderBy: { endsAt: 'desc' },
    });
  }

  private async scanPreExpiryReminders(): Promise<void> {
    const now = new Date();
    for (const offset of PRE_EXPIRY_OFFSET_DAYS) {
      const target = addUtcDaysStart(now, offset);
      const next = addUtcDaysStart(now, offset + 1);

      const subs = await this.prisma.memberSubscription.findMany({
        where: {
          isCurrentSubscription: true,
          status: {
            in: [
              MemberSubscriptionStatus.ACTIVE,
              MemberSubscriptionStatus.SCHEDULED,
              MemberSubscriptionStatus.FROZEN,
            ],
          },
          endsAt: {
            gte: target,
            lt: next,
          },
          gymUser: { role: 'MEMBER', isActive: true },
        },
        select: {
          id: true,
          gymUser: {
            select: {
              id: true,
              gymId: true,
              userId: true,
              user: { select: { fullName: true } },
            },
          },
        },
      });

      const dayKey = target.toISOString().slice(0, 10);
      const jobKind =
        offset === 7 ? 'EXPIRY_REMINDER_7D' : 'EXPIRY_REMINDER_3D';
      const templateKind = OFFSET_TO_TEMPLATE_KIND[offset];
      if (!templateKind) {
        continue;
      }

      const gymIds = [...new Set(subs.map((s) => s.gymUser.gymId))];
      const enabledGyms = await this.gymsWithAutomationEnabled(
        gymIds,
        templateKind,
      );

      for (const s of subs) {
        const gu = s.gymUser;
        if (!enabledGyms.has(gu.gymId)) {
          continue;
        }
        this.events.emit(NOTIFICATION_EVENTS.EXPIRY_ALERT, {
          gymId: gu.gymId,
          memberSubscriptionId: s.id,
          gymUserId: gu.id,
          memberName: gu.user.fullName,
          phase: 'pre',
          daysBeforeExpiry: offset,
        });
        await this.whatsappQueue.add(
          'send-text',
          {
            kind: jobKind,
            gymId: gu.gymId,
            memberUserId: gu.userId,
            memberSubscriptionId: s.id,
          } satisfies SendTextJob,
          {
            jobId: `wa-${jobKind}-${gu.gymId}-${gu.userId}-${offset}-${dayKey}`,
            ...WHATSAPP_SEND_JOB_OPTS,
          },
        );
      }
    }
  }

  /**
   * Memberships whose `endsAt` fell on the previous UTC calendar day — send
   * a post-expiry follow-up once per member/subscription/day.
   */
  private async scanPostExpiryFollowUps(): Promise<void> {
    const now = new Date();
    const yesterdayStart = addUtcDaysStart(now, -1);
    const todayStart = addUtcDaysStart(now, 0);
    const dayKey = yesterdayStart.toISOString().slice(0, 10);

    const subs = await this.prisma.memberSubscription.findMany({
      where: {
        isCurrentSubscription: true,
        status: {
          not: MemberSubscriptionStatus.CANCELED,
        },
        endsAt: {
          gte: yesterdayStart,
          lt: todayStart,
        },
        gymUser: { role: 'MEMBER', isActive: true },
      },
      select: {
        id: true,
        gymUser: {
          select: {
            id: true,
            gymId: true,
            userId: true,
            user: { select: { fullName: true } },
          },
        },
      },
    });

    const enabledGyms = await this.gymsWithAutomationEnabled(
      [...new Set(subs.map((s) => s.gymUser.gymId))],
      MessageTemplateKind.POST_EXPIRY,
    );

    for (const s of subs) {
      const gu = s.gymUser;
      if (!enabledGyms.has(gu.gymId)) {
        continue;
      }
      this.events.emit(NOTIFICATION_EVENTS.EXPIRY_ALERT, {
        gymId: gu.gymId,
        memberSubscriptionId: s.id,
        gymUserId: gu.id,
        memberName: gu.user.fullName,
        phase: 'post',
      });
      await this.whatsappQueue.add(
        'send-text',
        {
          kind: 'POST_EXPIRY',
          gymId: gu.gymId,
          memberUserId: gu.userId,
          memberSubscriptionId: s.id,
        } satisfies SendTextJob,
        {
          jobId: `wa-POST_EXPIRY-${gu.gymId}-${gu.userId}-${s.id}-${dayKey}`,
          ...WHATSAPP_SEND_JOB_OPTS,
        },
      );
    }
  }

  @OnWorkerEvent('failed')
  onFailed(job: Job | undefined, err: Error): void {
    this.logger.error(`Job ${job?.id} failed: ${err.message}`, err.stack);
  }

  private async gymsWithAutomationEnabled(
    gymIds: string[],
    kind: MessageTemplateKind,
  ): Promise<Set<string>> {
    const enabled = new Set<string>();
    const unique = [...new Set(gymIds)];
    await Promise.all(
      unique.map(async (gymId) => {
        if (await this.templates.isTemplateEnabled(gymId, kind)) {
          enabled.add(gymId);
        }
      }),
    );
    return enabled;
  }
}

function addUtcDaysStart(from: Date, days: number): Date {
  return new Date(
    Date.UTC(
      from.getUTCFullYear(),
      from.getUTCMonth(),
      from.getUTCDate() + days,
      0,
      0,
      0,
      0,
    ),
  );
}
