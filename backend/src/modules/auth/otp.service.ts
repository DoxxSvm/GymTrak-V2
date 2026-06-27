import {
  HttpException,
  HttpStatus,
  Injectable,
  Logger,
  OnModuleInit,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { OtpPurpose } from '@prisma/client';
import * as bcrypt from 'bcrypt';
import { randomInt } from 'crypto';
import { PrismaService } from '../prisma/prisma.service';
import { Msg91OtpService } from './msg91-otp.service';

const OTP_SALT_ROUNDS = 10;
const DEFAULT_STATIC_CODE = '123456';

@Injectable()
export class OtpService implements OnModuleInit {
  private readonly logger = new Logger(OtpService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
    private readonly msg91: Msg91OtpService,
  ) {}

  onModuleInit(): void {
    if (this.shouldUseMsg91()) {
      this.logger.log(
        'OTP delivery: MSG91 (SMS). Set IS_MSG91_ENABLED=false for static OTP in dev.',
      );
    } else if (!this.msg91.isRequested()) {
      this.logger.warn(
        `IS_MSG91_ENABLED=false — OTP uses static code "${this.resolveStaticCode()}" (no SMS).`,
      );
    } else if (this.msg91.isRequested() && !this.msg91.isEnabled()) {
      this.logger.warn(
        `IS_MSG91_ENABLED=true but MSG91 is not configured (MSG91_AUTH_KEY missing) — falling back to static code "${this.resolveStaticCode()}".`,
      );
    } else if (this.isStaticOtpEnabled()) {
      const hint =
        this.explicitOtpMode() != null
          ? 'OTP_MODE=static'
          : 'OTP_STATIC_ENABLED=true (legacy)';
      this.logger.warn(
        `${hint} — all challenges use fixed code "${this.resolveStaticCode()}" (dev/local only).`,
      );
    }
    if (
      this.isStaticOtpEnabled() &&
      this.config.get<string>('NODE_ENV') === 'production' &&
      !this.msg91.isRequested()
    ) {
      this.logger.error(
        'Static OTP in production is insecure. Set IS_MSG91_ENABLED=true and configure MSG91.',
      );
    }
  }

  /** Set `OTP_MODE=static` or `dynamic`. If unset, `OTP_STATIC_ENABLED=true` still selects static (legacy). */
  private explicitOtpMode(): 'static' | 'dynamic' | undefined {
    const raw = this.config.get<string>('OTP_MODE')?.trim().toLowerCase();
    if (!raw) return undefined;
    if (raw === 'static' || raw === 'dynamic') return raw;
    this.logger.warn(
      `Invalid OTP_MODE="${raw}"; expected static or dynamic. Falling back to legacy OTP_STATIC_ENABLED / dynamic.`,
    );
    return undefined;
  }

  private isStaticOtpEnabled(): boolean {
    if (this.shouldUseMsg91()) {
      return false;
    }
    if (!this.msg91.isRequested()) {
      return true;
    }
    if (this.msg91.isRequested() && !this.msg91.isEnabled()) {
      return true;
    }
    const explicit = this.explicitOtpMode();
    if (explicit === 'static') return true;
    if (explicit === 'dynamic') return false;
    const v = this.config.get<boolean | string>('OTP_STATIC_ENABLED');
    return v === true || v === 'true' || v === '1';
  }

  private resolveStaticCode(): string {
    const raw = this.config.get<string>('OTP_STATIC_CODE')?.trim();
    return raw && raw.length > 0 ? raw : DEFAULT_STATIC_CODE;
  }

  private shouldUseMsg91(): boolean {
    const skip = this.config.get<string | boolean>('MSG91_SKIP_SEND');
    if (skip === true || skip === 'true' || skip === '1') {
      return false;
    }
    return this.msg91.isEnabled();
  }

  private generatePlaintextCode(): string {
    if (this.isStaticOtpEnabled()) {
      return this.resolveStaticCode();
    }
    return String(randomInt(100000, 999999));
  }

  async createChallenge(
    phone: string,
    purpose: OtpPurpose,
  ): Promise<{ expiresAt: Date; resendAvailableAt: Date }> {
    const staticEnabled = this.isStaticOtpEnabled();
    const ttlSeconds = this.config.get<number>('OTP_CODE_TTL_SECONDS') ?? 300;
    const resendCooldownSec =
      this.config.get<number>('OTP_RESEND_COOLDOWN_SECONDS') ?? 45;
    const expiresAt = new Date(Date.now() + ttlSeconds * 1000);
    const resendAvailableAt = staticEnabled
      ? new Date()
      : new Date(Date.now() + resendCooldownSec * 1000);
    const maxAttempts = this.config.get<number>('OTP_MAX_ATTEMPTS') ?? 5;

    const useMsg91 = this.shouldUseMsg91();
    let code = '';
    let codeHash = '';
    let externalReqId: string | null = null;
    let externalChannel: string | null = null;

    const latest = await this.prisma.otpChallenge.findFirst({
      where: { phone, purpose },
      orderBy: { createdAt: 'desc' },
    });
    if (latest && !staticEnabled) {
      const nextAllowedAt = new Date(
        latest.createdAt.getTime() + resendCooldownSec * 1000,
      );
      if (nextAllowedAt > new Date()) {
        throw new HttpException(
          'Please wait before requesting another OTP',
          HttpStatus.TOO_MANY_REQUESTS,
        );
      }
    }

    if (useMsg91) {
      try {
        const sent = await this.msg91.sendOtp(phone);
        externalReqId = sent.requestId;
        externalChannel = sent.channel;
        codeHash = await bcrypt.hash('MSG91_EXTERNAL', OTP_SALT_ROUNDS);
      } catch (err) {
        throw err;
      }
    } else {
      code = this.generatePlaintextCode();
      codeHash = await bcrypt.hash(code, OTP_SALT_ROUNDS);
    }

    await this.prisma.$transaction(async (tx) => {
      await tx.otpChallenge.deleteMany({ where: { phone, purpose } });
      await tx.otpChallenge.create({
        data: {
          phone,
          purpose,
          codeHash,
          expiresAt,
          maxAttempts,
          externalReqId,
          externalChannel,
        },
      });
    });

    if (this.config.get<boolean>('OTP_DEV_LOG_CODE') && code) {
      this.logger.warn(`[DEV] OTP for ${phone} (${purpose}): ${code}`);
    }

    return { expiresAt, resendAvailableAt };
  }

  async verifyAndConsume(
    phone: string,
    code: string,
    purpose: OtpPurpose,
  ): Promise<{ valid: true } | { valid: false; reason: string }> {
    const challenge = await this.prisma.otpChallenge.findFirst({
      where: { phone, purpose },
      orderBy: { createdAt: 'desc' },
    });

    if (!challenge) {
      return { valid: false, reason: 'NO_CHALLENGE' };
    }
    if (challenge.expiresAt < new Date()) {
      await this.prisma.otpChallenge.delete({ where: { id: challenge.id } });
      return { valid: false, reason: 'EXPIRED' };
    }
    if (challenge.attempts >= challenge.maxAttempts) {
      throw new HttpException(
        'Too many OTP attempts',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    const useMsg91 =
      this.shouldUseMsg91() &&
      !!challenge.externalReqId &&
      !!challenge.externalChannel;

    let ok = false;
    if (useMsg91) {
      if (challenge.externalChannel === 'widget') {
        ok = await this.msg91.verifyOtp(phone, code, {
          requestId: challenge.externalReqId!,
          channel: 'widget',
        });
      } else {
        ok = await this.msg91.verifyOtp(phone, code, { channel: 'v5' });
      }
    } else {
      ok = await bcrypt.compare(code, challenge.codeHash);
    }

    if (!ok) {
      await this.prisma.otpChallenge.update({
        where: { id: challenge.id },
        data: { attempts: { increment: 1 } },
      });
      return { valid: false, reason: 'INVALID_CODE' };
    }

    await this.prisma.otpChallenge.delete({ where: { id: challenge.id } });
    return { valid: true };
  }
}
