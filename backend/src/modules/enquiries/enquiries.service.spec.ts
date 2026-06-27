import { EventEmitter2 } from '@nestjs/event-emitter';
import { EnquiryStatus } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { MembersService } from '../members/members.service';
import { PrismaService } from '../prisma/prisma.service';
import { SaasEntitlementsService } from '../saas/saas-entitlements.service';
import { EnquiriesService } from './enquiries.service';

describe('EnquiriesService', () => {
  let service: EnquiriesService;
  let prisma: {
    enquiry: {
      create: jest.Mock;
      findFirst: jest.Mock;
      findMany: jest.Mock;
      findUnique: jest.Mock;
      update: jest.Mock;
      count: jest.Mock;
    };
  };
  let gymAccess: { assertCanManageGym: jest.Mock };
  let members: { create: jest.Mock };
  let saas: { assertLeads: jest.Mock };
  let events: { emit: jest.Mock };

  beforeEach(() => {
    prisma = {
      enquiry: {
        create: jest.fn(),
        findFirst: jest.fn(),
        findMany: jest.fn(),
        findUnique: jest.fn(),
        update: jest.fn(),
        count: jest.fn(),
      },
    };
    gymAccess = {
      assertCanManageGym: jest.fn(),
    };
    members = {
      create: jest.fn(),
    };
    saas = {
      assertLeads: jest.fn(),
    };
    events = {
      emit: jest.fn(),
    };

    service = new EnquiriesService(
      prisma as unknown as PrismaService,
      gymAccess as unknown as GymAccessService,
      members as unknown as MembersService,
      saas as unknown as SaasEntitlementsService,
      events as unknown as EventEmitter2,
    );
  });

  it('creates enquiry with enriched fields and derived name parts', async () => {
    prisma.enquiry.create.mockResolvedValue({
      id: 'enq-1',
      gymId: 'gym-1',
      name: 'John Doe',
      firstName: 'John',
      lastName: 'Doe',
      phone: '9876543210',
      email: 'john@example.com',
      photoUrl: '/uploads/john.png',
      gender: 'Male',
      address: 'Rajkot',
      message: null,
      source: 'walk_in',
      medium: 'offline',
      interestedIn: 'muscle',
      notes: null,
      assignedToUserId: null,
      enquiryDate: new Date('2026-04-03T00:00:00.000Z'),
      followUpAt: new Date('2026-04-05T10:00:00.000Z'),
      status: EnquiryStatus.OPEN,
      createdAt: new Date(),
      updatedAt: new Date(),
      convertedAt: null,
      convertedGymUserId: null,
      assignedTo: null,
      convertedGymUser: null,
    });

    await service.create('actor-1', {
      gymId: 'gym-1',
      name: 'John Doe',
      phone: '9876543210',
      email: 'john@example.com',
      firstName: 'John',
      lastName: 'Doe',
      photoUrl: '/uploads/john.png',
      gender: 'male',
      address: 'Rajkot',
      source: 'walk_in',
      medium: 'offline',
      interestedIn: 'muscle',
      enquiryDate: '2026-04-03',
      followUpAt: '2026-04-05T10:00:00.000Z',
    });

    expect(prisma.enquiry.create).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          name: 'John Doe',
          firstName: 'John',
          lastName: 'Doe',
          gender: 'Male',
          medium: 'offline',
          interestedIn: 'muscle',
        }),
      }),
    );
  });

  it('updates name when first and last name are edited', async () => {
    prisma.enquiry.findFirst.mockResolvedValue({
      id: 'enq-1',
      status: EnquiryStatus.OPEN,
    });
    prisma.enquiry.findUnique.mockResolvedValue({
      name: 'John Doe',
      firstName: 'John',
      lastName: 'Doe',
    });
    prisma.enquiry.update.mockResolvedValue({
      id: 'enq-1',
      gymId: 'gym-1',
      name: 'Jane Smith',
      firstName: 'Jane',
      lastName: 'Smith',
      phone: '9876543210',
      email: null,
      photoUrl: null,
      gender: null,
      address: null,
      message: null,
      source: null,
      medium: null,
      interestedIn: null,
      notes: null,
      assignedToUserId: null,
      enquiryDate: null,
      followUpAt: null,
      status: EnquiryStatus.OPEN,
      createdAt: new Date(),
      updatedAt: new Date(),
      convertedAt: null,
      convertedGymUserId: null,
      assignedTo: null,
      convertedGymUser: null,
    });

    await service.update('actor-1', 'gym-1', 'enq-1', {
      firstName: 'Jane',
      lastName: 'Smith',
    });

    expect(prisma.enquiry.update).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          firstName: 'Jane',
          lastName: 'Smith',
          name: 'Jane Smith',
        }),
      }),
    );
  });

  it('carries gender and address to member conversion', async () => {
    prisma.enquiry.findFirst.mockResolvedValue({
      id: 'enq-1',
      gymId: 'gym-1',
      name: 'John Doe',
      firstName: 'John',
      lastName: 'Doe',
      phone: '9876543210',
      email: 'john@example.com',
      photoUrl: '/uploads/john.png',
      gender: 'Male',
      address: 'Rajkot',
      message: null,
      source: null,
      medium: null,
      interestedIn: null,
      notes: 'needs trainer',
      assignedToUserId: null,
      enquiryDate: null,
      followUpAt: null,
      status: EnquiryStatus.OPEN,
      createdAt: new Date(),
      updatedAt: new Date(),
      convertedAt: null,
      convertedGymUserId: null,
    });
    members.create.mockResolvedValue({ gymUserId: 'member-1' });
    prisma.enquiry.update.mockResolvedValue({});

    await service.convert('actor-1', 'gym-1', 'enq-1', {});

    expect(members.create).toHaveBeenCalledWith(
      'actor-1',
      expect.objectContaining({
        fullName: 'John Doe',
        gender: 'Male',
        address: 'Rajkot',
        avatarUrl: '/uploads/john.png',
      }),
    );
    expect(prisma.enquiry.update).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          photoUrl: '/uploads/john.png',
        }),
      }),
    );
  });

  it('convert uses photoUrlOverride for member avatar and enquiry photo', async () => {
    prisma.enquiry.findFirst.mockResolvedValue({
      id: 'enq-1',
      gymId: 'gym-1',
      name: 'John Doe',
      firstName: 'John',
      lastName: 'Doe',
      phone: '9876543210',
      email: null,
      photoUrl: '/uploads/old.png',
      gender: null,
      address: null,
      message: null,
      source: null,
      medium: null,
      interestedIn: null,
      notes: null,
      assignedToUserId: null,
      enquiryDate: null,
      followUpAt: null,
      status: EnquiryStatus.OPEN,
      createdAt: new Date(),
      updatedAt: new Date(),
      convertedAt: null,
      convertedGymUserId: null,
    });
    members.create.mockResolvedValue({ gymUserId: 'member-1' });
    prisma.enquiry.update.mockResolvedValue({});

    await service.convert('actor-1', 'gym-1', 'enq-1', {
      photoUrlOverride: '/uploads/new.png',
    });

    expect(members.create).toHaveBeenCalledWith(
      'actor-1',
      expect.objectContaining({
        avatarUrl: '/uploads/new.png',
      }),
    );
    expect(prisma.enquiry.update).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          photoUrl: '/uploads/new.png',
        }),
      }),
    );
  });
});
