import {
  BadRequestException,
  GoneException,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import {
  AppOnboardingRole,
  GlobalRole,
  GymRole,
  OtpPurpose,
} from '@prisma/client';
import * as bcrypt from 'bcrypt';
import { createHash, randomBytes } from 'crypto';
import { toE164Phone } from '../../common/utils/phone.util';
import { PrismaService } from '../prisma/prisma.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { OtpService } from './otp.service';
import { Msg91OtpService } from './msg91-otp.service';
import { SessionService } from './session.service';
import type { AuthResponse } from './types/auth-session.type';
import type { JwtPayload, JwtUser } from './types/jwt-user.type';
import { activePersonaFromOwnerRow } from './types/jwt-user.type';
import { LastActiveRole } from '@prisma/client';

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  accessExpiresInSeconds: number;
  refreshExpiresAt: Date;
}

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly jwt: JwtService,
    private readonly config: ConfigService,
    private readonly otp: OtpService,
    private readonly msg91: Msg91OtpService,
    private readonly session: SessionService,
    private readonly permissionEngine: PermissionEngineService,
  ) {}

  getMsg91Status() {
    return this.msg91.getStatus();
  }

  async testMsg91Send(phone: string, countryCode?: string) {
    const e164Phone = this.toE164(phone, countryCode);
    return this.msg91.sendTestOtp(e164Phone);
  }

  async retryMsg91Voice(phone: string, countryCode?: string) {
    const e164Phone = this.toE164(phone, countryCode);
    await this.msg91.retryOtp(e164Phone, { retryType: 'voice' });
    return {
      ok: true as const,
      e164: e164Phone,
      mobile: this.msg91.toMsg91Mobile(e164Phone),
      message: 'MSG91 voice OTP retry requested. Answer the call if SMS did not arrive.',
    };
  }

  async sendOtp(phone: string, purpose: OtpPurpose) {
    if (
      purpose !== OtpPurpose.PHONE_LOGIN &&
      purpose !== OtpPurpose.PHONE_VERIFY
    ) {
      throw new BadRequestException('Unsupported OTP purpose');
    }
    const { expiresAt, resendAvailableAt } = await this.otp.createChallenge(
      phone,
      purpose,
    );
    return { ok: true as const, expiresAt, resendAvailableAt };
  }

  async verifyOtp(
    phone: string,
    code: string,
    purpose: OtpPurpose,
  ): Promise<AuthResponse> {
    if (
      purpose !== OtpPurpose.PHONE_LOGIN &&
      purpose !== OtpPurpose.PHONE_VERIFY
    ) {
      throw new BadRequestException('Unsupported OTP purpose');
    }
    const result = await this.otp.verifyAndConsume(phone, code, purpose);
    if (!result.valid) {
      this.throwForOtpFailure(result.reason);
    }

    const user = await this.prisma.user.upsert({
      where: { phone },
      create: { phone, phoneVerified: true },
      update: { phoneVerified: true },
      select: { id: true, phone: true, globalRole: true },
    });

    const tokens = await this.issueTokenPair(user);
    return this.toAuthResponse(user.id, tokens);
  }

  async staffLogin(username: string, password: string): Promise<AuthResponse> {
    const user = await this.prisma.user.findFirst({
      where: { username: username.toLowerCase() },
      select: {
        id: true,
        phone: true,
        globalRole: true,
        passwordHash: true,
        status: true,
      },
    });
    if (!user?.passwordHash || user.status !== 'ACTIVE') {
      throw new UnauthorizedException('Invalid credentials');
    }

    const passwordOk = await bcrypt.compare(password, user.passwordHash);
    if (!passwordOk) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const staffMembership = await this.prisma.gymUser.findFirst({
      where: {
        userId: user.id,
        isActive: true,
        role: { in: [GymRole.STAFF, GymRole.TRAINER] },
      },
      orderBy: { joinedAt: 'asc' },
      select: { gymId: true },
    });
    if (!staffMembership && user.globalRole !== GlobalRole.SUPER_ADMIN) {
      throw new UnauthorizedException('Staff access required');
    }

    const approvedGymId =
      user.globalRole === GlobalRole.SUPER_ADMIN
        ? await this.getDefaultGymIdForUser(user.id)
        : await this.resolveApprovedStaffGymId(user.id, staffMembership?.gymId);
    if (!approvedGymId && user.globalRole !== GlobalRole.SUPER_ADMIN) {
      throw new UnauthorizedException(
        'No approved permissions assigned for this trainer/staff account',
      );
    }

    const tokens = await this.issueTokenPair(
      {
        id: user.id,
        phone: user.phone,
        globalRole: user.globalRole,
      },
      approvedGymId ?? undefined,
    );
    return this.toAuthResponse(user.id, tokens, approvedGymId ?? undefined);
  }

  async refresh(refreshToken: string): Promise<AuthResponse> {
    const tokenHash = this.hashOpaqueToken(refreshToken);
    const row = await this.prisma.refreshToken.findUnique({
      where: { tokenHash },
    });
    if (!row || row.revokedAt || row.expiresAt < new Date()) {
      throw new UnauthorizedException('Invalid refresh token');
    }

    await this.prisma.refreshToken.update({
      where: { id: row.id },
      data: { revokedAt: new Date() },
    });

    const user = await this.prisma.user.findUnique({
      where: { id: row.userId },
      select: { id: true, phone: true, globalRole: true, status: true },
    });
    if (!user || user.status !== 'ACTIVE') {
      throw new UnauthorizedException();
    }

    const tokens = await this.issueTokenPair(user);
    return this.toAuthResponse(user.id, tokens);
  }

  async getProfile(userId: string) {
    return this.session.buildSession(userId);
  }

  /**
   * POST /auth/login with phone: send OTP for any number; does not create a user.
   */
  async phoneLoginSendOtp(phone: string, countryCode?: string) {
    const e164Phone = this.toE164(phone, countryCode);
    const existing = await this.prisma.user.findUnique({
      where: { phone: e164Phone },
      select: { id: true, onboardingCompletedAt: true },
    });
    const isRegistered = !!existing && !!existing.onboardingCompletedAt;

    await this.otp.createChallenge(e164Phone, OtpPurpose.PHONE_LOGIN);
    this.logger.log(
      `Phone login OTP sent for ${e164Phone} registered=${isRegistered}`,
    );

    return {
      success: true as const,
      isRegistered,
      phone: e164Phone,
    };
  }

  /**
   * After OTP, create DB user for temp JWT (owner signup). Idempotent for same phone.
   */
  async getUserForTokenPair(userId: string) {
    return this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
      select: { id: true, phone: true, globalRole: true },
    });
  }

  async ensureUserForOwnerSignup(
    jwt: JwtUser,
  ): Promise<{ userId: string; wasTemp: boolean }> {
    if (!jwt.isTemp) {
      return { userId: jwt.sub, wasTemp: false };
    }
    const user = await this.prisma.user.upsert({
      where: { phone: jwt.phone },
      create: {
        phone: jwt.phone,
        status: 'ACTIVE',
        phoneVerified: true,
      },
      update: {
        phoneVerified: true,
        status: 'ACTIVE',
      },
      select: { id: true },
    });
    return { userId: user.id, wasTemp: true };
  }

  async signTempSignupToken(phone: string): Promise<string> {
    const exp = this.config.get<string>('JWT_TEMP_SIGNUP_EXPIRES_IN') ?? '12m';
    const payload: JwtPayload = {
      sub: phone,
      phone,
      globalRole: GlobalRole.USER,
      isTemp: true,
    };
    const expiresInSeconds = this.parseDurationToSeconds(exp);
    return this.jwt.signAsync(payload, { expiresIn: expiresInSeconds });
  }

  async sendOwnerAppOtp(phone: string, countryCode?: string) {
    const e164Phone = this.toE164(phone, countryCode);
    const owner = await this.prisma.user.findFirst({
      where: {
        phone: e164Phone,
        status: 'ACTIVE',
        OR: [{ ownedGyms: { some: {} } }, { selectedOnboardingRole: 'OWNER' }],
      },
      select: { id: true },
    });
    if (!owner) {
      throw new UnauthorizedException('Gym owner account not found');
    }

    const { expiresAt } = await this.otp.createChallenge(
      e164Phone,
      OtpPurpose.PHONE_LOGIN,
    );
    this.logger.log(`OTP send attempt success for ${e164Phone}`);

    return {
      success: true as const,
      message: 'OTP sent successfully',
      expires_in: Math.max(
        1,
        Math.floor((expiresAt.getTime() - Date.now()) / 1000),
      ),
    };
  }

  async verifyOwnerAppOtp(phone: string, otp: string, countryCode?: string) {
    const e164Phone = this.toE164(phone, countryCode);
    const verification = await this.otp.verifyAndConsume(
      e164Phone,
      otp,
      OtpPurpose.PHONE_LOGIN,
    );
    if (!verification.valid) {
      this.logger.warn(
        `OTP verification failed for ${e164Phone}. reason=${verification.reason}`,
      );
      this.throwForOtpFailure(verification.reason);
    }

    const user = await this.prisma.user.findFirst({
      where: { phone: e164Phone },
      select: {
        id: true,
        phone: true,
        globalRole: true,
        fullName: true,
        status: true,
        selectedOnboardingRole: true,
        onboardingCompletedAt: true,
        lastActiveRole: true,
        ownedGyms: { select: { id: true }, take: 1 },
        memberProfile: { select: { id: true } },
      },
    });

    if (!user) {
      const tempToken = await this.signTempSignupToken(e164Phone);
      this.logger.log(`OTP verified new phone ${e164Phone} → temp token`);
      return {
        success: true as const,
        registration_state: 'new_signup_required' as const,
        isRegistered: false as const,
        tempToken,
        phone: e164Phone,
        gym_id: null,
      };
    }

    if (user.status !== 'ACTIVE') {
      throw new UnauthorizedException('Account is not active');
    }

    const tokens = await this.issueTokenPair(user);
    let role = await this.resolveMobileAppRole(
      user.id,
      user.globalRole,
      user.selectedOnboardingRole as AppOnboardingRole,
      user.lastActiveRole
    );
    // Gym owners: `resolveMobileAppRole` can yield `trainer` when `selectedOnboardingRole`
    // is unset; always derive owner-app persona from `lastActiveRole` + `member_profiles`.
    if (user.ownedGyms.length > 0) {
      const persona = activePersonaFromOwnerRow({
        lastActiveRole: user.lastActiveRole,
        ownedGyms: user.ownedGyms,
        memberProfile: user.memberProfile,
      });
      role = persona === 'member' ? 'member' : 'gym_owner';
    }
    const gym_id = await this.getDefaultGymIdForUser(user.id);
    await this.prisma.user.update({
      where: { id: user.id },
      data: { phoneVerified: true },
    });
    this.logger.log(`OTP verification success for ${e164Phone}`);

    if (
      role === 'member' &&
      !user.onboardingCompletedAt &&
      user.ownedGyms.length === 0
    ) {
      return {
        success: true as const,
        registration_state: 'onboarding_required' as const,
        isRegistered: false as const,
        app_role: role,
        access_token: tokens.accessToken,
        refresh_token: tokens.refreshToken,
        gym_id,
        user: {
          id: user.id,
          name: user.fullName ?? '',
          phone: user.phone,
          role,
        },
      };
    }

    return {
      success: true as const,
      registration_state: 'registered' as const,
      isRegistered: true as const,
      app_role: role,
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
      gym_id,
      user: {
        id: user.id,
        name: user.fullName ?? '',
        phone: user.phone,
        role,
      },
    };
  }

  private async resolveMobileAppRole(
    userId: string,
    globalRole: GlobalRole,
    selectedOnboardingRole?: AppOnboardingRole,
    lastActiveRole?: LastActiveRole,
  ): Promise<'gym_owner' | 'trainer' | 'super_admin' | 'owner' | 'member'> {

    if (selectedOnboardingRole === 'OWNER') {
      return lastActiveRole === 'OWNER' ? 'gym_owner' : 'member';
    }
    else if (globalRole === GlobalRole.SUPER_ADMIN) {
      return 'super_admin';
    } else if (globalRole === GlobalRole.USER) {
      return selectedOnboardingRole === 'MEMBER' ? 'member' : 'trainer';
    }
    const trainer = await this.prisma.gymUser.findFirst({
      where: { userId, isActive: true, role: GymRole.TRAINER },
      select: { id: true },
    });
    if (trainer) {
      return 'trainer';
    }
    return 'gym_owner';
  }

  async mobileLogin(username: string, password: string) {
    this.logger.log(`Login attempt for username=${username}`);
    const user = await this.prisma.user.findFirst({
      where: { username: username.toLowerCase() },
      select: {
        id: true,
        phone: true,
        globalRole: true,
        fullName: true,
        passwordHash: true,
        status: true,
      },
    });
    if (!user?.passwordHash || user.status !== 'ACTIVE') {
      this.logger.warn(`Login failed for username=${username}`);
      throw new UnauthorizedException('Invalid credentials');
    }

    const passwordOk = await bcrypt.compare(password, user.passwordHash);
    if (!passwordOk) {
      this.logger.warn(`Login failed for username=${username}`);
      throw new UnauthorizedException('Invalid credentials');
    }

    const gymMembership = await this.prisma.gymUser.findFirst({
      where: {
        userId: user.id,
        isActive: true,
        role: { in: [GymRole.TRAINER, GymRole.OWNER] },
      },
      orderBy: { joinedAt: 'asc' },
      select: { role: true, gymId: true },
    });
    const isOwner = await this.prisma.gym.findFirst({
      where: { ownerId: user.id },
      select: { id: true },
    });

    if (
      !gymMembership &&
      !isOwner &&
      user.globalRole !== GlobalRole.SUPER_ADMIN
    ) {
      this.logger.warn(`Login denied by role for username=${username}`);
      throw new UnauthorizedException('Trainer or gym owner access required');
    }

    const approvedGymIdForTrainer =
      gymMembership?.role === GymRole.TRAINER
        ? await this.resolveApprovedStaffGymId(user.id, gymMembership.gymId)
        : null;

    const gymId =
      approvedGymIdForTrainer ?? (await this.getDefaultGymIdForUser(user.id));
    const tokens = await this.issueTokenPair(user);
    const role =
      user.globalRole === GlobalRole.SUPER_ADMIN
        ? 'super_admin'
        : gymMembership?.role === GymRole.TRAINER
          ? 'trainer'
          : 'gym_owner';
    this.logger.log(`Login success for username=${username}`);
    return {
      success: true as const,
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
      gymId: gymId,
      user: {
        id: user.id,
        name: user.fullName ?? '',
        role,
      },
    };
  }

  async logout(refreshToken: string) {
    const tokenHash = this.hashOpaqueToken(refreshToken);
    await this.prisma.refreshToken.updateMany({
      where: { tokenHash, revokedAt: null },
      data: { revokedAt: new Date() },
    });
    return { success: true as const, message: 'Logged out successfully' };
  }

  private async toAuthResponse(
    userId: string,
    tokens: TokenPair,
    gymIdOverride?: string | null,
  ): Promise<AuthResponse> {
    const { session, user } = await this.session.buildSession(userId);
    const gymId =
      gymIdOverride !== undefined
        ? gymIdOverride
        : await this.getDefaultGymIdForUser(userId);
    return { ...tokens, session, user, gymId };
  }

  /**
   * First gym to use after login: owned gym, else first active trainer/staff/member row.
   * Public for JWT strategy (legacy tokens without `gymId` claim).
   */
  async getDefaultGymIdForUser(userId: string): Promise<string | null> {
    const owned = await this.prisma.gym.findFirst({
      where: { ownerId: userId },
      orderBy: { name: 'asc' },
      select: { id: true },
    });
    if (owned) {
      return owned.id;
    }
    const staffRow = await this.prisma.gymUser.findFirst({
      where: {
        userId,
        isActive: true,
        role: { in: [GymRole.TRAINER, GymRole.STAFF] },
      },
      orderBy: { joinedAt: 'asc' },
      select: { gymId: true },
    });
    if (staffRow) {
      return staffRow.gymId;
    }
    const memberRow = await this.prisma.gymUser.findFirst({
      where: {
        userId,
        isActive: true,
        role: GymRole.MEMBER,
      },
      orderBy: { joinedAt: 'asc' },
      select: { gymId: true },
    });
    return memberRow?.gymId ?? null;
  }

  /**
   * Persona encoded into JWT on login/refresh/switch. Heals `lastActiveRole=MEMBER` without a profile row.
   */
  async resolveActiveAppPersonaForIssue(
    userId: string,
  ): Promise<JwtPayload['activeAppRole']> {
    const row = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        lastActiveRole: true,
        ownedGyms: { select: { id: true }, take: 1 },
        memberProfile: { select: { id: true } },
      },
    });
    if (!row || row.ownedGyms.length === 0) return undefined;
    if (row.lastActiveRole === 'MEMBER' && !row.memberProfile) {
      await this.prisma.user.update({
        where: { id: userId },
        data: { lastActiveRole: 'OWNER' },
      });
      return 'owner';
    }
    return activePersonaFromOwnerRow({
      lastActiveRole: row.lastActiveRole,
      ownedGyms: row.ownedGyms,
      memberProfile: row.memberProfile,
    });
  }

  async issueTokenPair(
    user: {
      id: string;
      phone: string;
      globalRole: JwtPayload['globalRole'];
    },
    gymIdOverride?: string | null,
  ): Promise<TokenPair> {
    const accessExpiresIn =
      this.config.get<string>('JWT_ACCESS_EXPIRES_IN') ?? '15m';
    const refreshExpiresIn =
      this.config.get<string>('JWT_REFRESH_EXPIRES_IN') ?? '7d';

    const accessExpiresInSeconds = this.parseDurationToSeconds(accessExpiresIn);
    const refreshMs = this.parseDurationToMs(refreshExpiresIn);
    const refreshExpiresAt = new Date(Date.now() + refreshMs);

    const gymId =
      gymIdOverride !== undefined
        ? gymIdOverride
        : await this.getDefaultGymIdForUser(user.id);
    const activeAppRole = await this.resolveActiveAppPersonaForIssue(user.id);
    const payload: JwtPayload = {
      sub: user.id,
      phone: user.phone,
      globalRole: user.globalRole,
      gymId,
      ...(activeAppRole !== undefined ? { activeAppRole } : {}),
    };

    const accessToken = await this.jwt.signAsync(payload);

    const refreshRaw = randomBytes(48).toString('base64url');
    const tokenHash = this.hashOpaqueToken(refreshRaw);

    await this.prisma.refreshToken.create({
      data: {
        tokenHash,
        userId: user.id,
        expiresAt: refreshExpiresAt,
      },
    });

    return {
      accessToken,
      refreshToken: refreshRaw,
      accessExpiresInSeconds,
      refreshExpiresAt,
    };
  }

  private async resolveApprovedStaffGymId(
    userId: string,
    preferredGymId?: string | null,
  ): Promise<string | null> {
    const memberships = await this.prisma.gymUser.findMany({
      where: {
        userId,
        isActive: true,
        role: { in: [GymRole.TRAINER, GymRole.STAFF] },
      },
      orderBy: { joinedAt: 'asc' },
      select: { gymId: true },
    });
    if (memberships.length === 0) {
      return null;
    }

    const orderedGymIds = [
      ...(preferredGymId ? [preferredGymId] : []),
      ...memberships.map((m) => m.gymId),
    ].filter((v, i, arr) => !!v && arr.indexOf(v) === i);

    for (const gymId of orderedGymIds) {
      const eff = await this.permissionEngine.getEffective(userId, gymId);
      if (Object.values(eff.effective).some(Boolean)) {
        return gymId;
      }
    }
    // Active trainer/staff at this gym but no RBAC rows yet (or only non-matrix
    // permissions): still attach a gym so login and session bootstrap succeed.
    return orderedGymIds[0] ?? null;
  }

  private hashOpaqueToken(token: string): string {
    return createHash('sha256').update(token).digest('hex');
  }

  private toE164(phone: string, countryCode?: string): string {
    try {
      return toE164Phone(phone, countryCode?.trim() || '+91');
    } catch {
      throw new BadRequestException('Invalid phone number');
    }
  }

  /** Parses simple Nest/JWT duration strings: 15m, 7d, 24h, 3600s */
  private parseDurationToSeconds(value: string): number {
    return Math.floor(this.parseDurationToMs(value) / 1000);
  }

  private parseDurationToMs(value: string): number {
    const match = /^(\d+)(ms|s|m|h|d)$/.exec(value.trim());
    if (!match) {
      return 7 * 24 * 60 * 60 * 1000;
    }
    const n = Number(match[1]);
    const unit = match[2];
    switch (unit) {
      case 'ms':
        return n;
      case 's':
        return n * 1000;
      case 'm':
        return n * 60 * 1000;
      case 'h':
        return n * 60 * 60 * 1000;
      case 'd':
        return n * 24 * 60 * 60 * 1000;
      default:
        return 7 * 24 * 60 * 60 * 1000;
    }
  }

  /**
   * Maps {@link OtpService.verifyAndConsume} failure reasons to distinct HTTP status + message.
   * Expired: 410 Gone. Wrong code: 400 Bad Request. Missing/used challenge: 400.
   */
  private throwForOtpFailure(reason: string): never {
    switch (reason) {
      case 'EXPIRED':
        throw new GoneException('OTP expired');
      case 'INVALID_CODE':
        throw new BadRequestException('Invalid OTP');
      case 'NO_CHALLENGE':
      default:
        throw new BadRequestException(
          'No active OTP. Please request a new code.',
        );
    }
  }
}
