import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  AppOnboardingRole,
  GlobalRole,
  GymRole,
  Prisma,
} from '@prisma/client';
import { randomBytes } from 'crypto';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PrismaService } from '../prisma/prisma.service';
import type { CreateGymDto } from './dto/create-gym.dto';
import type { UpdateGymDto } from './dto/update-gym.dto';

export type ListedGym = {
  id: string;
  name: string;
  slug: string;
  status: string;
  address: string | null;
  location: { latitude: string | null; longitude: string | null };
  gstin: string | null;
  logoUrl: string | null;
  role: GymRole;
};

@Injectable()
export class GymsService {
  constructor(private readonly prisma: PrismaService) {}

  async listForUser(userId: string): Promise<{ gyms: ListedGym[] }> {
    const rows = await this.prisma.gym.findMany({
      where: {
        OR: [
          { ownerId: userId },
          { members: { some: { userId, isActive: true } } },
        ],
      },
      select: {
        id: true,
        name: true,
        slug: true,
        status: true,
        address: true,
        latitude: true,
        longitude: true,
        gstin: true,
        logoUrl: true,
        ownerId: true,
        members: {
          where: { userId },
          select: { role: true },
        },
      },
      orderBy: { name: 'asc' },
    });

    const gyms: ListedGym[] = rows.map((g) => {
      const role: GymRole =
        g.ownerId === userId
          ? GymRole.OWNER
          : (g.members[0]?.role ?? GymRole.MEMBER);
      return {
        id: g.id,
        name: g.name,
        slug: g.slug,
        status: g.status,
        address: g.address,
        location: {
          latitude: g.latitude ? g.latitude.toString() : null,
          longitude: g.longitude ? g.longitude.toString() : null,
        },
        gstin: g.gstin,
        logoUrl: g.logoUrl,
        role,
      };
    });

    return { gyms };
  }

  /**
   * Creates an owned gym. Completes owner onboarding when this is the first gym
   * on the owner path (same as POST /onboarding/owner).
   */
  async create(userId: string, dto: CreateGymDto) {
    await this.assertCanCreateOwnedGym(userId);

    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        onboardingCompletedAt: true,
        selectedOnboardingRole: true,
      },
    });

    const baseSlug = slugify(dto.name);
    const slug = await this.uniqueGymSlug(baseSlug);

    const gym = await this.prisma.$transaction(async (tx) => {
      const created = await tx.gym.create({
        data: {
          name: dto.name.trim(),
          slug,
          ownerId: userId,
          address: dto.address?.trim() || null,
          latitude:
            dto.latitude == null ? null : new Prisma.Decimal(dto.latitude),
          longitude:
            dto.longitude == null ? null : new Prisma.Decimal(dto.longitude),
          gstin: dto.gstin?.trim().toUpperCase() || null,
          logoUrl: dto.logoUrl?.trim() || null,
        },
      });

      await tx.gymUser.create({
        data: {
          userId,
          gymId: created.id,
          role: GymRole.OWNER,
        },
      });

      await tx.ownerProfile.upsert({
        where: { userId },
        create: { userId },
        update: {},
      });

      const shouldCompleteOwnerOnboarding =
        !user.onboardingCompletedAt &&
        user.selectedOnboardingRole === AppOnboardingRole.OWNER;

      if (shouldCompleteOwnerOnboarding) {
        await tx.user.update({
          where: { id: userId },
          data: { onboardingCompletedAt: new Date() },
        });
      }

      return created;
    });

    return {
      id: gym.id,
      name: gym.name,
      slug: gym.slug,
      status: gym.status,
    };
  }

  async update(user: JwtUser, gymId: string, dto: UpdateGymDto) {
    const gymRow = await this.prisma.gym.findUnique({
      where: { id: gymId },
    });
    if (!gymRow) {
      throw new NotFoundException('Gym not found');
    }
    this.assertUserCanManageGym(user, gymRow);

    const patchKeys = Object.keys(dto).filter(
      (k) => (dto as Record<string, unknown>)[k] !== undefined,
    );
    if (patchKeys.length === 0) {
      throw new BadRequestException('No fields to update');
    }

    let name = gymRow.name;
    let slug = gymRow.slug;
    if (dto.name !== undefined) {
      name = dto.name.trim();
      slug = await this.uniqueGymSlug(slugify(name), gymId);
    }

    const updated = await this.prisma.gym.update({
      where: { id: gymId },
      data: {
        ...(dto.name !== undefined && { name, slug }),
        ...(dto.address !== undefined && {
          address: dto.address?.trim() || null,
        }),
        ...(dto.latitude !== undefined && {
          latitude:
            dto.latitude == null ? null : new Prisma.Decimal(dto.latitude),
        }),
        ...(dto.longitude !== undefined && {
          longitude:
            dto.longitude == null ? null : new Prisma.Decimal(dto.longitude),
        }),
        ...(dto.gstin !== undefined && {
          gstin: dto.gstin?.trim()
            ? dto.gstin.trim().toUpperCase()
            : null,
        }),
        ...(dto.logoUrl !== undefined && {
          logoUrl: dto.logoUrl?.trim() || null,
        }),
      },
    });

    return {
      id: updated.id,
      name: updated.name,
      slug: updated.slug,
      status: updated.status,
    };
  }

  async remove(user: JwtUser, gymId: string) {
    const gymRow = await this.prisma.gym.findUnique({
      where: { id: gymId },
    });
    if (!gymRow) {
      throw new NotFoundException('Gym not found');
    }
    this.assertUserCanManageGym(user, gymRow);
    await this.prisma.gym.delete({ where: { id: gymId } });
    return { success: true as const };
  }

  private assertUserCanManageGym(
    user: JwtUser,
    gym: { ownerId: string },
  ): void {
    if (user.globalRole === GlobalRole.SUPER_ADMIN) {
      return;
    }
    if (gym.ownerId === user.sub) {
      return;
    }
    throw new ForbiddenException('Only the gym owner can modify this gym');
  }

  private async assertCanCreateOwnedGym(userId: string): Promise<void> {
    const u = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        globalRole: true,
        selectedOnboardingRole: true,
        ownedGyms: { select: { id: true }, take: 1 },
      },
    });

    if (u.globalRole === GlobalRole.SUPER_ADMIN) {
      return;
    }

    if (u.ownedGyms.length > 0) {
      return;
    }

    if (u.selectedOnboardingRole === AppOnboardingRole.OWNER) {
      return;
    }

    throw new ForbiddenException('Only gym owners can create gyms');
  }

  private async uniqueGymSlug(
    base: string,
    excludeGymId?: string,
  ): Promise<string> {
    let candidate = base;
    for (let i = 0; i < 8; i++) {
      const existing = await this.prisma.gym.findUnique({
        where: { slug: candidate },
      });
      if (!existing || existing.id === excludeGymId) {
        return candidate;
      }
      const suffix = randomBytes(3).toString('hex');
      candidate = `${base}-${suffix}`;
    }
    return `${base}-${randomBytes(6).toString('hex')}`;
  }
}

function slugify(name: string): string {
  const s = name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48);
  return s.length > 0 ? s : 'gym';
}
