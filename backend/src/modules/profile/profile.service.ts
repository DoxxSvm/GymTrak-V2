import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  GymRole,
  Prisma,
  SalaryPeriod,
  UserStatus,
  GymStatus,
} from '@prisma/client';
import { normalizeEmailForStorage } from '../../common/utils/normalize-email';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { GymProfileService } from '../gym-profile/gym-profile.service';
import { TrainersService } from '../trainers/trainers.service';
import type { UpdateMeMemberDto } from './dto/update-me-member.dto';
import type { UpdateMeOwnerDto } from './dto/update-me-owner.dto';
import type { UpdateMeTrainerDto } from './dto/update-me-trainer.dto';
import type { UpdateProfileDto } from './dto/update-profile.dto';
import type { UpdateUnifiedProfileDto } from './dto/unified-profile.dto';
import type {
  UnifiedProfileData,
  UnifiedProfileEnvelope,
  UnifiedSalaryDuration,
} from './types/unified-profile.types';

function ageFromDob(d: Date, now: Date): number {
  let age = now.getFullYear() - d.getFullYear();
  const m = now.getMonth() - d.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < d.getDate())) {
    age -= 1;
  }
  return age;
}

const DAY_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function periodToUnified(
  p: SalaryPeriod | null | undefined,
): UnifiedSalaryDuration | null {
  if (p == null) {
    return null;
  }
  switch (p) {
    case SalaryPeriod.MONTHLY:
      return 'month';
    case SalaryPeriod.WEEKLY:
      return 'week';
    case SalaryPeriod.YEARLY:
      return 'year';
    case SalaryPeriod.HOURLY:
    default:
      return 'month';
  }
}

function unifiedToSalaryPeriod(d: UnifiedSalaryDuration): SalaryPeriod {
  switch (d) {
    case 'month':
      return SalaryPeriod.MONTHLY;
    case 'week':
      return SalaryPeriod.WEEKLY;
    case 'year':
      return SalaryPeriod.YEARLY;
    default:
      return SalaryPeriod.MONTHLY;
  }
}

function splitFullName(fullName: string | null | undefined): {
  firstName: string;
  lastName: string;
} {
  const s = (fullName ?? '').trim();
  if (!s) {
    return { firstName: '', lastName: '' };
  }
  const parts = s.split(/\s+/);
  if (parts.length === 1) {
    return { firstName: parts[0], lastName: '' };
  }
  return {
    firstName: parts[0],
    lastName: parts.slice(1).join(' '),
  };
}

@Injectable()
export class ProfileService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly trainers: TrainersService,
    private readonly gymProfile: GymProfileService,
  ) {}

  async getProfile(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, fullName: true, email: true, phone: true },
    });
    if (!user) {
      throw new NotFoundException('User not found');
    }
    const gym = await this.prisma.gym.findFirst({
      where: { ownerId: userId },
      select: { name: true, logoUrl: true },
      orderBy: { createdAt: 'asc' },
    });
    return {
      name: user.fullName ?? '',
      email: user.email ?? '',
      phone: user.phone,
      gym_name: gym?.name ?? '',
      profile_image: gym?.logoUrl ?? '',
    };
  }

  /**
   * Edit-profile payload for the current user at `gymId`.
   * Role is derived: gym owner → owner; else trainer or member membership.
   */
  async getMe(userId: string, gymId: string) {
    const gym = await this.prisma.gym.findFirst({
      where: { id: gymId, status: GymStatus.ACTIVE },
      select: {
        id: true,
        name: true,
        address: true,
        gstin: true,
        logoUrl: true,
        ownerId: true,
      },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const now = new Date();

    if (gym.ownerId === userId) {
      const owner = await this.prisma.user.findUniqueOrThrow({
        where: { id: userId },
        select: {
          id: true,
          fullName: true,
          phone: true,
          avatarUrl: true,
          email: true,
        },
      });
      return {
        role: 'owner' as const,
        phone: owner.phone,
        personal: {
          fullName: owner.fullName ?? '',
          avatarUrl: owner.avatarUrl,
          email: owner.email,
        },
        gym: {
          id: gym.id,
          name: gym.name,
          address: gym.address,
          gstin: gym.gstin,
          logoUrl: gym.logoUrl,
        },
      };
    }

    const gu = await this.prisma.gymUser.findFirst({
      where: { gymId, userId, isActive: true },
      select: { id: true, role: true },
    });
    if (!gu) {
      throw new ForbiddenException('You are not a member of this gym');
    }

    if (gu.role === GymRole.TRAINER) {
      const detail = await this.trainers.getBasicSelf(userId, gymId);
      const fn = splitFullName(detail.user.fullName);
      return {
        role: 'trainer' as const,
        phone: detail.user.phone,
        ...detail,
        firstName: fn.firstName,
        lastName: fn.lastName,
      };
    }

    if (gu.role === GymRole.MEMBER) {
      const row = await this.prisma.gymUser.findFirst({
        where: { id: gu.id },
        include: {
          user: {
            select: {
              id: true,
              fullName: true,
              phone: true,
              avatarUrl: true,
              email: true,
            },
          },
        },
      });
      if (!row) {
        throw new NotFoundException('Member profile not found');
      }
      const dob = row.dateOfBirth;
      return {
        role: 'member' as const,
        phone: row.user.phone,
        gymUserId: row.id,
        fullName: row.user.fullName ?? '',
        name: row.user.fullName ?? '',
        avatarUrl: row.user.avatarUrl,
        profile_image: row.user.avatarUrl,
        email: row.user.email,
        gender: row.gender,
        dateOfBirth: dob,
        ageYears:
          dob && !Number.isNaN(dob.getTime()) ? ageFromDob(dob, now) : null,
        gym: { id: gym.id, name: gym.name },
      };
    }

    throw new ForbiddenException(
      'Edit profile for this role is not available here; use staff settings if applicable.',
    );
  }

  async updateMeMember(userId: string, gymId: string, dto: UpdateMeMemberDto) {
    const { gymUserId } = await this.gymAccess.assertMemberAtGym(userId, gymId);

    const avatar =
      dto.avatarUrl?.trim() || dto.profile_image?.trim() || undefined;
    const displayName = dto.fullName?.trim() || dto.name?.trim();

    await this.prisma.$transaction(async (tx) => {
      const userUpdate: Prisma.UserUpdateInput = {};
      if (displayName !== undefined) {
        userUpdate.fullName = displayName;
      }
      if (avatar !== undefined) {
        userUpdate.avatarUrl = avatar || null;
      }
      if (Object.keys(userUpdate).length > 0) {
        await tx.user.update({
          where: { id: userId },
          data: userUpdate,
        });
      }
      const guUpdate: Prisma.GymUserUpdateInput = {};
      if (dto.gender !== undefined) {
        guUpdate.gender = dto.gender;
      }
      if (dto.dateOfBirth !== undefined) {
        if (!dto.dateOfBirth?.trim()) {
          guUpdate.dateOfBirth = null;
        } else {
          const d = new Date(dto.dateOfBirth);
          if (Number.isNaN(d.getTime())) {
            throw new BadRequestException('Invalid dateOfBirth');
          }
          guUpdate.dateOfBirth = d;
        }
      }
      if (Object.keys(guUpdate).length > 0) {
        await tx.gymUser.update({
          where: { id: gymUserId },
          data: guUpdate,
        });
      }
    });

    return this.getMe(userId, gymId);
  }

  async updateMeOwner(userId: string, gymId: string, dto: UpdateMeOwnerDto) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(userId, gymId);

    const avatar =
      dto.avatarUrl?.trim() || dto.profile_image?.trim() || undefined;
    if (dto.fullName !== undefined || avatar !== undefined) {
      const userUpdate: Prisma.UserUpdateInput = {};
      if (dto.fullName !== undefined) {
        userUpdate.fullName = dto.fullName.trim();
      }
      if (avatar !== undefined) {
        userUpdate.avatarUrl = avatar || null;
      }
      await this.prisma.user.update({
        where: { id: userId },
        data: userUpdate,
      });
    }

    const hasGymPatch =
      dto.gymName !== undefined ||
      dto.gymAddress !== undefined ||
      dto.gymGstNumber !== undefined ||
      dto.gymLogoUrl !== undefined;
    if (hasGymPatch) {
      await this.gymProfile.update(userId, gymId, {
        ...(dto.gymName !== undefined
          ? { name: dto.gymName?.trim() || undefined }
          : {}),
        ...(dto.gymAddress !== undefined
          ? { address: dto.gymAddress?.trim() || null }
          : {}),
        ...(dto.gymGstNumber !== undefined
          ? {
              gstin:
                dto.gymGstNumber === null
                  ? null
                  : dto.gymGstNumber.trim().toUpperCase() || null,
            }
          : {}),
        ...(dto.gymLogoUrl !== undefined
          ? { logoUrl: dto.gymLogoUrl?.trim() || null }
          : {}),
      });
    }

    return this.getMe(userId, gymId);
  }

  async updateMeTrainer(
    userId: string,
    gymId: string,
    dto: UpdateMeTrainerDto,
  ) {
    const avatar =
      dto.avatarUrl?.trim() || dto.profile_image?.trim() || undefined;

    let fullName: string | undefined;
    if (dto.fullName?.trim()) {
      fullName = dto.fullName.trim();
    } else if (dto.firstName != null || dto.lastName != null) {
      const f = dto.firstName?.trim() ?? '';
      const l = dto.lastName?.trim() ?? '';
      const merged = `${f} ${l}`.trim();
      if (merged) {
        fullName = merged;
      }
    }

    let salaryCents: number | null | undefined;
    if (dto.salaryCents !== undefined) {
      salaryCents = dto.salaryCents;
    } else if (dto.salary !== undefined) {
      salaryCents = Math.round(dto.salary * 100);
    }

    await this.trainers.updateSelf(userId, gymId, {
      ...(fullName !== undefined ? { fullName } : {}),
      ...(avatar !== undefined ? { avatarUrl: avatar || null } : {}),
      ...(dto.dateOfBirth !== undefined
        ? { dateOfBirth: dto.dateOfBirth?.trim() || null }
        : {}),
      ...(dto.gender !== undefined ? { gender: dto.gender } : {}),
      ...(dto.experience !== undefined
        ? { experience: dto.experience ?? null }
        : {}),
      ...(dto.address !== undefined ? { address: dto.address ?? null } : {}),
      ...(salaryCents !== undefined ? { salaryCents } : {}),
      ...(dto.salaryPeriod !== undefined
        ? { salaryPeriod: dto.salaryPeriod }
        : {}),
      ...(dto.expertise !== undefined ? { expertise: dto.expertise } : {}),
      ...(dto.shifts !== undefined ? { shifts: dto.shifts } : {}),
    });
    return this.getMe(userId, gymId);
  }

  async updateProfile(userId: string, dto: UpdateProfileDto) {
    await this.prisma.$transaction(async (tx) => {
      const userData: { fullName?: string; email?: string | null } = {};
      if (dto.name !== undefined) {
        userData.fullName = dto.name.trim();
      }
      if (dto.email !== undefined) {
        userData.email = normalizeEmailForStorage(dto.email);
      }
      if (Object.keys(userData).length > 0) {
        await tx.user.update({ where: { id: userId }, data: userData });
      }
      if (dto.profile_image !== undefined) {
        await tx.gym.updateMany({
          where: { ownerId: userId },
          data: { logoUrl: dto.profile_image.trim() || null },
        });
      }
    });
    return this.getProfile(userId);
  }

  async deleteAccount(userId: string) {
    await this.prisma.$transaction(async (tx) => {
      await tx.gym.updateMany({
        where: { ownerId: userId },
        data: { status: GymStatus.ARCHIVED },
      });
      await tx.gymUser.updateMany({
        where: { userId },
        data: { isActive: false },
      });
      await tx.user.update({
        where: { id: userId },
        data: { status: UserStatus.DELETED },
      });
    });
    return { success: true as const };
  }

  /**
   * Unified mobile profile (gym owner or trainer) for `gymId`.
   * Members and other roles receive 403.
   */
  async getUnifiedProfile(
    userId: string,
    gymId: string,
  ): Promise<UnifiedProfileEnvelope> {
    const gym = await this.prisma.gym.findFirst({
      where: { id: gymId, status: GymStatus.ACTIVE },
      select: {
        id: true,
        name: true,
        address: true,
        gstin: true,
        logoUrl: true,
        ownerId: true,
      },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const userBase = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        fullName: true,
        avatarUrl: true,
        createdAt: true,
        updatedAt: true,
      },
    });
    if (!userBase) {
      throw new NotFoundException('User not found');
    }

    if (gym.ownerId === userId) {
      const fn = splitFullName(userBase.fullName);
      const data: UnifiedProfileData = {
        id: userId,
        role: 'gym_owner',
        personalInfo: {
          firstName: fn.firstName,
          lastName: fn.lastName,
          fullName: userBase.fullName ?? '',
          profileImage: userBase.avatarUrl,
          dateOfBirth: null,
          gender: null,
          address: null,
        },
        gymDetails: {
          gymName: gym.name,
          gymAddress: gym.address ?? null,
          gstNumber: gym.gstin ?? null,
          gymLogo: gym.logoUrl ?? null,
        },
        trainerDetails: null,
        createdAt: userBase.createdAt.toISOString(),
        updatedAt: userBase.updatedAt.toISOString(),
      };
      return { success: true, data };
    }

    const gu = await this.prisma.gymUser.findFirst({
      where: { gymId, userId, isActive: true },
      select: { id: true, role: true, dateOfBirth: true, gender: true },
    });
    if (!gu) {
      throw new ForbiddenException('You are not a member of this gym');
    }
    if (gu.role !== GymRole.TRAINER) {
      throw new ForbiddenException(
        'Unified profile is available for gym owners and trainers only',
      );
    }

    const detail = await this.trainers.getBasicSelf(userId, gymId);
    const fn = splitFullName(detail.user.fullName);
    const p = detail.profile;
    const dob = p?.dateOfBirth ?? gu.dateOfBirth;
    const dobStr =
      dob && !Number.isNaN(dob.getTime())
        ? dob.toISOString().slice(0, 10)
        : null;
    const genderRaw = p?.gender ?? gu.gender;
    const gender =
      genderRaw === 'male' || genderRaw === 'female' || genderRaw === 'other'
        ? genderRaw
        : null;
    const salaryMajor =
      p?.salaryCents != null ? Math.round(p.salaryCents / 100) : null;

    const data: UnifiedProfileData = {
      id: userId,
      role: 'trainer',
      personalInfo: {
        firstName: fn.firstName,
        lastName: fn.lastName,
        fullName: detail.user.fullName ?? '',
        profileImage: detail.user.avatarUrl,
        dateOfBirth: dobStr,
        gender,
        address: p?.address ?? null,
      },
      gymDetails: null,
      trainerDetails: {
        experience: p?.experience ?? null,
        salary: salaryMajor,
        salaryDuration: periodToUnified(p?.salaryPeriod ?? undefined),
        expertise: detail.expertise ?? [],
        shifts: detail.shifts.map((s) => ({
          id: s.id,
          name: `Shift — ${DAY_SHORT[s.dayOfWeek] ?? s.dayOfWeek}`,
          dayOfWeek: s.dayOfWeek,
          startTime: s.startTime,
          endTime: s.endTime,
        })),
      },
      createdAt: userBase.createdAt.toISOString(),
      updatedAt: userBase.updatedAt.toISOString(),
    };
    return { success: true, data };
  }

  /**
   * Unified update: `role` must match the caller’s relationship to `gymId`.
   * Use pre-uploaded URLs for images (multipart can be added later).
   */
  async updateUnifiedProfile(
    userId: string,
    gymId: string,
    dto: UpdateUnifiedProfileDto,
  ): Promise<UnifiedProfileEnvelope> {
    const gym = await this.prisma.gym.findFirst({
      where: { id: gymId, status: GymStatus.ACTIVE },
      select: { ownerId: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }

    const isOwner = gym.ownerId === userId;
    const gu = await this.prisma.gymUser.findFirst({
      where: { gymId, userId, isActive: true },
      select: { role: true },
    });
    const isTrainer = gu?.role === GymRole.TRAINER;

    if (dto.role === 'gym_owner') {
      if (!isOwner) {
        throw new BadRequestException(
          'role gym_owner does not match your access at this gym',
        );
      }
      if (dto.trainerDetails != null) {
        throw new BadRequestException(
          'trainerDetails must be omitted for gym_owner',
        );
      }
      this.assertGymOwnerGstIfNeeded(dto);
      const ownerDto = this.mapUnifiedToOwnerDto(dto);
      if (Object.keys(ownerDto).length > 0) {
        await this.updateMeOwner(userId, gymId, ownerDto);
      }
      return this.getUnifiedProfile(userId, gymId);
    }

    if (dto.role === 'trainer') {
      if (!isTrainer) {
        throw new BadRequestException(
          'role trainer does not match your access at this gym',
        );
      }
      if (dto.gymDetails != null) {
        throw new BadRequestException('gymDetails must be omitted for trainer');
      }
      const trainerDto = this.mapUnifiedToTrainerDto(dto);
      if (Object.keys(trainerDto).length > 0) {
        await this.updateMeTrainer(userId, gymId, trainerDto);
      }
      return this.getUnifiedProfile(userId, gymId);
    }

    throw new BadRequestException('Invalid role');
  }

  private assertGymOwnerGstIfNeeded(dto: UpdateUnifiedProfileDto): void {
    const gd = dto.gymDetails;
    if (!gd) {
      return;
    }
    const hasAny = [gd.gymName, gd.gymAddress, gd.gymLogo, gd.gstNumber].some(
      (v) => v != null && String(v).trim() !== '',
    );
    if (!hasAny) {
      return;
    }
    if (!gd.gstNumber?.trim()) {
      throw new BadRequestException(
        'gymDetails.gstNumber is required when sending gym details as gym_owner',
      );
    }
  }

  private mapUnifiedToOwnerDto(dto: UpdateUnifiedProfileDto): UpdateMeOwnerDto {
    const out: UpdateMeOwnerDto = {};
    const p = dto.personalInfo;
    if (p) {
      if (p.fullName?.trim()) {
        out.fullName = p.fullName.trim();
      } else {
        const f = p.firstName?.trim() ?? '';
        const l = p.lastName?.trim() ?? '';
        const merged = `${f} ${l}`.trim();
        if (merged) {
          out.fullName = merged;
        }
      }
      const img = p.profileImage?.trim();
      if (img !== undefined) {
        out.avatarUrl = img || undefined;
        out.profile_image = img || undefined;
      }
    }
    const g = dto.gymDetails;
    if (g) {
      if (g.gymName !== undefined) {
        out.gymName = g.gymName?.trim();
      }
      if (g.gymAddress !== undefined) {
        out.gymAddress = g.gymAddress?.trim();
      }
      if (g.gstNumber !== undefined) {
        out.gymGstNumber = g.gstNumber?.trim().toUpperCase() || null;
      }
      if (g.gymLogo !== undefined) {
        out.gymLogoUrl = g.gymLogo?.trim();
      }
    }
    return out;
  }

  private mapUnifiedToTrainerDto(
    dto: UpdateUnifiedProfileDto,
  ): UpdateMeTrainerDto {
    const out: UpdateMeTrainerDto = {};
    const p = dto.personalInfo;
    if (p) {
      if (p.fullName?.trim()) {
        out.fullName = p.fullName.trim();
      } else {
        const f = p.firstName?.trim();
        const l = p.lastName?.trim();
        if (f !== undefined) {
          out.firstName = f;
        }
        if (l !== undefined) {
          out.lastName = l;
        }
      }
      const img = p.profileImage?.trim();
      if (img !== undefined) {
        out.avatarUrl = img || undefined;
        out.profile_image = img || undefined;
      }
      if (p.dateOfBirth !== undefined) {
        out.dateOfBirth = p.dateOfBirth?.trim() || undefined;
      }
      if (p.gender !== undefined) {
        out.gender = p.gender;
      }
      if (p.address !== undefined) {
        out.address = p.address?.trim();
      }
    }
    const t = dto.trainerDetails;
    if (t) {
      if (t.experience !== undefined) {
        out.experience = t.experience?.trim();
      }
      if (t.salary !== undefined) {
        out.salary = t.salary;
      }
      if (t.salaryDuration !== undefined) {
        out.salaryPeriod = unifiedToSalaryPeriod(t.salaryDuration);
      }
      if (t.expertise !== undefined) {
        out.expertise = t.expertise;
      }
      if (t.shifts !== undefined) {
        out.shifts = t.shifts.map((s) => ({
          dayOfWeek: s.dayOfWeek,
          startTime: s.startTime,
          endTime: s.endTime,
        }));
      }
    }
    return out;
  }
}
