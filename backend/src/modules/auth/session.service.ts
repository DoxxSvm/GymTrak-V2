import { Injectable } from '@nestjs/common';
import { GlobalRole, GymRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import type { AuthSession, PublicUser } from './types/auth-session.type';

@Injectable()
export class SessionService {
  constructor(private readonly prisma: PrismaService) {}

  async getPublicUser(userId: string): Promise<PublicUser> {
    const u = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        id: true,
        phone: true,
        fullName: true,
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
      },
    });
    return u;
  }

  async buildSession(
    userId: string,
  ): Promise<{ session: AuthSession; user: PublicUser }> {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        id: true,
        phone: true,
        fullName: true,
        heightCm: true,
        weightKg: true,
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
        globalRole: true,
        ownedGyms: { select: { id: true } },
        gymMemberships: {
          where: { isActive: true },
          select: { role: true },
        },
      },
    });

    const publicUser: PublicUser = {
      id: user.id,
      phone: user.phone,
      fullName: user.fullName,
      selectedOnboardingRole: user.selectedOnboardingRole,
      onboardingCompletedAt: user.onboardingCompletedAt,
    };

    const isStaff = user.gymMemberships.some(
      (m) => m.role === GymRole.TRAINER || m.role === GymRole.STAFF,
    );
    const isSuperAdmin = user.globalRole === GlobalRole.SUPER_ADMIN;

    if (isSuperAdmin || isStaff) {
      return {
        user: publicUser,
        session: {
          needsRoleSelection: false,
          needsOwnerOnboarding: false,
          needsMemberOnboarding: false,
          onboardingCompleted: true,
          isStaff: true,
        },
      };
    }

    const memberDone =
      user.selectedOnboardingRole === 'MEMBER' &&
      user.fullName != null &&
      user.fullName.trim().length > 0 &&
      user.heightCm != null &&
      user.weightKg != null;

    const ownerDone = user.ownedGyms.length > 0;

    const onboardingCompleted = !!(
      user.onboardingCompletedAt ||
      ownerDone ||
      memberDone
    );

    const needsRoleSelection =
      !onboardingCompleted && user.selectedOnboardingRole == null;

    const needsOwnerOnboarding =
      !onboardingCompleted &&
      user.selectedOnboardingRole === 'OWNER' &&
      user.ownedGyms.length === 0;

    const needsMemberOnboarding =
      !onboardingCompleted &&
      user.selectedOnboardingRole === 'MEMBER' &&
      !memberDone;

    return {
      user: publicUser,
      session: {
        needsRoleSelection,
        needsOwnerOnboarding,
        needsMemberOnboarding,
        onboardingCompleted,
        isStaff: false,
      },
    };
  }
}
