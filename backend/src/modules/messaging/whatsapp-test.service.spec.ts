import { ConfigService } from '@nestjs/config';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { WhatsAppTestMessageType } from './dto/test-whatsapp-message.dto';
import { WhatsAppApiService } from './whatsapp-api.service';
import { WhatsAppTestService } from './whatsapp-test.service';

describe('WhatsAppTestService', () => {
  let service: WhatsAppTestService;
  let gymAccess: { assertCanManageGym: jest.Mock };
  let prisma: {
    gym: { findUnique: jest.Mock };
    gymUser: { findFirst: jest.Mock };
  };
  let whatsapp: {
    sendOnboardingTemplate: jest.Mock;
    sendPaymentConfirmationTemplate: jest.Mock;
    sendText: jest.Mock;
  };
  let config: { get: jest.Mock };

  beforeEach(() => {
    gymAccess = { assertCanManageGym: jest.fn().mockResolvedValue(undefined) };
    prisma = {
      gym: {
        findUnique: jest.fn().mockResolvedValue({ id: 'gym-1', name: 'Zest' }),
      },
      gymUser: { findFirst: jest.fn() },
    };
    whatsapp = {
      sendOnboardingTemplate: jest.fn().mockResolvedValue(undefined),
      sendPaymentConfirmationTemplate: jest.fn().mockResolvedValue(undefined),
      sendText: jest.fn().mockResolvedValue(undefined),
    };
    config = {
      get: jest.fn((key: string) => {
        if (key === 'WHATSAPP_SKIP_SEND') return false;
        if (key === 'WHATSAPP_PROVIDER') return 'meta';
        return undefined;
      }),
    };

    service = new WhatsAppTestService(
      prisma as unknown as PrismaService,
      gymAccess as unknown as GymAccessService,
      whatsapp as unknown as WhatsAppApiService,
      config as unknown as ConfigService,
    );
  });

  it('sends onboarding template to a raw phone', async () => {
    const res = await service.sendTest('actor-1', 'gym-1', {
      type: WhatsAppTestMessageType.ONBOARDING,
      phone: '+918130916940',
      memberName: 'Shivam',
    });

    expect(gymAccess.assertCanManageGym).toHaveBeenCalledWith('actor-1', 'gym-1');
    expect(whatsapp.sendOnboardingTemplate).toHaveBeenCalledWith(
      '+918130916940',
      expect.objectContaining({ gymName: 'Zest' }),
    );
    expect(res).toEqual({
      sent: true,
      skipped: false,
      type: WhatsAppTestMessageType.ONBOARDING,
      to: '918130916940',
      provider: 'meta',
    });
  });

  it('sends payment confirmation with defaults', async () => {
    await service.sendTest('actor-1', 'gym-1', {
      type: WhatsAppTestMessageType.PAYMENT_CONFIRMATION,
      phone: '918130916940',
    });

    expect(whatsapp.sendPaymentConfirmationTemplate).toHaveBeenCalledWith(
      '918130916940',
      expect.objectContaining({
        memberName: 'Test Member',
        amountFormatted: '₹1,500',
        planLabel: 'Gym membership',
      }),
    );
  });

  it('resolves phone from member_id', async () => {
    prisma.gymUser.findFirst.mockResolvedValue({
      user: { phone: '+919999999999', fullName: 'Asha' },
    });

    await service.sendTest('actor-1', 'gym-1', {
      type: WhatsAppTestMessageType.TEXT,
      member_id: 'gu-1',
      message: 'Hello from test',
    });

    expect(whatsapp.sendText).toHaveBeenCalledWith(
      '+919999999999',
      'Hello from test',
    );
  });
});
