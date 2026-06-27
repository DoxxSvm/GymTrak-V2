import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { GymRole, MessageTemplateKind } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import {
  TestWhatsAppMessageDto,
  WhatsAppTestMessageType,
} from './dto/test-whatsapp-message.dto';
import { defaultBodyForKind, interpolate } from './template-copy';
import { WhatsAppApiService } from './whatsapp-api.service';
import { formatPaymentAmount } from './whatsapp-templates';

@Injectable()
export class WhatsAppTestService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly whatsapp: WhatsAppApiService,
    private readonly config: ConfigService,
  ) {}

  async sendTest(
    actorUserId: string,
    gymId: string,
    body: TestWhatsAppMessageDto,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    if (!body.phone?.trim() && !body.member_id?.trim()) {
      throw new BadRequestException('phone or member_id is required');
    }

    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true, name: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const resolved = await this.resolveRecipient(gymId, body);
    const skipped = this.isSkipSend();

    try {
      if (body.type === WhatsAppTestMessageType.ONBOARDING) {
        const fallbackText = interpolate(
          defaultBodyForKind(MessageTemplateKind.WELCOME),
          {
            memberName: resolved.memberName,
            gymName: gym.name,
          },
        );
        await this.whatsapp.sendOnboardingTemplate(resolved.phone, {
          gymName: gym.name,
          fallbackText,
        });
      } else if (body.type === WhatsAppTestMessageType.PAYMENT_CONFIRMATION) {
        const amountCents = body.amountCents ?? 150_000;
        const currency = body.currency?.trim() || 'INR';
        const planLabel = body.planLabel?.trim() || 'Gym membership';
        const amountFormatted = formatPaymentAmount(amountCents, currency);
        const fallbackText = interpolate(
          defaultBodyForKind(MessageTemplateKind.PAYMENT_CONFIRMATION),
          {
            memberName: resolved.memberName,
            gymName: gym.name,
            amount: amountCents.toString(),
            currency,
          },
        );
        await this.whatsapp.sendPaymentConfirmationTemplate(resolved.phone, {
          memberName: resolved.memberName,
          amountFormatted,
          planLabel,
          fallbackText,
        });
      } else {
        const message =
          body.message?.trim() ||
          interpolate(
            defaultBodyForKind(MessageTemplateKind.EXPIRY_REMINDER_7D),
            {
              memberName: resolved.memberName,
              gymName: gym.name,
              expiryDate: new Date().toISOString().slice(0, 10),
              daysRemaining: '7',
            },
          );
        await this.whatsapp.sendText(resolved.phone, message);
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : 'WhatsApp send failed';
      throw new BadRequestException(message);
    }

    const digits = resolved.phone.replace(/\D/g, '');
    return {
      sent: !skipped,
      skipped,
      type: body.type,
      to: digits,
      provider: (this.config.get<string>('WHATSAPP_PROVIDER') ?? 'meta').toLowerCase(),
    };
  }

  private isSkipSend(): boolean {
    return (
      this.config.get<boolean>('WHATSAPP_SKIP_SEND') === true ||
      this.config.get<string>('WHATSAPP_SKIP_SEND') === 'true'
    );
  }

  private async resolveRecipient(
    gymId: string,
    body: TestWhatsAppMessageDto,
  ): Promise<{ phone: string; memberName: string }> {
    if (body.member_id?.trim()) {
      const member = await this.prisma.gymUser.findFirst({
        where: {
          id: body.member_id.trim(),
          gymId,
          role: GymRole.MEMBER,
        },
        select: {
          user: { select: { phone: true, fullName: true } },
        },
      });
      if (!member) {
        throw new BadRequestException('Member not found');
      }
      if (!member.user.phone?.trim()) {
        throw new BadRequestException('Member has no phone number');
      }
      return {
        phone: member.user.phone,
        memberName:
          body.memberName?.trim() ||
          member.user.fullName?.trim() ||
          'Member',
      };
    }

    const phone = body.phone?.trim();
    if (!phone) {
      throw new BadRequestException('phone is required');
    }

    return {
      phone,
      memberName: body.memberName?.trim() || 'Test Member',
    };
  }
}
