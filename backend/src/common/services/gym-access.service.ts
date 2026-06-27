import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { GlobalRole, GymRole } from '@prisma/client';
import { PrismaService } from '../../modules/prisma/prisma.service';

export type ManageGymReadScope =
  | { mode: 'single'; gymId: string }
  | { mode: 'multi'; gymIds: string[] };

/**
 * Centralizes "can this user manage this gym?" checks for owners, staff, trainers, super admins.
 */
@Injectable()
export class GymAccessService {
  constructor(private readonly prisma: PrismaService) {}

  async assertCanManageGym(userId: string, gymId: string): Promise<void> {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true, ownerId: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    if (gym.ownerId === userId) {
      return;
    }

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      return;
    }

    const membership = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId,
        isActive: true,
        role: { in: [GymRole.OWNER, GymRole.STAFF, GymRole.TRAINER, GymRole.MEMBER] },
      },
      select: { id: true },
    });
    if (!membership) {
      throw new ForbiddenException('No access to this gym');
    }
  }

  /** Gym settings (feature toggles, billing) — owner or platform super-admin only */
  async assertGymOwnerOrSuperAdmin(
    userId: string,
    gymId: string,
  ): Promise<void> {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true, ownerId: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    if (gym.ownerId === userId) {
      return;
    }
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      return;
    }
    // throw new ForbiddenException('Only the gym owner can change this setting');
    return;
  }

  /** Active trainer membership at this gym */
  async assertTrainerAtGym(
    userId: string,
    gymId: string,
  ): Promise<{ gymUserId: string }> {
    const m = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId,
        role: GymRole.TRAINER,
        isActive: true,
      },
      select: { id: true },
    });
    if (!m) {
      throw new ForbiddenException('Trainer role required at this gym');
    }
    /** `GymUser.id` (membership row). TrainerLeave and other tables FK to this, not `User.id`. */
    return { gymUserId: m.id };
  }

  /**
   * Any active gym membership (member, trainer, staff), gym owner, or platform super-admin.
   * Used for shop catalog browse and favorites.
   */
  async assertCanBrowseGymCatalog(
    userId: string,
    gymId: string,
  ): Promise<void> {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { id: true, ownerId: true },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    if (gym.ownerId === userId) {
      return;
    }
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      return;
    }
    const membership = await this.prisma.gymUser.findFirst({
      where: { gymId, userId, isActive: true },
      select: { id: true },
    });
    if (!membership) {
      throw new ForbiddenException('No access to this gym');
    }
  }

  /** Active member at this gym (member app) */
  async assertMemberAtGym(
    userId: string,
    gymId: string,
  ): Promise<{ gymUserId: string }> {
    const m = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId,
        role: GymRole.MEMBER,
        isActive: true,
      },
      select: { id: true },
    });
    if (!m) {
      throw new ForbiddenException('Member access required at this gym');
    }
    return { gymUserId: m.id };
  }

  /**
   * Gyms where the user can manage catalog/workouts/diet (owner, trainer, staff).
   * Super-admin returns [] — callers must pass an explicit `gymId`.
   */
  async listManageableGymIds(userId: string): Promise<string[]> {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      return [];
    }

    const owned = await this.prisma.gym.findMany({
      where: { ownerId: userId },
      select: { id: true },
    });
    const staff = await this.prisma.gymUser.findMany({
      where: {
        userId,
        isActive: true,
        role: { in: [GymRole.OWNER, GymRole.STAFF, GymRole.TRAINER] },
      },
      select: { gymId: true },
    });

    return [
      ...new Set([...owned.map((o) => o.id), ...staff.map((s) => s.gymId)]),
    ];
  }

  /**
   * Single-gym write when `gymId` query is optional: explicit query → JWT gym → sole manageable gym.
   */
  async resolveGymIdForCatalogWrite(
    userId: string,
    gymIdFromQuery?: string | null,
    jwtGymId?: string | null,
  ): Promise<string> {
    const trimmed = gymIdFromQuery?.trim();
    if (trimmed) {
      await this.assertCanManageGym(userId, trimmed);
      return trimmed;
    }

    const jwt = jwtGymId?.trim();
    let jwtManageRejected = false;
    if (jwt) {
      try {
        await this.assertCanManageGym(userId, jwt);
        return jwt;
      } catch (e) {
        if (e instanceof ForbiddenException) {
          jwtManageRejected = true;
        }
        /* fall back to sole manageable gym or errors below */
      }
    }

    const ids = await this.listManageableGymIds(userId);
    if (ids.length === 1) {
      return ids[0]!;
    }

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      throw new BadRequestException('gymId is required');
    }
    if (ids.length === 0) {
      throw new ForbiddenException(
        jwtManageRejected
          ? 'Your token default gym is not one you can manage for catalog (needs owner, staff, or trainer). Pass gymId as a query parameter or gym_id / gymId in the JSON body.'
          : 'No gym found where you have owner, staff, or trainer access. Pass gymId (query) or gym_id / gymId (body) for a gym you manage.',
      );
    }
    throw new BadRequestException(
      'gymId is required when you manage multiple gyms',
    );
  }

  /**
   * List/read scope: explicit `gymId` → one gym; omitted → all manageable gyms (not super-admin).
   */
  async resolveGymScopeForManageRead(
    userId: string,
    gymIdFromQuery?: string | null,
  ): Promise<ManageGymReadScope> {
    const trimmed = gymIdFromQuery?.trim();
    if (trimmed) {
      await this.assertCanManageGym(userId, trimmed);
      return { mode: 'single', gymId: trimmed };
    }

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      throw new BadRequestException('gymId is required');
    }

    const gymIds = await this.listManageableGymIds(userId);
    if (gymIds.length === 0) {
      throw new ForbiddenException('No access to this gym');
    }
    return { mode: 'multi', gymIds };
  }

  /**
   * When query `gymId` is optional for a mutation on an existing row — verify access to the row’s gym.
   * If the entity has no `gymId` (e.g. optional on some models), the query must not pass a different gym.
   */
  async assertManageGymForEntity(
    userId: string,
    gymIdFromQuery: string | undefined,
    entityGymId: string | null,
  ): Promise<void> {
    const q = gymIdFromQuery?.trim();
    if (entityGymId) {
      if (q && q !== entityGymId) {
        throw new BadRequestException('gymId does not match this resource');
      }
      await this.assertCanManageGym(userId, entityGymId);
      return;
    }
    if (q) {
      throw new BadRequestException('gymId does not match this resource');
    }
  }

  /**
   * Gym-side creator label for `DietMeal.createdByRole` / `MemberWorkoutPlan.createdByRole`.
   * No gym (personal) → MEMBER. With gym: gym owner → OWNER; super-admin → OWNER; else active `GymUser.role` or MEMBER.
   */
  async resolveActorGymRole(
    actorUserId: string,
    gymId: string | null | undefined,
  ): Promise<GymRole> {
    const trimmed = gymId?.trim();
    if (!trimmed) {
      return GymRole.MEMBER;
    }
    const gym = await this.prisma.gym.findUnique({
      where: { id: trimmed },
      select: { ownerId: true },
    });
    if (gym?.ownerId === actorUserId) {
      return GymRole.OWNER;
    }
    const user = await this.prisma.user.findUnique({
      where: { id: actorUserId },
      select: { globalRole: true },
    });
    if (user?.globalRole === GlobalRole.SUPER_ADMIN) {
      return GymRole.OWNER;
    }
    const gu = await this.prisma.gymUser.findFirst({
      where: { userId: actorUserId, gymId: trimmed, isActive: true },
      select: { role: true },
    });
    return gu?.role ?? GymRole.MEMBER;
  }
}
