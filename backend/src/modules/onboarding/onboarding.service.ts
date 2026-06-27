import {
  BadRequestException,
  ConflictException,
  Injectable,
} from '@nestjs/common';
import { AppOnboardingRole, Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { SessionService } from '../auth/session.service';
import type { AuthSession, PublicUser } from '../auth/types/auth-session.type';
import { GymsService } from '../gyms/gyms.service';

@Injectable()
export class OnboardingService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly session: SessionService,
    private readonly gyms: GymsService,
  ) {}

  async chooseRole(userId: string, role: AppOnboardingRole) {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
        ownedGyms: { select: { id: true }, take: 1 },
      },
    });

    if (user.onboardingCompletedAt) {
      throw new ConflictException('Onboarding already completed');
    }
    if (user.selectedOnboardingRole) {
      throw new ConflictException('Role already selected');
    }
    if (user.ownedGyms.length > 0) {
      throw new ConflictException('You already own a gym');
    }

    await this.prisma.user.update({
      where: { id: userId },
      data: { selectedOnboardingRole: role },
    });

    return this.snapshot(userId);
  }

  async completeOwner(userId: string, gymName: string) {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
        ownedGyms: { select: { id: true }, take: 1 },
      },
    });

    if (user.onboardingCompletedAt) {
      throw new ConflictException('Onboarding already completed');
    }
    if (user.selectedOnboardingRole !== AppOnboardingRole.OWNER) {
      throw new BadRequestException('Owner onboarding path required');
    }
    if (user.ownedGyms.length > 0) {
      throw new ConflictException('Gym already created');
    }

    await this.gyms.create(userId, { name: gymName.trim() });

    return this.snapshot(userId);
  }

  async completeMember(
    userId: string,
    body: { fullName: string; heightCm: number; weightKg: number },
  ) {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
      },
    });

    if (user.onboardingCompletedAt) {
      throw new ConflictException('Onboarding already completed');
    }
    if (user.selectedOnboardingRole !== AppOnboardingRole.MEMBER) {
      throw new BadRequestException('Member onboarding path required');
    }

    await this.prisma.user.update({
      where: { id: userId },
      data: {
        fullName: body.fullName.trim(),
        heightCm: new Prisma.Decimal(body.heightCm),
        weightKg: new Prisma.Decimal(body.weightKg),
        onboardingCompletedAt: new Date(),
      },
    });

    return this.snapshot(userId);
  }

  private async snapshot(userId: string): Promise<{
    session: AuthSession;
    user: PublicUser;
  }> {
    return this.session.buildSession(userId);
  }
}
