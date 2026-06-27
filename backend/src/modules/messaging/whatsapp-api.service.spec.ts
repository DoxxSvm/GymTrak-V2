import { ConfigService } from '@nestjs/config';
import { WhatsAppApiService } from './whatsapp-api.service';

describe('WhatsAppApiService', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  function makeService(
    config: Record<string, string | boolean | undefined>,
  ): WhatsAppApiService {
    const configService = {
      get: (key: string) => config[key],
    } as unknown as ConfigService;
    return new WhatsAppApiService(configService);
  }

  it('posts onboarding template to marketing_messages', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      text: async () => '',
    });
    global.fetch = fetchMock;

    const service = makeService({
      WHATSAPP_PROVIDER: 'meta',
      WHATSAPP_SKIP_SEND: false,
      WHATSAPP_ACCESS_TOKEN: 'token',
      WHATSAPP_PHONE_NUMBER_ID: '1098408740030503',
    });

    await service.sendOnboardingTemplate('+918130916940', {
      gymName: 'Zest',
      fallbackText: 'fallback',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      'https://graph.facebook.com/v25.0/1098408740030503/marketing_messages',
    );
    const body = JSON.parse(init.body as string);
    expect(body.template.name).toBe('onboarding');
    expect(body.to).toBe('918130916940');
    expect(body.template.components[1].parameters[0].text).toBe('Zest');
  });

  it('posts payment template to messages', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      text: async () => '',
    });
    global.fetch = fetchMock;

    const service = makeService({
      WHATSAPP_PROVIDER: 'meta',
      WHATSAPP_SKIP_SEND: false,
      WHATSAPP_ACCESS_TOKEN: 'token',
      WHATSAPP_PHONE_NUMBER_ID: '1098408740030503',
    });

    await service.sendPaymentConfirmationTemplate('+918130916940', {
      memberName: 'Shivam',
      amountFormatted: '₹1,500',
      planLabel: 'GymTrak monthly membership',
      fallbackText: 'fallback',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      'https://graph.facebook.com/v25.0/1098408740030503/messages',
    );
    const body = JSON.parse(init.body as string);
    expect(body.template.name).toBe('payment_confirmation_gt');
    expect(body.template.language.code).toBe('en_US');
    expect(body.template.components[0].parameters).toEqual([
      { type: 'text', text: 'Shivam' },
      { type: 'text', text: '₹1,500' },
      { type: 'text', text: 'GymTrak monthly membership' },
    ]);
  });

  it('skips HTTP when WHATSAPP_SKIP_SEND is true', async () => {
    const fetchMock = jest.fn();
    global.fetch = fetchMock;

    const service = makeService({
      WHATSAPP_PROVIDER: 'meta',
      WHATSAPP_SKIP_SEND: true,
      WHATSAPP_ACCESS_TOKEN: 'token',
      WHATSAPP_PHONE_NUMBER_ID: '1098408740030503',
    });

    await service.sendOnboardingTemplate('+918130916940', {
      gymName: 'Zest',
      fallbackText: 'fallback',
    });

    expect(fetchMock).not.toHaveBeenCalled();
  });
});
