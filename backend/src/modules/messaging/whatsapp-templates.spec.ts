import {
  buildOnboardingTemplateBody,
  buildPaymentConfirmationTemplateBody,
  formatPaymentAmount,
} from './whatsapp-templates';

describe('whatsapp-templates', () => {
  describe('formatPaymentAmount', () => {
    it('formats INR without decimals', () => {
      expect(formatPaymentAmount(150_000, 'INR')).toBe('₹1,500');
    });

    it('formats non-INR with currency prefix', () => {
      expect(formatPaymentAmount(1999, 'USD')).toBe('USD 19.99');
    });
  });

  describe('buildOnboardingTemplateBody', () => {
    it('builds marketing onboarding payload', () => {
      const body = buildOnboardingTemplateBody(
        '918130916940',
        'Zest',
        'https://example.com/header.jpg',
        'onboarding',
      );

      expect(body).toEqual({
        messaging_product: 'whatsapp',
        to: '918130916940',
        type: 'template',
        template: {
          name: 'onboarding',
          language: { code: 'en' },
          components: [
            {
              type: 'header',
              parameters: [
                {
                  type: 'image',
                  image: { link: 'https://example.com/header.jpg' },
                },
              ],
            },
            {
              type: 'body',
              parameters: [
                {
                  type: 'text',
                  parameter_name: 'text',
                  text: 'Zest',
                },
              ],
            },
          ],
        },
      });
    });
  });

  describe('buildPaymentConfirmationTemplateBody', () => {
    it('builds payment confirmation payload', () => {
      const body = buildPaymentConfirmationTemplateBody(
        '918130916940',
        'Shivam',
        '₹1,500',
        'GymTrak monthly membership',
        'payment_confirmation_gt',
      );

      expect(body).toEqual({
        messaging_product: 'whatsapp',
        to: '918130916940',
        type: 'template',
        template: {
          name: 'payment_confirmation_gt',
          language: { code: 'en_US' },
          components: [
            {
              type: 'body',
              parameters: [
                { type: 'text', text: 'Shivam' },
                { type: 'text', text: '₹1,500' },
                { type: 'text', text: 'GymTrak monthly membership' },
              ],
            },
          ],
        },
      });
    });
  });
});
