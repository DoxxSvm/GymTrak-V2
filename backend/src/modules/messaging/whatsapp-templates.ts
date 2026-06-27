export const META_WHATSAPP_API_VERSION = 'v25.0';

export const DEFAULT_ONBOARDING_HEADER_IMAGE_URL =
  'https://i.ibb.co/0ySmzmQV/grok-image-42077230-49a0-4e5f-ab07-5dbf30622309.jpg';

export const DEFAULT_ONBOARDING_TEMPLATE_NAME = 'onboarding';
export const DEFAULT_PAYMENT_TEMPLATE_NAME = 'payment_confirmation_gt';

export function formatPaymentAmount(
  amountCents: number,
  currency: string,
): string {
  const amount = amountCents / 100;
  if (currency === 'INR') {
    return `₹${amount.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  }
  return `${currency} ${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export function buildOnboardingTemplateBody(
  toDigits: string,
  gymName: string,
  headerImageUrl: string,
  templateName: string,
): Record<string, unknown> {
  return {
    messaging_product: 'whatsapp',
    to: toDigits,
    type: 'template',
    template: {
      name: templateName,
      language: { code: 'en' },
      components: [
        {
          type: 'header',
          parameters: [
            {
              type: 'image',
              image: { link: headerImageUrl },
            },
          ],
        },
        {
          type: 'body',
          parameters: [
            {
              type: 'text',
              parameter_name: 'text',
              text: gymName,
            },
          ],
        },
      ],
    },
  };
}

export function buildPaymentConfirmationTemplateBody(
  toDigits: string,
  memberName: string,
  amountFormatted: string,
  planLabel: string,
  templateName: string,
): Record<string, unknown> {
  return {
    messaging_product: 'whatsapp',
    to: toDigits,
    type: 'template',
    template: {
      name: templateName,
      language: { code: 'en_US' },
      components: [
        {
          type: 'body',
          parameters: [
            { type: 'text', text: memberName },
            { type: 'text', text: amountFormatted },
            { type: 'text', text: planLabel },
          ],
        },
      ],
    },
  };
}
