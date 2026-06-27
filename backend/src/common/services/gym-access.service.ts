import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { GlobalRole, GymRole } from '@prisma/client';
import { PrismaService } from '../../modules/prisma/prisma.service';

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
        role: { in: [GymRole.OWNER, GymRole.STAFF, GymRole.TRAINER] },
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
    throw new ForbiddenException('Only the gym owner can change this setting');
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
}
