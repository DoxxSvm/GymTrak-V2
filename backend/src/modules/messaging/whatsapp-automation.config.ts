import { MessageTemplateKind } from '@prisma/client';

/** Stable ids for mobile `Whatsapp Automation` screen (order matches UI). */
export type WhatsAppAutomationTemplateId =
  | 'onboarding_welcome'
  | 'expire_7_days'
  | 'expire_3_days'
  | 'expired_reminder'
  | 'payment_received';

export type WhatsAppAutomationTemplateMeta = {
  id: WhatsAppAutomationTemplateId;
  kind: MessageTemplateKind;
  title: string;
  description: string;
  /** When the backend automatically enqueues WhatsApp (if `enabled` is true). */
  autoTrigger:
    | 'member_joined'
    | 'subscription_ends_in_7_days'
    | 'subscription_ends_in_3_days'
    | 'subscription_expired'
    | 'payment_completed';
  /** Only onboarding welcome exposes editable message text in the app. */
  supportsCustomMessage: boolean;
};

export const WHATSAPP_AUTOMATION_TEMPLATES: WhatsAppAutomationTemplateMeta[] = [
  {
    id: 'onboarding_welcome',
    kind: MessageTemplateKind.WELCOME,
    title: 'Onboarding Welcome',
    description: 'Sent when a new member joins',
    autoTrigger: 'member_joined',
    supportsCustomMessage: true,
  },
  {
    id: 'expire_7_days',
    kind: MessageTemplateKind.EXPIRY_REMINDER_7D,
    title: 'Expire 7 Days',
    description: 'Reminder 7 days before plan expires',
    autoTrigger: 'subscription_ends_in_7_days',
    supportsCustomMessage: false,
  },
  {
    id: 'expire_3_days',
    kind: MessageTemplateKind.EXPIRY_REMINDER_3D,
    title: 'Expire 3 Days',
    description: 'Reminder 3 days before plan expires',
    autoTrigger: 'subscription_ends_in_3_days',
    supportsCustomMessage: false,
  },
  {
    id: 'expired_reminder',
    kind: MessageTemplateKind.POST_EXPIRY,
    title: 'Expired Reminder',
    description: 'Sent after plan has expired',
    autoTrigger: 'subscription_expired',
    supportsCustomMessage: false,
  },
  {
    id: 'payment_received',
    kind: MessageTemplateKind.PAYMENT_CONFIRMATION,
    title: 'Payment Received',
    description: 'Confirmation after payment',
    autoTrigger: 'payment_completed',
    supportsCustomMessage: false,
  },
];

const ID_BY_KIND = new Map(
  WHATSAPP_AUTOMATION_TEMPLATES.map((t) => [t.kind, t.id]),
);

export function automationIdForKind(
  kind: MessageTemplateKind,
): WhatsAppAutomationTemplateId | undefined {
  return ID_BY_KIND.get(kind);
}

export function automationMetaById(
  id: string,
): WhatsAppAutomationTemplateMeta | undefined {
  return WHATSAPP_AUTOMATION_TEMPLATES.find((t) => t.id === id);
}
