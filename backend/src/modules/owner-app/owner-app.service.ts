import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { AppOnboardingRole, GymRole, Prisma } from '@prisma/client';
import * as QRCode from 'qrcode';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AuthService } from '../auth/auth.service';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { PrismaService } from '../prisma/prisma.service';
import { GymsService } from '../gyms/gyms.service';
import type { GenerateGymQrDto } from './dto/generate-gym-qr.dto';

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
  ) {}

  async selectRole(jwt: JwtUser, role: 'gym_owner' | 'trainer') {
    const { userId } = await this.auth.ensureUserForOwnerSignup(jwt);

    if (role === 'gym_owner') {
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
      return { success: true as const, role };
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

    return { success: true as const, role };
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
      ...(qrRawPayload !== undefined
        ? { qrContentBase64: qrRawPayload }
        : {}),
    };
  }
}
