import { Injectable } from '@nestjs/common';
import { GymFeatureKey } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { GymAccessService } from '../../common/services/gym-access.service';

const ALL_KEYS: GymFeatureKey[] = [
  GymFeatureKey.trainers,
  GymFeatureKey.trainer_shifts,
  GymFeatureKey.trainer_attendance,
  GymFeatureKey.trainer_payroll,
  GymFeatureKey.payments,
  GymFeatureKey.dashboard,
];

/**
 * Per-gym product toggles. Missing rows default to enabled so existing gyms stay fully on
 * until an owner explicitly turns a feature off.
 */
@Injectable()
export class GymFeaturesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async listEffective(actorUserId: string, gymId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    return this.effectiveMap(gymId);
  }

  async set(
    actorUserId: string,
    gymId: string,
    key: GymFeatureKey,
    enabled: boolean,
  ) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    await this.prisma.gymFeature.upsert({
      where: { gymId_key: { gymId, key } },
      create: { gymId, key, enabled },
      update: { enabled },
    });
    return this.effectiveMap(gymId);
  }

  async isEnabled(gymId: string, key: GymFeatureKey): Promise<boolean> {
    const row = await this.prisma.gymFeature.findUnique({
      where: { gymId_key: { gymId, key } },
    });
    if (!row) {
      return true;
    }
    return row.enabled;
  }

  private async effectiveMap(
    gymId: string,
  ): Promise<{ gymId: string; features: Record<string, boolean> }> {
    const rows = await this.prisma.gymFeature.findMany({ where: { gymId } });
    const byKey = new Map(rows.map((r) => [r.key, r.enabled]));
    const features: Record<string, boolean> = {};
    for (const k of ALL_KEYS) {
      features[k] = byKey.has(k) ? (byKey.get(k) as boolean) : true;
    }
    return { gymId, features };
  }
}
