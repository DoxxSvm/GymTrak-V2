import { Injectable } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import type {
  DefaultPlanConfigDto,
  UpdateSystemConfigDto,
} from './dto/update-system-config.dto';

export type SystemConfigResponse = {
  gymId: string;
  currency: string;
  gstEnabled: boolean;
  gstRatePercent: number | null;
  gstInclusive: boolean;
  gstStateCode: string | null;
  defaultPlanConfig: Record<string, unknown> | null;
};

@Injectable()
export class SystemConfigService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async get(actorUserId: string, gymId: string): Promise<SystemConfigResponse> {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const row = await this.prisma.gymSystemConfig.findUnique({
      where: { gymId },
    });
    return this.toResponse(row, gymId);
  }

  async update(
    actorUserId: string,
    gymId: string,
    dto: UpdateSystemConfigDto,
  ): Promise<SystemConfigResponse> {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);

    const jsonPatch = this.defaultPlanConfigToPrismaJson(dto);

    const updateData: Prisma.GymSystemConfigUpdateInput = {};
    if (dto.currency !== undefined) {
      updateData.currency = dto.currency.trim().toUpperCase();
    }
    if (dto.gstEnabled !== undefined) {
      updateData.gstEnabled = dto.gstEnabled;
    }
    if (dto.gstRatePercent !== undefined) {
      updateData.gstRatePercent =
        dto.gstRatePercent === null
          ? null
          : new Prisma.Decimal(dto.gstRatePercent);
    }
    if (dto.gstInclusive !== undefined) {
      updateData.gstInclusive = dto.gstInclusive;
    }
    if (dto.gstStateCode !== undefined) {
      updateData.gstStateCode =
        dto.gstStateCode === null ? null : dto.gstStateCode.trim();
    }
    if (jsonPatch !== undefined) {
      updateData.defaultPlanConfig = jsonPatch;
    }

    if (Object.keys(updateData).length === 0) {
      const row = await this.prisma.gymSystemConfig.findUnique({
        where: { gymId },
      });
      return this.toResponse(row, gymId);
    }

    const row = await this.prisma.gymSystemConfig.upsert({
      where: { gymId },
      create: {
        gymId,
        currency: dto.currency?.trim().toUpperCase() ?? 'USD',
        gstEnabled: dto.gstEnabled ?? false,
        gstRatePercent:
          dto.gstRatePercent === undefined || dto.gstRatePercent === null
            ? null
            : new Prisma.Decimal(dto.gstRatePercent),
        gstInclusive: dto.gstInclusive ?? false,
        gstStateCode:
          dto.gstStateCode === undefined || dto.gstStateCode === null
            ? null
            : dto.gstStateCode.trim(),
        defaultPlanConfig:
          jsonPatch === undefined ? Prisma.JsonNull : jsonPatch,
      },
      update: updateData,
    });

    return this.toResponse(row, gymId);
  }

  private toResponse(
    row: {
      currency: string;
      gstEnabled: boolean;
      gstRatePercent: Prisma.Decimal | null;
      gstInclusive: boolean;
      gstStateCode: string | null;
      defaultPlanConfig: Prisma.JsonValue | null;
    } | null,
    gymId: string,
  ): SystemConfigResponse {
    const json = row?.defaultPlanConfig;
    return {
      gymId,
      currency: row?.currency ?? 'USD',
      gstEnabled: row?.gstEnabled ?? false,
      gstRatePercent:
        row?.gstRatePercent != null ? Number(row.gstRatePercent) : null,
      gstInclusive: row?.gstInclusive ?? false,
      gstStateCode: row?.gstStateCode ?? null,
      defaultPlanConfig:
        json && typeof json === 'object' && !Array.isArray(json)
          ? (json as Record<string, unknown>)
          : null,
    };
  }

  private defaultPlanConfigToPrismaJson(
    dto: UpdateSystemConfigDto,
  ): Prisma.InputJsonValue | typeof Prisma.JsonNull | undefined {
    if (dto.defaultPlanConfig === undefined) {
      return undefined;
    }
    if (dto.defaultPlanConfig === null) {
      return Prisma.JsonNull;
    }
    return this.serializeDefaultPlan(dto.defaultPlanConfig);
  }

  private serializeDefaultPlan(v: DefaultPlanConfigDto): Prisma.InputJsonValue {
    const out: Record<string, number> = {};
    if (v.membershipDurationDays != null) {
      out.membershipDurationDays = v.membershipDurationDays;
    }
    if (v.ptDurationDays != null) {
      out.ptDurationDays = v.ptDurationDays;
    }
    return Object.keys(out).length > 0 ? out : {};
  }
}
