import { EventEmitter2 } from '@nestjs/event-emitter';
import { GymRole, MemberSubscriptionStatus } from '@prisma/client';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { GymAccessService } from '../../common/services/gym-access.service';
import { WhatsAppAutomationService } from '../messaging/whatsapp-automation.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import { SubscriptionsService } from './subscriptions.service';

describe('SubscriptionsService', () => {
  let service: SubscriptionsService;
  let prisma: {
    gymUser: { findUnique: jest.Mock };
    $transaction: jest.Mock;
  };
  let gymAccess: { assertCanManageGym: jest.Mock };
  let events: { emit: jest.Mock };
  let permissions: { assertOwnerOrPermission: jest.Mock };

  beforeEach(() => {
    prisma = {
      gymUser: { findUnique: jest.fn() },
      $transaction: jest.fn(),
    };
    gymAccess = {
      assertCanManageGym: jest.fn(),
    };
    events = {
      emit: jest.fn(),
    };
    permissions = {
      assertOwnerOrPermission: jest.fn(),
    };

    service = new SubscriptionsService(
      prisma as unknown as PrismaService,
      gymAccess as unknown as GymAccessService,
      {} as WhatsAppAutomationService,
      events as unknown as EventEmitter2,
      permissions as unknown as PermissionEngineService,
    );
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('checks members permission for compat subscription creation', async () => {
    prisma.gymUser.findUnique.mockResolvedValue({
      id: 'member-1',
      gymId: 'gym-1',
      role: GymRole.MEMBER,
    });
    permissions.assertOwnerOrPermission.mockResolvedValue(undefined);
    jest.spyOn(service, 'createMemberSubscription').mockResolvedValue('sub-1');

    const result = await service.createSubscriptionCompat('actor-1', {
      member_id: 'member-1',
      plan_id: 'plan-1',
      price: 120,
      start_date: '2026-04-02T00:00:00.000Z',
      duration_months: 2,
      discount: 20,
    });

    expect(permissions.assertOwnerOrPermission).toHaveBeenCalledWith(
      'actor-1',
      'gym-1',
      PERMISSION_CODES.MEMBERS,
    );
    expect(service.createMemberSubscription).toHaveBeenCalledWith(
      'actor-1',
      'gym-1',
      'member-1',
      expect.objectContaining({
        planId: 'plan-1',
        startsAt: '2026-04-02T00:00:00.000Z',
        endsAt: '2026-06-02T00:00:00.000Z',
        priceCents: 10000,
        currency: 'INR',
      }),
    );
    expect(result).toEqual({ success: true, subscription_id: 'sub-1' });
  });

  it('stops compat creation when permission check fails', async () => {
    prisma.gymUser.findUnique.mockResolvedValue({
      id: 'member-1',
      gymId: 'gym-1',
      role: GymRole.MEMBER,
    });
    permissions.assertOwnerOrPermission.mockRejectedValue(
      new Error('Insufficient permissions'),
    );
    const createSpy = jest.spyOn(service, 'createMemberSubscription');

    await expect(
      service.createSubscriptionCompat('actor-1', {
        member_id: 'member-1',
        plan_id: 'plan-1',
        price: 120,
        start_date: '2026-04-02T00:00:00.000Z',
      }),
    ).rejects.toThrow('Insufficient permissions');

    expect(createSpy).not.toHaveBeenCalled();
  });

  it('cancels an active subscription and syncs membership', async () => {
    jest.useFakeTimers().setSystemTime(new Date('2026-04-02T12:00:00.000Z'));
    gymAccess.assertCanManageGym.mockResolvedValue(undefined);

    const tx = {
      memberSubscription: {
        findFirst: jest.fn().mockResolvedValue({
          id: 'sub-1',
          gymUserId: 'gym-user-1',
          status: MemberSubscriptionStatus.ACTIVE,
          startsAt: new Date('2026-04-01T00:00:00.000Z'),
          endsAt: new Date('2026-04-20T00:00:00.000Z'),
        }),
        update: jest.fn().mockResolvedValue(undefined),
      },
      $executeRaw: jest.fn().mockResolvedValue(undefined),
    };
    prisma.$transaction.mockImplementation(async (cb) => cb(tx));
    jest.spyOn(service, 'syncMembershipEndsAt').mockResolvedValue(undefined);
    jest
      .spyOn(service, 'getDetail')
      .mockResolvedValue({ id: 'sub-1', status: 'CANCELED' } as never);

    const result = await service.cancel('actor-1', 'gym-1', 'sub-1', {
      reason: 'Requested by member',
    });

    expect(tx.memberSubscription.update).toHaveBeenCalledWith({
      where: { id: 'sub-1' },
      data: {
        status: MemberSubscriptionStatus.CANCELED,
        endsAt: new Date('2026-04-02T12:00:00.000Z'),
        freezeStartedAt: null,
        freezeEndsAt: null,
      },
    });
    expect(service.syncMembershipEndsAt).toHaveBeenCalledWith(tx, 'gym-user-1');
    expect(result).toEqual({ id: 'sub-1', status: 'CANCELED' });
  });

  it('rejects cancel for ended subscriptions', async () => {
    jest.useFakeTimers().setSystemTime(new Date('2026-04-20T12:00:00.000Z'));
    gymAccess.assertCanManageGym.mockResolvedValue(undefined);

    const tx = {
      memberSubscription: {
        findFirst: jest.fn().mockResolvedValue({
          id: 'sub-1',
          gymUserId: 'gym-user-1',
          status: MemberSubscriptionStatus.ACTIVE,
          startsAt: new Date('2026-04-01T00:00:00.000Z'),
          endsAt: new Date('2026-04-10T00:00:00.000Z'),
        }),
        update: jest.fn(),
      },
      $executeRaw: jest.fn(),
    };
    prisma.$transaction.mockImplementation(async (cb) => cb(tx));

    await expect(
      service.cancel('actor-1', 'gym-1', 'sub-1', {}),
    ).rejects.toThrow('Cannot cancel an ended subscription');
    expect(tx.memberSubscription.update).not.toHaveBeenCalled();
  });
});
