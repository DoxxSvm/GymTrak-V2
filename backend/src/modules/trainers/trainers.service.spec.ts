import { GymAccessService } from '../../common/services/gym-access.service';
import { AuditService } from '../audit/audit.service';
import { GymFeaturesService } from '../gym-features/gym-features.service';
import { PrismaService } from '../prisma/prisma.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { TrainersService } from './trainers.service';

describe('TrainersService', () => {
  let service: TrainersService;
  let prisma: {
    gymUser: { update: jest.Mock };
    gymUserPermission: { findMany: jest.Mock };
    $transaction: jest.Mock;
  };
  let gymAccess: {
    assertCanManageGym: jest.Mock;
    assertGymOwnerOrSuperAdmin: jest.Mock;
  };
  let features: { isEnabled: jest.Mock };
  let permissionEngine: { expandEffectivePermissions: jest.Mock };

  beforeEach(() => {
    prisma = {
      gymUser: { update: jest.fn() },
      gymUserPermission: { findMany: jest.fn() },
      $transaction: jest.fn(),
    };
    gymAccess = {
      assertCanManageGym: jest.fn(),
      assertGymOwnerOrSuperAdmin: jest.fn(),
    };
    features = {
      isEnabled: jest.fn().mockResolvedValue(true),
    };
    permissionEngine = {
      expandEffectivePermissions: jest.fn((base) => base),
    };

    service = new TrainersService(
      prisma as unknown as PrismaService,
      gymAccess as unknown as GymAccessService,
      features as unknown as GymFeaturesService,
      permissionEngine as unknown as PermissionEngineService,
      {} as AuditService,
    );
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('returns the expanded trainer profile fields from getBasic', async () => {
    gymAccess.assertCanManageGym.mockResolvedValue(undefined);
    prisma.gymUserPermission.findMany.mockResolvedValue([
      { permission: { code: 'dashboard:access' } },
      { permission: { code: 'payments:access' } },
    ]);
    jest.spyOn(service as never, 'loadTrainerOrThrow').mockResolvedValue({
      id: 'trainer-gym-user-1',
      userId: 'user-1',
      isActive: true,
      dateOfBirth: new Date('2026-05-04T00:00:00.000Z'),
      gender: 'male',
      joinedAt: new Date('2026-04-01T00:00:00.000Z'),
      user: {
        id: 'user-1',
        phone: '9999999999',
        email: 'john@example.com',
        fullName: 'John',
        username: '#9509000',
        passwordHash: 'hashed',
        avatarUrl: '/uploads/images/john.png',
        createdAt: new Date('2026-04-01T00:00:00.000Z'),
      },
      trainerProfile: {
        salaryCents: 5000,
        salaryPeriod: 'MONTHLY',
        contractStartsAt: null,
        contractEndsAt: null,
        experience: '5+ years',
        address: 'At-Rajkot',
        notes: 'Senior trainer',
      },
      trainerExpertise: [{ tag: { name: 'Strength' } }],
      trainerShifts: [
        { id: 'shift-1', dayOfWeek: 1, startTime: '08:00', endTime: '09:00' },
      ],
    } as never);

    const result = await service.getBasic('owner-1', 'gym-1', 'trainer-1');

    expect(result.user.avatarUrl).toBe('/uploads/images/john.png');
    expect(result.profile).toMatchObject({
      dateOfBirth: new Date('2026-05-04T00:00:00.000Z'),
      gender: 'male',
      experience: '5+ years',
      address: 'At-Rajkot',
      salaryCents: 5000,
    });
    expect(result.permissions).toMatchObject({
      dashboard: true,
      payments: true,
      members: false,
      admin: false,
      show_dashboard: true,
      show_payments: true,
      show_payment_in_details: true,
      add_clients: false,
      add_trainer: false,
    });
    expect(permissionEngine.expandEffectivePermissions).toHaveBeenCalled();
  });

  it('soft deletes only the gym membership', async () => {
    gymAccess.assertGymOwnerOrSuperAdmin.mockResolvedValue(undefined);
    jest.spyOn(service as never, 'loadTrainerOrThrow').mockResolvedValue({
      id: 'trainer-1',
    } as never);
    prisma.gymUser.update.mockResolvedValue({ id: 'trainer-1' });

    const result = await service.softDelete('owner-1', 'gym-1', 'trainer-1');

    expect(prisma.gymUser.update).toHaveBeenCalledWith({
      where: { id: 'trainer-1' },
      data: { isActive: false },
    });
    expect(result).toEqual({ success: true });
  });

  it('maps requested permission labels onto the existing flags', async () => {
    gymAccess.assertGymOwnerOrSuperAdmin.mockResolvedValue(undefined);
    jest.spyOn(service as never, 'loadTrainerOrThrow').mockResolvedValue({
      id: 'trainer-1',
    } as never);
    jest.spyOn(service, 'getBasic').mockResolvedValue({ ok: true } as never);
    const tx = {
      permission: {
        findMany: jest.fn().mockResolvedValue([
          { id: 'p-dashboard', code: 'dashboard:access' },
          { id: 'p-payments', code: 'payments:access' },
          { id: 'p-members', code: 'members:manage' },
          { id: 'p-admin', code: 'admin:access' },
        ]),
      },
      gymUserPermission: {
        deleteMany: jest.fn().mockResolvedValue(undefined),
        createMany: jest.fn().mockResolvedValue(undefined),
      },
    };
    prisma.$transaction.mockImplementation(async (cb) =>
      cb(tx as Parameters<typeof cb>[0]),
    );

    await service.updatePermissionsCompat('owner-1', 'gym-1', 'trainer-1', {
      add_clients: true,
      show_dashboard: true,
      show_payments: false,
      show_payment_in_details: true,
      add_trainer: true,
    });

    expect(tx.gymUserPermission.createMany).toHaveBeenCalledWith({
      data: [
        { gymUserId: 'trainer-1', permissionId: 'p-dashboard' },
        { gymUserId: 'trainer-1', permissionId: 'p-payments' },
        { gymUserId: 'trainer-1', permissionId: 'p-members' },
        { gymUserId: 'trainer-1', permissionId: 'p-admin' },
      ],
    });
  });

  it('returns permission status map for dynamic patch update', async () => {
    gymAccess.assertCanManageGym.mockResolvedValue(undefined);
    jest.spyOn(service as never, 'loadTrainerOrThrow').mockResolvedValue({
      id: 'trainer-1',
    } as never);
    const tx = {
      permission: {
        findMany: jest.fn().mockResolvedValue([
          { id: 'p-dashboard', code: 'dashboard:access' },
          { id: 'p-payments', code: 'payments:access' },
          { id: 'p-members', code: 'members:manage' },
          { id: 'p-admin', code: 'admin:access' },
        ]),
      },
      gymUserPermission: {
        deleteMany: jest.fn().mockResolvedValue(undefined),
        createMany: jest.fn().mockResolvedValue(undefined),
        findMany: jest
          .fn()
          .mockResolvedValue([
            { permission: { code: 'dashboard:access' } },
            { permission: { code: 'members:manage' } },
          ]),
      },
    };
    prisma.$transaction.mockImplementation(async (cb) =>
      cb(tx as Parameters<typeof cb>[0]),
    );
    prisma.gymUserPermission.findMany.mockResolvedValue([
      { permission: { code: 'dashboard:access' } },
      { permission: { code: 'members:manage' } },
    ]);

    const result = await service.updatePermissions(
      'owner-1',
      'gym-1',
      'trainer-1',
      {
        dashboard: true,
        members: true,
      },
    );

    expect(result).toEqual({
      gymUserId: 'trainer-1',
      gymId: 'gym-1',
      permissions: {
        dashboard: true,
        payments: false,
        member: true,
        admin: false,
      },
    });
  });

  it('rejects partial explicit credentials on create', async () => {
    await expect(
      service.create('owner-1', {
        gymId: 'gym-1',
        phone: '9999999999',
        fullName: 'John',
        permissions: [],
        username: '#9509000',
      }),
    ).rejects.toThrow('Provide both username and password, or neither');
  });
});
