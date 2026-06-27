import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Prisma } from '@prisma/client';
import { randomBytes } from 'crypto';
import * as QRCode from 'qrcode';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type { UpdateGymProfileDto } from './dto/update-gym-profile.dto';

@Injectable()
export class GymProfileService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly config: ConfigService,
  ) {}

  async get(actorUserId: string, gymId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const gym = await this.prisma.gym.findUniqueOrThrow({
      where: { id: gymId },
      select: {
        id: true,
        name: true,
        slug: true,
        address: true,
        latitude: true,
        longitude: true,
        timezone: true,
        gstin: true,
        logoUrl: true,
      },
    });
    const base =
      this.config.get<string>('APP_PUBLIC_URL')?.replace(/\/$/, '') ||
      'https://gymtrak.app';
    const publicUrl = `${base}/gym/${gym.slug}`;
    const qrPngBase64 = await QRCode.toDataURL(publicUrl, {
      width: 256,
      margin: 1,
    });
    return {
      ...gym,
      latitude: gym.latitude ? gym.latitude.toString() : null,
      longitude: gym.longitude ? gym.longitude.toString() : null,
      publicUrl,
      qr: {
        payloadUrl: publicUrl,
        imagePngDataUrl: qrPngBase64,
      },
    };
  }

  async update(actorUserId: string, gymId: string, dto: UpdateGymProfileDto) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    const data: Prisma.GymUpdateInput = {};
    if (dto.name !== undefined) {
      const n = dto.name?.trim();
      if (n && n.length > 0) {
        data.name = n;
      }
    }
    if (dto.address !== undefined) {
      data.address = dto.address?.trim() || null;
    }
    if (dto.latitude !== undefined) {
      data.latitude =
        dto.latitude == null ? null : new Prisma.Decimal(dto.latitude);
    }
    if (dto.longitude !== undefined) {
      data.longitude =
        dto.longitude == null ? null : new Prisma.Decimal(dto.longitude);
    }
    if (dto.rotateQrSecret) {
      data.qrSigningSecret = randomBytes(32).toString('hex');
    }
    if (dto.gstin !== undefined) {
      data.gstin = dto.gstin?.trim().toUpperCase() || null;
    }
    if (dto.logoUrl !== undefined) {
      data.logoUrl = dto.logoUrl?.trim() || null;
    }
    const gym = await this.prisma.gym.update({
      where: { id: gymId },
      data,
      select: {
        id: true,
        name: true,
        slug: true,
        address: true,
        latitude: true,
        longitude: true,
        timezone: true,
        gstin: true,
        logoUrl: true,
      },
    });
    const base =
      this.config.get<string>('APP_PUBLIC_URL')?.replace(/\/$/, '') ||
      'https://gymtrak.app';
    const publicUrl = `${base}/gym/${gym.slug}`;
    const qrPngBase64 = await QRCode.toDataURL(publicUrl, {
      width: 256,
      margin: 1,
    });
    return {
      ...gym,
      latitude: gym.latitude ? gym.latitude.toString() : null,
      longitude: gym.longitude ? gym.longitude.toString() : null,
      publicUrl,
      qr: {
        payloadUrl: publicUrl,
        imagePngDataUrl: qrPngBase64,
      },
    };
  }
}
