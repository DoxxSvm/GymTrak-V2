import {
  BadRequestException,
  ConflictException,
  Injectable,
  Logger,
} from '@nestjs/common';
import { AppOnboardingRole, GymRole, Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { SessionService } from '../auth/session.service';
import type { AuthSession, PublicUser } from '../auth/types/auth-session.type';
import { GymsService } from '../gyms/gyms.service';
import { WhatsAppAutomationService } from '../messaging/whatsapp-automation.service';
import type { MemberOnboardingDto } from './dto/member-onboarding.dto';
import {
  computeMemberWellness,
  type MemberWellnessResult,
} from './member-wellness.util';

@Injectable()
export class OnboardingService {
  private readonly logger = new Logger(OnboardingService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly session: SessionService,
    private readonly gyms: GymsService,
    private readonly whatsapp: WhatsAppAutomationService,
  ) {}

  async chooseRole(userId: string, role: AppOnboardingRole) {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: {
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
        ownedGyms: { select: { id: true }, take: 1 },
        phone: true,
      },
    });

    if (user.onboardingCompletedAt) {
      throw new ConflictException('Onboarding already completed');
    }
    if (user.selectedOnboardingRole === role) {
      return this.snapshot(userId);
    }
    if (user.selectedOnboardingRole) {
      throw new ConflictException(
        'A different role is already selected. Complete onboarding or use the same role to refresh your session.',
      );
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
    body: MemberOnboardingDto,
  ): Promise<{
    session: AuthSession;
    user: PublicUser;
    wellness: MemberWellnessResult;
  }> {
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

    const wellness = computeMemberWellness({
      heightCm: body.heightCm,
      weightKg: body.weightKg,
      ageYears: body.ageYears,
      gender: body.gender,
      activityLevel: body.activityLevel,
    });

    await this.prisma.user.update({
      where: { id: userId },
      data: {
        wellness: wellness as unknown as Prisma.InputJsonValue,
        fullName: body.fullName.trim(),
        heightCm: new Prisma.Decimal(body.heightCm),
        weightKg: new Prisma.Decimal(body.weightKg),
        onboardingCompletedAt: new Date(),
        ...(body.ageYears != null ? { ageYears: body.ageYears } : {}),
        ...(body.gender != null ? { gender: body.gender } : {}),
        ...(body.activityLevel != null
          ? { activityLevel: body.activityLevel }
          : {}),
        ...(body.fitnessGoal != null && body.fitnessGoal.trim().length > 0
          ? { fitnessGoal: body.fitnessGoal.trim() }
          : {}),
      },
    });

    const snapshot = await this.snapshot(userId);
    await this.enqueueWelcomeForGymMemberships(userId);
    return { ...snapshot, wellness };
  }

  /**
   * Onboarding welcome WhatsApp for each active gym membership (per-gym template toggle).
   */
  private async enqueueWelcomeForGymMemberships(userId: string): Promise<void> {
    const memberships = await this.prisma.gymUser.findMany({
      where: { userId, role: GymRole.MEMBER, isActive: true },
      select: { gymId: true },
      distinct: ['gymId'],
    });
    for (const { gymId } of memberships) {
      void this.whatsapp.enqueueWelcome(gymId, userId).catch((err) => {
        this.logger.warn(
          `Welcome WhatsApp enqueue failed (gym=${gymId}): ${(err as Error).message}`,
        );
      });
    }
  }

  private async snapshot(userId: string): Promise<{
    session: AuthSession;
    user: PublicUser;
  }> {
    return this.session.buildSession(userId);
  }
}
