import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

/**
 * Outbound WhatsApp: Meta Cloud API or Twilio WhatsApp API.
 * WHATSAPP_SKIP_SEND=true logs only (safe for local dev).
 */
@Injectable()
export class WhatsAppApiService {
  private readonly logger = new Logger(WhatsAppApiService.name);

  constructor(private readonly config: ConfigService) {}

  async sendText(toE164: string, body: string): Promise<void> {
    const skip =
      this.config.get<boolean>('WHATSAPP_SKIP_SEND') === true ||
      this.config.get<string>('WHATSAPP_SKIP_SEND') === 'true';
    const digits = toE164.replace(/\D/g, '');
    if (!digits) {
      this.logger.warn('WhatsApp: empty phone, skipping');
      return;
    }
    if (skip) {
      this.logger.log(`[WhatsApp skip] -> ${digits}: ${body}`);
      return;
    }

    const provider = (
      this.config.get<string>('WHATSAPP_PROVIDER') ?? 'meta'
    ).toLowerCase();

    if (provider === 'twilio') {
      await this.sendViaTwilio(digits, body);
      return;
    }

    await this.sendViaMeta(digits, body);
  }

  private async sendViaMeta(digits: string, body: string): Promise<void> {
    const token = this.config.get<string>('WHATSAPP_ACCESS_TOKEN');
    const phoneId = this.config.get<string>('WHATSAPP_PHONE_NUMBER_ID');
    if (!token || !phoneId) {
      this.logger.warn(
        'Meta WhatsApp not configured (WHATSAPP_ACCESS_TOKEN / WHATSAPP_PHONE_NUMBER_ID); skipping send',
      );
      return;
    }

    const url = `https://graph.facebook.com/v21.0/${phoneId}/messages`;
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        messaging_product: 'whatsapp',
        to: digits,
        type: 'text',
        text: { body },
      }),
    });
    if (!res.ok) {
      const errText = await res.text();
      this.logger.error(`Meta WhatsApp API ${res.status}: ${errText}`);
      throw new Error(`WhatsApp send failed: ${res.status}`);
    }
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
