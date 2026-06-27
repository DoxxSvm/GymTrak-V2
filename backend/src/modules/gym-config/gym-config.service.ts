import { Injectable, NotFoundException } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import type { UpdateGymConfigDto } from './dto/update-gym-config.dto';

/** API shape expected by the Flutter app (legacy Express contract). */
export type GymConfigApiResponse = {
  currencyCode: string;
  gst: {
    enabled: boolean;
    ratePercent: number;
    inclusive: boolean;
    stateCode: string | null;
  };
  defaultPlan: {
    name: string;
    durationMonths: number;
    priceCents: number;
  };
};

function parseDefaultPlanFromJson(
  json: Prisma.JsonValue | null | undefined,
): GymConfigApiResponse['defaultPlan'] {
  const base = { name: 'Monthly', durationMonths: 1, priceCents: 0 };
  if (!json || typeof json !== 'object' || Array.isArray(json)) {
    return base;
  }
  const o = json as Record<string, unknown>;
  return {
    name: typeof o.name === 'string' ? o.name : base.name,
    durationMonths:
      typeof o.durationMonths === 'number'
        ? o.durationMonths
        : base.durationMonths,
    priceCents:
      typeof o.priceCents === 'number' ? o.priceCents : base.priceCents,
  };
}

function toApiResponse(row: {
  currency: string;
  gstEnabled: boolean;
  gstRatePercent: Prisma.Decimal | null;
  gstInclusive: boolean;
  gstStateCode: string | null;
  defaultPlanConfig: Prisma.JsonValue | null;
}): GymConfigApiResponse {
  const rate =
    row.gstRatePercent != null ? Number(row.gstRatePercent.toString()) : 0;
  return {
    currencyCode: row.currency,
    gst: {
      enabled: row.gstEnabled,
      ratePercent: rate,
      inclusive: row.gstInclusive,
      stateCode: row.gstStateCode,
    },
    defaultPlan: parseDefaultPlanFromJson(row.defaultPlanConfig),
  };
}

function mergeDefaultPlanJson(
  existing: Prisma.JsonValue | null | undefined,
  patch: UpdateGymConfigDto,
): Prisma.InputJsonValue {
  const prev =
    existing && typeof existing === 'object' && !Array.isArray(existing)
      ? { ...(existing as Record<string, unknown>) }
      : {};
  const dp = patch.defaultPlan;
  if (dp?.name !== undefined) prev.name = dp.name;
  if (dp?.durationMonths !== undefined) prev.durationMonths = dp.durationMonths;
  if (dp?.priceCents !== undefined) prev.priceCents = dp.priceCents;
  return prev as Prisma.InputJsonValue;
}

@Injectable()
export class GymConfigService {
  constructor(private readonly prisma: PrismaService) {}

  /**
   * Only the gym owner may read/write (matches legacy Express `ownerUserId` check).
   */
  async assertOwnerGym(gymId: string, userId: string) {
    const gym = await this.prisma.gym.findFirst({
      where: { id: gymId, ownerId: userId },
      select: { id: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found or access denied');
    }
  }

  async get(gymId: string, userId: string): Promise<GymConfigApiResponse> {
    await this.assertOwnerGym(gymId, userId);

    let row = await this.prisma.gymSystemConfig.findUnique({
      where: { gymId },
    });
    if (!row) {
      await this.prisma.gymSystemConfig.create({
        data: {
          gymId,
          currency: 'INR',
          gstEnabled: false,
          gstRatePercent: new Prisma.Decimal(0),
          gstInclusive: true,
          gstStateCode: null,
          defaultPlanConfig: Prisma.JsonNull,
        },
      });
      row = await this.prisma.gymSystemConfig.findUniqueOrThrow({
        where: { gymId },
      });
    }

    return toApiResponse(row);
  }

  async patch(
    gymId: string,
    userId: string,
    dto: UpdateGymConfigDto,
  ): Promise<GymConfigApiResponse> {
    await this.assertOwnerGym(gymId, userId);

    const existing = await this.prisma.gymSystemConfig.findUnique({
      where: { gymId },
    });

    const mergedPlanJson = mergeDefaultPlanJson(
      existing?.defaultPlanConfig ?? null,
      dto,
    );

    const currency =
      dto.currencyCode !== undefined
        ? dto.currencyCode.trim().toUpperCase()
        : (existing?.currency ?? 'INR');
    const gstEnabled = dto.gstEnabled ?? existing?.gstEnabled ?? false;
    const gstInclusive = dto.gstInclusive ?? existing?.gstInclusive ?? true;

    let gstRatePercent: Prisma.Decimal | null;
    if (dto.gstRatePercent !== undefined) {
      gstRatePercent =
        dto.gstRatePercent === null
          ? null
          : new Prisma.Decimal(dto.gstRatePercent);
    } else {
      gstRatePercent =
        existing?.gstRatePercent != null
          ? new Prisma.Decimal(existing.gstRatePercent.toString())
          : new Prisma.Decimal(0);
    }

    let gstStateCode: string | null;
    if (dto.gstStateCode !== undefined) {
      gstStateCode =
        dto.gstStateCode === null || dto.gstStateCode === ''
          ? null
          : dto.gstStateCode.trim();
    } else {
      gstStateCode = existing?.gstStateCode ?? null;
    }

    const row = await this.prisma.gymSystemConfig.upsert({
      where: { gymId },
      create: {
        gymId,
        currency,
        gstEnabled,
        gstRatePercent,
        gstInclusive,
        gstStateCode,
        defaultPlanConfig: mergedPlanJson,
      },
      update: {
        currency,
        gstEnabled,
        gstRatePercent,
        gstInclusive,
        gstStateCode,
        defaultPlanConfig: mergedPlanJson,
      },
    });

    return toApiResponse(row);
  }
}
