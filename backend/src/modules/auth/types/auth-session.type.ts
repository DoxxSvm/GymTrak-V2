import type { AppOnboardingRole } from '@prisma/client';

export interface AuthSession {
  needsRoleSelection: boolean;
  needsOwnerOnboarding: boolean;
  needsMemberOnboarding: boolean;
  onboardingCompleted: boolean;
  /** Trainer/staff — skip consumer onboarding flows */
  isStaff: boolean;
}

export interface PublicUser {
  id: string;
  phone: string;
  fullName: string | null;
  selectedOnboardingRole: AppOnboardingRole | null;
  onboardingCompletedAt: Date | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessExpiresInSeconds: number;
  refreshExpiresAt: Date;
  session: AuthSession;
  user: PublicUser;
  /** Default gym for dashboard/API calls: owned gym first, else first trainer/staff/member gym */
  gymId: string | null;
}
