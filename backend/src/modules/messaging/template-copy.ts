import { MessageTemplateKind } from '@prisma/client';

const DEFAULTS: Record<MessageTemplateKind, string> = {
  [MessageTemplateKind.WELCOME]:
    'Hi {{memberName}}, welcome to {{gymName}}! We are glad to have you.',
  [MessageTemplateKind.EXPIRY_REMINDER]:
    'Hi {{memberName}}, your membership at {{gymName}} expires on {{expiryDate}}. Renew soon to keep training!',
  [MessageTemplateKind.EXPIRY_REMINDER_7D]:
    'Hi {{memberName}}, reminder: your membership at {{gymName}} expires on {{expiryDate}} (in {{daysRemaining}} days). Renew soon!',
  [MessageTemplateKind.EXPIRY_REMINDER_3D]:
    'Hi {{memberName}}, your membership at {{gymName}} expires on {{expiryDate}} (in {{daysRemaining}} days). Renew soon to keep training!',
  [MessageTemplateKind.POST_EXPIRY]:
    'Hi {{memberName}}, your membership at {{gymName}} ended on {{expiredOn}}. We would love to have you back — reply to renew.',
  [MessageTemplateKind.PAYMENT_CONFIRMATION]:
    'Hi {{memberName}}, we received {{amount}} {{currency}} at {{gymName}}. Thank you!',
};

export function defaultBodyForKind(kind: MessageTemplateKind): string {
  return DEFAULTS[kind];
}

export function interpolate(
  template: string,
  vars: Record<string, string>,
): string {
  let out = template;
  for (const [k, v] of Object.entries(vars)) {
    out = out.split(`{{${k}}}`).join(v);
  }
  return out;
}
