import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  buildOnboardingTemplateBody,
  buildPaymentConfirmationTemplateBody,
  DEFAULT_ONBOARDING_HEADER_IMAGE_URL,
  DEFAULT_ONBOARDING_TEMPLATE_NAME,
  DEFAULT_PAYMENT_TEMPLATE_NAME,
  META_WHATSAPP_API_VERSION,
} from './whatsapp-templates';

/**
 * Outbound WhatsApp: Meta Cloud API or Twilio WhatsApp API.
 * WHATSAPP_SKIP_SEND=true logs only (safe for local dev).
 *
 * Meta onboarding uses `/marketing_messages`; payment confirmation uses `/messages`.
 * Expiry reminders still use free-form text until Meta templates are added.
 */
@Injectable()
export class WhatsAppApiService {
  private readonly logger = new Logger(WhatsAppApiService.name);

  constructor(private readonly config: ConfigService) {}

  async sendText(toE164: string, body: string): Promise<void> {
    const digits = this.normalizeDigits(toE164);
    if (!digits) {
      return;
    }
    if (this.shouldSkip()) {
      this.logger.log(`[WhatsApp skip] -> ${digits}: ${body}`);
      return;
    }

    const provider = this.provider();
    if (provider === 'twilio') {
      await this.sendViaTwilio(digits, body);
      return;
    }

    await this.sendViaMetaText(digits, body);
  }

  async sendOnboardingTemplate(
    toE164: string,
    params: { gymName: string; fallbackText: string },
  ): Promise<void> {
    const digits = this.normalizeDigits(toE164);
    if (!digits) {
      return;
    }

    const provider = this.provider();
    if (provider !== 'meta') {
      await this.sendText(toE164, params.fallbackText);
      return;
    }

    const headerImageUrl =
      this.config.get<string>('WHATSAPP_ONBOARDING_HEADER_IMAGE_URL')?.trim() ||
      DEFAULT_ONBOARDING_HEADER_IMAGE_URL;
    const templateName =
      this.config.get<string>('WHATSAPP_TEMPLATE_ONBOARDING_NAME')?.trim() ||
      DEFAULT_ONBOARDING_TEMPLATE_NAME;

    const body = buildOnboardingTemplateBody(
      digits,
      params.gymName,
      headerImageUrl,
      templateName,
    );

    if (this.shouldSkip()) {
      this.logger.log(
        `[WhatsApp skip] onboarding template -> ${digits}: ${JSON.stringify(body)}`,
      );
      return;
    }

    await this.postMeta('marketing_messages', body);
  }

  async sendPaymentConfirmationTemplate(
    toE164: string,
    params: {
      memberName: string;
      amountFormatted: string;
      planLabel: string;
      fallbackText: string;
    },
  ): Promise<void> {
    const digits = this.normalizeDigits(toE164);
    if (!digits) {
      return;
    }

    const provider = this.provider();
    if (provider !== 'meta') {
      await this.sendText(toE164, params.fallbackText);
      return;
    }

    const templateName =
      this.config.get<string>('WHATSAPP_TEMPLATE_PAYMENT_NAME')?.trim() ||
      DEFAULT_PAYMENT_TEMPLATE_NAME;

    const body = buildPaymentConfirmationTemplateBody(
      digits,
      params.memberName,
      params.amountFormatted,
      params.planLabel,
      templateName,
    );

    if (this.shouldSkip()) {
      this.logger.log(
        `[WhatsApp skip] payment template -> ${digits}: ${JSON.stringify(body)}`,
      );
      return;
    }

    await this.postMeta('messages', body);
  }

  private provider(): string {
    return (
      this.config.get<string>('WHATSAPP_PROVIDER') ?? 'meta'
    ).toLowerCase();
  }

  private shouldSkip(): boolean {
    return (
      this.config.get<boolean>('WHATSAPP_SKIP_SEND') === true ||
      this.config.get<string>('WHATSAPP_SKIP_SEND') === 'true'
    );
  }

  private normalizeDigits(toE164: string): string | null {
    const digits = toE164.replace(/\D/g, '');
    if (!digits) {
      this.logger.warn('WhatsApp: empty phone, skipping');
      return null;
    }
    return digits;
  }

  private metaConfig(): { token: string; phoneId: string } | null {
    const token = this.config.get<string>('WHATSAPP_ACCESS_TOKEN');
    const phoneId = this.config.get<string>('WHATSAPP_PHONE_NUMBER_ID');
    if (!token || !phoneId) {
      this.logger.warn(
        'Meta WhatsApp not configured (WHATSAPP_ACCESS_TOKEN / WHATSAPP_PHONE_NUMBER_ID); skipping send',
      );
      return null;
    }
    return { token, phoneId };
  }

  private async postMeta(
    endpoint: 'messages' | 'marketing_messages',
    body: Record<string, unknown>,
  ): Promise<void> {
    const cfg = this.metaConfig();
    if (!cfg) {
      return;
    }

    const url = `https://graph.facebook.com/${META_WHATSAPP_API_VERSION}/${cfg.phoneId}/${endpoint}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${cfg.token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      const errText = await res.text();
      this.logger.error(`Meta WhatsApp API ${res.status}: ${errText}`);
      throw new Error(`WhatsApp send failed: ${res.status}`);
    }
  }

  private async sendViaMetaText(digits: string, body: string): Promise<void> {
    await this.postMeta('messages', {
      messaging_product: 'whatsapp',
      to: digits,
      type: 'text',
      text: { body },
    });
  }

  private async sendViaTwilio(digits: string, body: string): Promise<void> {
    const accountSid = this.config.get<string>('TWILIO_ACCOUNT_SID');
    const authToken = this.config.get<string>('TWILIO_AUTH_TOKEN');
    const from = this.config.get<string>('TWILIO_WHATSAPP_FROM');
    if (!accountSid || !authToken || !from?.trim()) {
      this.logger.warn(
        'Twilio WhatsApp not configured (TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN / TWILIO_WHATSAPP_FROM); skipping send',
      );
      return;
    }

    const to = `whatsapp:+${digits}`;

    const fromNorm = from.trim().startsWith('whatsapp:')
      ? from.trim()
      : `whatsapp:${from.trim()}`;

    const res = await fetch(
      `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Messages.json`,
      {
        method: 'POST',
        headers: {
          Authorization: `Basic ${Buffer.from(`${accountSid}:${authToken}`).toString('base64')}`,
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
          From: fromNorm,
          To: to,
          Body: body,
        }),
      },
    );

    if (!res.ok) {
      const errText = await res.text();
      this.logger.error(`Twilio WhatsApp API ${res.status}: ${errText}`);
      throw new Error(`Twilio WhatsApp send failed: ${res.status}`);
    }
  }
}
