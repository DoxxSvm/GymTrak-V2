import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { AppOnboardingRole, GymRole, Prisma } from '@prisma/client';
import * as QRCode from 'qrcode';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuthService } from '../auth/auth.service';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { OnboardingService } from '../onboarding/onboarding.service';
import { PrismaService } from '../prisma/prisma.service';
import { GymsService } from '../gyms/gyms.service';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import type { GenerateGymQrDto } from './dto/generate-gym-qr.dto';
import type { SwitchToMemberDto } from './dto/switch-to-member.dto';

const GYM_LOCATION_QR_VERSION = 1 as const;

type GymLocationQrPayload = {
  v: typeof GYM_LOCATION_QR_VERSION;
  gymId: string;
  address: string;
  latitude: number;
  longitude: number;
  gymRadius: number;
};

@Injectable()
export class OwnerAppService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gyms: GymsService,
    private readonly auth: AuthService,
    private readonly gymAccess: GymAccessService,
    private readonly onboarding: OnboardingService,
    private readonly subscriptions: SubscriptionsService,
  ) { }

  /** New access + refresh pair (snake_case) for mobile clients after role selection. */
  private async withFreshTokens<T extends Record<string, unknown>>(
    userId: string,
    body: T,
  ): Promise<T & { access_token: string; refresh_token: string }> {
    const userRow = await this.auth.getUserForTokenPair(userId);
    const tokens = await this.auth.issueTokenPair(userRow);
    return {
      ...body,
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
    };
  }

  async selectRole(
    jwt: JwtUser,
    role: 'gym_owner' | 'trainer' | 'owner' | 'member',
  ) {
    const { userId } = await this.auth.ensureUserForOwnerSignup(jwt);

    if (role === 'member') {
      await this.prisma.user.update({
        where: { id: userId },
        data: { selectedOnboardingRole: AppOnboardingRole.MEMBER },
      });
      return this.withFreshTokens(userId, {
        success: true as const,
        role: 'member' as const,
      });
    }

    if (role === 'gym_owner' || role === 'owner') {
      const user = await this.prisma.user.findUniqueOrThrow({
        where: { id: userId },
        select: {
          selectedOnboardingRole: true,
          onboardingCompletedAt: true,
          ownedGyms: { select: { id: true }, take: 1 },
        },
      });
      if (user.onboardingCompletedAt || user.ownedGyms.length > 0) {
        throw new ConflictException('Role cannot be changed after gym setup');
      }
      await this.prisma.user.update({
        where: { id: userId },
        data: { selectedOnboardingRole: AppOnboardingRole.OWNER },
      });
      return this.withFreshTokens(userId, {
        success: true as const,
        role: 'gym_owner' as const,
      });
    }

    const trainerMembership = await this.prisma.gymUser.findFirst({
      where: { userId, role: GymRole.TRAINER, isActive: true },
      select: { id: true },
    });
    if (!trainerMembership) {
      throw new UnauthorizedException(
        'Trainer role not available for this account',
      );
    }

    return this.withFreshTokens(userId, {
      success: true as const,
      role: 'trainer' as const,
    });
  }

  /** Owned gym for persona switch: JWT default gym if caller owns it, else lexicographically first owned gym. */
  private async resolveOwnedGymIdForOwnerPersona(
    userId: string,
    jwtGymId?: string | null,
  ): Promise<string> {
    const trimmed = jwtGymId?.trim();
    if (trimmed) {
      const owned = await this.prisma.gym.findFirst({
        where: { id: trimmed, ownerId: userId },
        select: { id: true },
      });
      if (owned) {
        return owned.id;
      }
    }
    const g = await this.prisma.gym.findFirst({
      where: { ownerId: userId },
      orderBy: { name: 'asc' },
      select: { id: true },
    });
    if (!g) {
      throw new ForbiddenException(
        'Only gym owners can create or switch to member profile',
      );
    }
    return g.id;
  }

  /**
   * Ensures a real `GymUser` MEMBER row at the owner's gym (same `userId` / phone as owner).
   * Separate from the OWNER row — enables member APIs (`assertMemberAtGym`) while persona JWT drives UI.
   */
  private async ensureOwnerSelfMemberGymUser(
    tx: Prisma.TransactionClient,
    params: {
      userId: string;
      gymId: string;
      gender?: string | null;
    },
  ): Promise<string> {
    const existing = await tx.gymUser.findFirst({
      where: {
        userId: params.userId,
        gymId: params.gymId,
        role: GymRole.MEMBER,
      },
      select: { id: true },
    });
    if (existing) {
      return existing.id;
    }
    const created = await tx.gymUser.create({
      data: {
        userId: params.userId,
        gymId: params.gymId,
        role: GymRole.MEMBER,
        isActive: true,
        gender: params.gender?.trim() || null,
      },
      select: { id: true },
    });
    return created.id;
  }

  /**
   * Dual persona: gym owner trains as member (same userId / mobile). Creates `member_profiles` once and a `GymUser` MEMBER row at the resolved owned gym.
   */
  async switchToMember(jwt: JwtUser, dto: SwitchToMemberDto) {
    if (jwt.isTemp === true) {
      throw new UnauthorizedException(
        'Complete signup before switching profile',
      );
    }
    const userId = jwt.sub;
    const gymId = await this.resolveOwnedGymIdForOwnerPersona(userId, jwt.gymId);

    const existing = await this.prisma.memberProfile.findUnique({
      where: { userId },
    });

    if (!existing) {
      const name = dto.name?.trim();
      const gender = dto.gender?.trim();
      if (
        !name ||
        dto.age == null ||
        !gender ||
        dto.height == null ||
        dto.weight == null
      ) {
        throw new BadRequestException({
          message:
            'First switch to member requires name, age, gender, height (cm), and weight (kg)',
          isOwnerMemberProfile: false,
        });
      }

      const age = dto.age;
      const heightCm = dto.height;
      const weightKg = dto.weight;

      try {
        await this.prisma.$transaction(async (tx) => {
          await tx.memberProfile.create({
            data: {
              userId,
              name,
              ageYears: age,
              gender,
              heightCm: new Prisma.Decimal(heightCm),
              weightKg: new Prisma.Decimal(weightKg),
            },
          });
          await tx.ownerProfile.upsert({
            where: { userId },
            create: { userId },
            update: {},
          });
          const memberGuId = await this.ensureOwnerSelfMemberGymUser(tx, {
            userId,
            gymId,
            gender,
          });
          await tx.user.update({
            where: { id: userId },
            data: {
              lastActiveRole: 'MEMBER',
              fullName: name,
              ageYears: age,
              gender,
              heightCm: new Prisma.Decimal(heightCm),
              weightKg: new Prisma.Decimal(weightKg),
            },
          });
          await this.subscriptions.syncMembershipEndsAt(tx, memberGuId);
        });
      } catch (e) {
        if (
          e instanceof Prisma.PrismaClientKnownRequestError &&
          e.code === 'P2002'
        ) {
          throw new ConflictException('Member profile already exists');
        }
        throw e;
      }

      return this.withFreshTokens(userId, {
        success: true as const,
        message: 'Member profile created and switched',
        role: 'member' as const,
      });
    }

    await this.prisma.$transaction(async (tx) => {
      const memberGuId = await this.ensureOwnerSelfMemberGymUser(tx, {
        userId,
        gymId,
        gender: existing.gender,
      });
      await tx.user.update({
        where: { id: userId },
        data: { lastActiveRole: 'MEMBER' },
      });
      await this.subscriptions.syncMembershipEndsAt(tx, memberGuId);
    });

    return this.withFreshTokens(userId, {
      success: true as const,
      message: 'Switched to member profile',
      role: 'member' as const,
    });
  }

  async getProfileStatus(jwt: JwtUser) {
    const userId = jwt.sub;
    const profile = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        lastActiveRole: true,
        memberProfile: {
          select: {
            id: true,
          },
        },
        ownerProfile: {
          select: {
            id: true,
          },
        }
      },
    });

    if (profile?.memberProfile && profile?.ownerProfile) {
      return {
        isSwitcheable: true as const,
        lastActiveRole: profile.lastActiveRole === 'OWNER' ? 'gym_owner' : 'member',
      };
    }

    return {
      isSwitcheable: false as const,
    };
  }

  async switchToOwner(jwt: JwtUser) {
    if (jwt.isTemp === true) {
      throw new UnauthorizedException(
        'Complete signup before switching profile',
      );
    }
    const userId = jwt.sub;

    const ownsGym = await this.prisma.gym.findFirst({
      where: { ownerId: userId },
      select: { id: true },
    });
    if (!ownsGym) {
      throw new ForbiddenException('Only gym owners can switch profile');
    }

    await this.prisma.$transaction(async (tx) => {
      await tx.ownerProfile.upsert({
        where: { userId },
        create: { userId },
        update: {},
      });
      await tx.user.update({
        where: { id: userId },
        data: { lastActiveRole: 'OWNER' },
      });
    });

    return this.withFreshTokens(userId, {
      success: true as const,
      message: 'Switched to owner profile',
      role: 'owner' as const,
    });
  }

  async setGym(jwt: JwtUser, dto: { gym_name: string; owner_name: string }) {
    const { userId, wasTemp } = await this.auth.ensureUserForOwnerSignup(jwt);

    const existingOwned = await this.prisma.gym.count({
      where: { ownerId: userId },
    });
    if (existingOwned > 0) {
      throw new ConflictException('One gym per owner is allowed in this flow');
    }

    const ownerName = dto.owner_name.trim();
    if (!ownerName) {
      throw new BadRequestException('owner_name is required');
    }
    await this.prisma.user.update({
      where: { id: userId },
      data: { fullName: ownerName },
    });

    const gym = await this.gyms.create(userId, { name: dto.gym_name.trim() });

    const base = {
      success: true as const,
      gym_id: gym.id,
      gym_name: gym.name,
    };

    if (!wasTemp) {
      return base;
    }

    const userRow = await this.auth.getUserForTokenPair(userId);
    const tokens = await this.auth.issueTokenPair(userRow);
    return {
      ...base,
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
    };
  }

  private encodeGymLocationQrPayload(payload: GymLocationQrPayload): string {
    return Buffer.from(JSON.stringify(payload), 'utf8').toString('base64');
  }

  async generateGymQr(actorUserId: string, dto: GenerateGymQrDto) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, dto.gymId);

    const payload: GymLocationQrPayload = {
      v: GYM_LOCATION_QR_VERSION,
      gymId: dto.gymId,
      address: dto.address.trim(),
      latitude: dto.latitude,
      longitude: dto.longitude,
      gymRadius: dto.gymRadius,
    };
    const qrContent = this.encodeGymLocationQrPayload(payload);
    const gymQrCode = await QRCode.toDataURL(qrContent, {
      width: 280,
      margin: 1,
      errorCorrectionLevel: 'M',
    });

    const gym = await this.prisma.gym.update({
      where: { id: dto.gymId },
      data: {
        address: dto.address.trim(),
        latitude: new Prisma.Decimal(dto.latitude),
        longitude: new Prisma.Decimal(dto.longitude),
        gymRadius: new Prisma.Decimal(dto.gymRadius),
        gymQrCode,
      },
      select: {
        id: true,
        name: true,
        slug: true,
        address: true,
        latitude: true,
        longitude: true,
        gymRadius: true,
        gymQrCode: true,
        logoUrl: true,
        status: true,
        timezone: true,
        gstin: true,
      },
    });

    return {
      success: true as const,
      data: this.serializeGymQrResponse(gym, qrContent),
    };
  }

  async scanGymQr(gymIdRaw: string) {
    const gymId = gymIdRaw?.trim();
    if (!gymId) {
      throw new BadRequestException('gymId query is required');
    }
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: {
        id: true,
        name: true,
        slug: true,
        address: true,
        latitude: true,
        longitude: true,
        gymRadius: true,
        gymQrCode: true,
        logoUrl: true,
        status: true,
        timezone: true,
        gstin: true,
      },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    return {
      success: true as const,
      data: this.serializeGymQrResponse(gym),
    };
  }

  private serializeGymQrResponse(
    gym: {
      id: string;
      name: string;
      slug: string;
      address: string | null;
      latitude: Prisma.Decimal | null;
      longitude: Prisma.Decimal | null;
      gymRadius: Prisma.Decimal | null;
      gymQrCode: string | null;
      logoUrl: string | null;
      status: string;
      timezone: string;
      gstin: string | null;
    },
    qrRawPayload?: string,
  ) {
    return {
      id: gym.id,
      name: gym.name,
      slug: gym.slug,
      address: gym.address,
      latitude: gym.latitude?.toString() ?? null,
      longitude: gym.longitude?.toString() ?? null,
      gymRadius: gym.gymRadius?.toString() ?? null,
      gymQrCode: gym.gymQrCode,
      imageUrl: gym.gymQrCode,
      logoUrl: gym.logoUrl,
      status: gym.status,
      timezone: gym.timezone,
      gstin: gym.gstin,
      ...(qrRawPayload !== undefined ? { qrContentBase64: qrRawPayload } : {}),
    };
  }
}
