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

const OTP_SALT_ROUNDS = 10;
const DEFAULT_STATIC_CODE = '12345';

@Injectable()
export class OtpService implements OnModuleInit {
  private readonly logger = new Logger(OtpService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
  ) {}

  onModuleInit(): void {
    if (this.isStaticOtpEnabled()) {
      const hint =
        this.explicitOtpMode() != null
          ? 'OTP_MODE=static'
          : 'OTP_STATIC_ENABLED=true (legacy)';
      this.logger.warn(
        `${hint} — all challenges use fixed code "${this.resolveStaticCode()}" (dev/local only).`,
      );
      if (this.config.get<string>('NODE_ENV') === 'production') {
        this.logger.error(
          'Static OTP in production is insecure. Set OTP_MODE=dynamic (and OTP_STATIC_ENABLED=false).',
        );
      }
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
    const code = this.generatePlaintextCode();
    const codeHash = await bcrypt.hash(code, OTP_SALT_ROUNDS);

    await this.prisma.$transaction(async (tx) => {
      const latest = await tx.otpChallenge.findFirst({
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
      await tx.otpChallenge.deleteMany({ where: { phone, purpose } });
      await tx.otpChallenge.create({
        data: {
          phone,
          purpose,
          codeHash,
          expiresAt,
          maxAttempts,
        },
      });
    });

    if (this.config.get<boolean>('OTP_DEV_LOG_CODE')) {
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

    const ok = await bcrypt.compare(code, challenge.codeHash);

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
