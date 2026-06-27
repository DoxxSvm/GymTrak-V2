import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { EnquiryStatus, Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { NOTIFICATION_EVENTS } from '../notifications/domain-events';
import { PrismaService } from '../prisma/prisma.service';
import { MembersService } from '../members/members.service';
import { SaasEntitlementsService } from '../saas/saas-entitlements.service';
import type { ConvertEnquiryDto } from './dto/convert-enquiry.dto';
import type { CreateEnquiryDto } from './dto/create-enquiry.dto';
import type { UpdateEnquiryDto } from './dto/update-enquiry.dto';

@Injectable()
export class EnquiriesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly members: MembersService,
    private readonly saas: SaasEntitlementsService,
    private readonly events: EventEmitter2,
  ) {}

  async list(
    actorUserId: string,
    gymId: string,
    status: EnquiryStatus | undefined,
    q: string | undefined,
    limit: number,
    offset: number,
  ) {
    await this.saas.assertLeads(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const where: Prisma.EnquiryWhereInput = {
      gymId,
      ...(status ? { status } : {}),
      ...(q?.trim()
        ? {
            OR: [
              { name: { contains: q.trim(), mode: 'insensitive' } },
              { firstName: { contains: q.trim(), mode: 'insensitive' } },
              { lastName: { contains: q.trim(), mode: 'insensitive' } },
              { phone: { contains: q.trim() } },
              { email: { contains: q.trim(), mode: 'insensitive' } },
              { medium: { contains: q.trim(), mode: 'insensitive' } },
              { interestedIn: { contains: q.trim(), mode: 'insensitive' } },
              { address: { contains: q.trim(), mode: 'insensitive' } },
            ],
          }
        : {}),
    };

    const [total, items] = await Promise.all([
      this.prisma.enquiry.count({ where }),
      this.prisma.enquiry.findMany({
        where,
        orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
        take: limit,
        skip: offset,
        include: {
          assignedTo: {
            select: { id: true, fullName: true, phone: true },
          },
          convertedGymUser: {
            select: { id: true },
          },
        },
      }),
    ]);

    return {
      total,
      limit,
      offset,
      items: items.map((e) => this.serialize(e)),
    };
  }

  async getOne(actorUserId: string, gymId: string, enquiryId: string) {
    await this.saas.assertLeads(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.enquiry.findFirst({
      where: { id: enquiryId, gymId },
      include: {
        assignedTo: {
          select: { id: true, fullName: true, phone: true },
        },
        convertedGymUser: {
          select: { id: true },
        },
      },
    });
    if (!row) {
      throw new NotFoundException('Enquiry not found');
    }
    return this.serialize(row);
  }

  async create(actorUserId: string, dto: CreateEnquiryDto) {
    await this.saas.assertLeads(dto.gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, dto.gymId);

    const followUpAt = this.parseDateValue(dto.followUpAt, 'followUpAt');
    const enquiryDate = this.parseDateValue(dto.enquiryDate, 'enquiryDate');
    const photoUrl = this.cleanOptionalString(dto.photoUrl);
    const gender = this.normalizeGender(dto.gender);
    const firstNameInput = this.cleanOptionalString(dto.firstName);
    const lastNameInput = this.cleanOptionalString(dto.lastName);
    const [firstName, lastName] = this.resolveNameParts(
      dto.name,
      firstNameInput,
      lastNameInput,
    );
    const name = this.resolveEnquiryName(dto.name, firstName, lastName);

    const row = await this.prisma.enquiry.create({
      data: {
        gymId: dto.gymId,
        name,
        firstName,
        lastName,
        phone: dto.phone.trim(),
        email: dto.email?.trim() || null,
        photoUrl,
        gender,
        address: this.cleanOptionalString(dto.address),
        message: dto.message?.trim() || null,
        source: dto.source?.trim() || null,
        medium: this.cleanOptionalString(dto.medium),
        interestedIn: this.cleanOptionalString(dto.interestedIn),
        notes: dto.notes?.trim() || null,
        assignedToUserId: dto.assignedToUserId ?? null,
        enquiryDate,
        followUpAt,
        status: EnquiryStatus.OPEN,
      },
      include: {
        assignedTo: {
          select: { id: true, fullName: true, phone: true },
        },
        convertedGymUser: {
          select: { id: true },
        },
      },
    });

    this.events.emit(NOTIFICATION_EVENTS.ENQUIRY_CREATED, {
      gymId: row.gymId,
      enquiryId: row.id,
      leadName: row.name,
      actorUserId,
    });

    return this.serialize(row);
  }

  async update(
    actorUserId: string,
    gymId: string,
    enquiryId: string,
    dto: UpdateEnquiryDto,
  ) {
    await this.saas.assertLeads(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    if (dto.status === EnquiryStatus.CONVERTED) {
      throw new BadRequestException(
        'Use POST /enquiries/:id/convert to convert to a member',
      );
    }

    const existing = await this.prisma.enquiry.findFirst({
      where: { id: enquiryId, gymId },
      select: { id: true, status: true },
    });
    if (!existing) {
      throw new NotFoundException('Enquiry not found');
    }
    if (existing.status === EnquiryStatus.CONVERTED) {
      throw new ConflictException('Enquiry already converted');
    }

    const followUpAt = this.parseNullableDateField(
      dto.followUpAt,
      'followUpAt',
    );
    const enquiryDate = this.parseNullableDateField(
      dto.enquiryDate,
      'enquiryDate',
    );
    const firstNameInput = this.cleanOptionalString(dto.firstName);
    const lastNameInput = this.cleanOptionalString(dto.lastName);
    const nameInput = this.cleanOptionalString(dto.name);

    const data: Prisma.EnquiryUncheckedUpdateInput = {};
    if (
      dto.name != null ||
      dto.firstName !== undefined ||
      dto.lastName !== undefined
    ) {
      const existingWithNames = await this.prisma.enquiry.findUnique({
        where: { id: enquiryId },
        select: { name: true, firstName: true, lastName: true },
      });
      const baseName = nameInput ?? existingWithNames?.name ?? '';
      const baseFirstName =
        firstNameInput ?? existingWithNames?.firstName ?? undefined;
      const baseLastName =
        lastNameInput ?? existingWithNames?.lastName ?? undefined;
      const [nextFirstName, nextLastName] = this.resolveNameParts(
        baseName,
        baseFirstName,
        baseLastName,
      );
      data.firstName = nextFirstName;
      data.lastName = nextLastName;
      data.name = this.resolveEnquiryName(
        baseName,
        nextFirstName,
        nextLastName,
      );
    }
    if (dto.phone != null) {
      data.phone = dto.phone.trim();
    }
    if (dto.email !== undefined) {
      data.email = dto.email?.trim() || null;
    }
    if (dto.photoUrl !== undefined) {
      data.photoUrl = this.cleanOptionalString(dto.photoUrl);
    }
    if (dto.gender !== undefined) {
      data.gender = this.normalizeGender(dto.gender);
    }
    if (dto.address !== undefined) {
      data.address = this.cleanOptionalString(dto.address);
    }
    if (dto.message !== undefined) {
      data.message = dto.message?.trim() || null;
    }
    if (dto.source !== undefined) {
      data.source = dto.source?.trim() || null;
    }
    if (dto.medium !== undefined) {
      data.medium = this.cleanOptionalString(dto.medium);
    }
    if (dto.interestedIn !== undefined) {
      data.interestedIn = this.cleanOptionalString(dto.interestedIn);
    }
    if (dto.notes !== undefined) {
      data.notes = dto.notes?.trim() || null;
    }
    if (dto.assignedToUserId !== undefined) {
      data.assignedToUserId = dto.assignedToUserId || null;
    }
    if (enquiryDate !== undefined) {
      data.enquiryDate = enquiryDate;
    }
    if (followUpAt !== undefined) {
      data.followUpAt = followUpAt;
    }
    if (dto.status != null) {
      data.status = dto.status;
    }

    const row = await this.prisma.enquiry.update({
      where: { id: enquiryId },
      data,
      include: {
        assignedTo: {
          select: { id: true, fullName: true, phone: true },
        },
        convertedGymUser: {
          select: { id: true },
        },
      },
    });

    return this.serialize(row);
  }

  async convert(
    actorUserId: string,
    gymId: string,
    enquiryId: string,
    dto: ConvertEnquiryDto,
  ) {
    await this.saas.assertLeads(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);

    const enquiry = await this.prisma.enquiry.findFirst({
      where: { id: enquiryId, gymId },
    });
    if (!enquiry) {
      throw new NotFoundException('Enquiry not found');
    }
    if (enquiry.status === EnquiryStatus.CONVERTED) {
      throw new ConflictException('Enquiry already converted');
    }

    const phone = (dto.phoneOverride ?? enquiry.phone).trim();
    const fullName = (
      dto.fullNameOverride ??
      this.resolveEnquiryName(enquiry.name, enquiry.firstName, enquiry.lastName)
    ).trim();
    const email =
      dto.emailOverride?.trim() ?? enquiry.email?.trim() ?? undefined;

    const photoUrlResolved =
      dto.photoUrlOverride !== undefined
        ? this.cleanOptionalString(dto.photoUrlOverride)
        : enquiry.photoUrl?.trim()
          ? enquiry.photoUrl.trim()
          : null;

    const member = await this.members.create(actorUserId, {
      gymId,
      phone,
      fullName,
      email,
      isLead: false,
      heightCm: dto.heightCm,
      weightKg: dto.weightKg,
      notes: dto.notes ?? enquiry.notes ?? undefined,
      gender: dto.gender ?? enquiry.gender ?? undefined,
      address: dto.address ?? enquiry.address ?? undefined,
      ...(dto.aadhaar_number ? { aadhaar_number: dto.aadhaar_number } : {}),
      initialSubscription: dto.initialSubscription,
      ...(photoUrlResolved ? { avatarUrl: photoUrlResolved } : {}),
    });

    const [firstName, lastName] = this.resolveNameParts(fullName, null, null);
    await this.prisma.enquiry.update({
      where: { id: enquiryId },
      data: {
        status: EnquiryStatus.CONVERTED,
        convertedAt: new Date(),
        convertedGymUserId: member.gymUserId,
        phone,
        name: fullName,
        firstName,
        lastName,
        ...(email !== undefined ? { email } : {}),
        photoUrl: photoUrlResolved,
      },
    });

    this.events.emit(NOTIFICATION_EVENTS.ENQUIRY_CONVERTED, {
      gymId,
      enquiryId,
      memberGymUserId: member.gymUserId,
      memberName: fullName,
      actorUserId,
    });

    return {
      enquiryId,
      status: EnquiryStatus.CONVERTED,
      convertedAt: new Date().toISOString(),
      member,
    };
  }

  async stats(actorUserId: string, gymId: string) {
    await this.saas.assertLeads(gymId, actorUserId);
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const [total, converted, open] = await Promise.all([
      this.prisma.enquiry.count({ where: { gymId } }),
      this.prisma.enquiry.count({
        where: { gymId, status: EnquiryStatus.CONVERTED },
      }),
      this.prisma.enquiry.count({
        where: { gymId, status: EnquiryStatus.OPEN },
      }),
    ]);
    return {
      total,
      converted,
      pending: open,
    };
  }

  private serialize(
    row: Prisma.EnquiryGetPayload<{
      include: {
        assignedTo: { select: { id: true; fullName: true; phone: true } };
        convertedGymUser: { select: { id: true } };
      };
    }>,
  ) {
    return {
      id: row.id,
      gymId: row.gymId,
      name: row.name,
      firstName: row.firstName,
      lastName: row.lastName,
      phone: row.phone,
      email: row.email,
      photoUrl: row.photoUrl,
      gender: row.gender,
      address: row.address,
      message: row.message,
      source: row.source,
      medium: row.medium,
      interestedIn: row.interestedIn,
      notes: row.notes,
      status: row.status,
      enquiryDate: row.enquiryDate,
      followUpAt: row.followUpAt,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
      convertedAt: row.convertedAt,
      assignedTo: row.assignedTo,
      convertedGymUserId: row.convertedGymUserId,
      convertedMemberId: row.convertedGymUser?.id ?? null,
    };
  }

  private parseDateValue(
    value: string | undefined,
    field: string,
  ): Date | null {
    if (!value) {
      return null;
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      throw new BadRequestException(`Invalid ${field}`);
    }
    return parsed;
  }

  private parseNullableDateField(
    value: string | undefined,
    field: string,
  ): Date | null | undefined {
    if (value === undefined) {
      return undefined;
    }
    if (!value) {
      return null;
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      throw new BadRequestException(`Invalid ${field}`);
    }
    return parsed;
  }

  private cleanOptionalString(value: string | undefined): string | null {
    if (value === undefined) {
      return null;
    }
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }

  private normalizeGender(value: string | undefined): string | null {
    if (value === undefined) {
      return null;
    }
    const raw = value.trim().toLowerCase();
    if (!raw) {
      return null;
    }
    if (raw === 'male') {
      return 'Male';
    }
    if (raw === 'female') {
      return 'Female';
    }
    if (raw === 'other') {
      return 'Other';
    }
    return value.trim();
  }

  private resolveNameParts(
    name: string,
    firstName: string | null | undefined,
    lastName: string | null | undefined,
  ): [string | null, string | null] {
    const first = firstName?.trim() ?? '';
    const last = lastName?.trim() ?? '';
    if (first || last) {
      return [first || null, last || null];
    }
    const normalized = name.trim();
    if (!normalized) {
      return [null, null];
    }
    const parts = normalized.split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
      return [parts[0], null];
    }
    return [parts[0], parts.slice(1).join(' ')];
  }

  private resolveEnquiryName(
    fallbackName: string,
    firstName: string | null | undefined,
    lastName: string | null | undefined,
  ): string {
    const full = [firstName?.trim(), lastName?.trim()]
      .filter(Boolean)
      .join(' ');
    if (full) {
      return full;
    }
    const normalizedFallback = fallbackName.trim();
    if (!normalizedFallback) {
      throw new BadRequestException('name or firstName is required');
    }
    return normalizedFallback;
  }
}
