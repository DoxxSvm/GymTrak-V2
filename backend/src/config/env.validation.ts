import { plainToInstance, Transform, Type } from 'class-transformer';
import {
  IsBoolean,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  Min,
  ValidateIf,
  validateSync,
} from 'class-validator';

class EnvironmentVariables {
  @IsOptional()
  @IsString()
  NODE_ENV?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(65535)
  PORT?: number;

  @IsNotEmpty()
  @IsString()
  DATABASE_URL: string;

  @IsNotEmpty()
  @IsString()
  JWT_ACCESS_SECRET: string;

  @IsNotEmpty()
  @IsString()
  JWT_REFRESH_SECRET: string;

  @IsOptional()
  @IsString()
  JWT_ACCESS_EXPIRES_IN?: string;

  @IsOptional()
  @IsString()
  JWT_REFRESH_EXPIRES_IN?: string;

  /** e.g. 12m — temp JWT after verify-otp for unregistered phones */
  @IsOptional()
  @IsString()
  JWT_TEMP_SIGNUP_EXPIRES_IN?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(60)
  @Max(3600)
  OTP_CODE_TTL_SECONDS?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(15)
  @Max(300)
  OTP_RESEND_COOLDOWN_SECONDS?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(3)
  @Max(10)
  OTP_MAX_ATTEMPTS?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(180)
  SUBSCRIPTION_MAX_FREEZE_DAYS?: number;

  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['OTP_DEV_LOG_CODE'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  OTP_DEV_LOG_CODE?: boolean;

  /**
   * `dynamic` (default) = random 6-digit code per challenge.
   * `static` = fixed code from OTP_STATIC_CODE (local/dev only).
   * If unset, OTP_STATIC_ENABLED=true still enables static mode (legacy).
   */
  @IsOptional()
  @ValidateIf((_, v) => v != null && String(v).trim() !== '')
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['OTP_MODE'];
    if (v == null || v === '') return undefined;
    return String(v).trim().toLowerCase();
  })
  @IsIn(['static', 'dynamic'])
  OTP_MODE?: 'static' | 'dynamic';

  /**
   * Legacy: when true and OTP_MODE is unset, same as OTP_MODE=static.
   * Ignored when OTP_MODE is explicitly static or dynamic.
   */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['OTP_STATIC_ENABLED'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  OTP_STATIC_ENABLED?: boolean;

  /** Plaintext OTP used in static mode (default 12345). */
  @IsOptional()
  @IsString()
  OTP_STATIC_CODE?: string;

  @IsOptional()
  @IsString()
  REDIS_URL?: string;

  /** When true, do not run `prisma migrate deploy` on startup (use if CI runs migrations). */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['SKIP_MIGRATIONS_ON_STARTUP'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  SKIP_MIGRATIONS_ON_STARTUP?: boolean;

  /** When true, skip BullMQ/WhatsApp queue (Nest starts without Redis). */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['DISABLE_BULLMQ'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  DISABLE_BULLMQ?: boolean;

  /** Base URL for deep links in gym QR (no trailing slash) */
  @IsOptional()
  @IsString()
  APP_PUBLIC_URL?: string;

  /** `meta` (default) or `twilio` */
  @IsOptional()
  @IsString()
  WHATSAPP_PROVIDER?: string;

  /** Meta WhatsApp Cloud API — optional; messages are skipped when unset */
  @IsOptional()
  @IsString()
  WHATSAPP_ACCESS_TOKEN?: string;

  @IsOptional()
  @IsString()
  WHATSAPP_PHONE_NUMBER_ID?: string;

  @IsOptional()
  @IsString()
  TWILIO_ACCOUNT_SID?: string;

  @IsOptional()
  @IsString()
  TWILIO_AUTH_TOKEN?: string;

  /** e.g. whatsapp:+14155238886 */
  @IsOptional()
  @IsString()
  TWILIO_WHATSAPP_FROM?: string;

  @IsOptional()
  @IsString()
  APP_NAME?: string;

  @IsOptional()
  @IsString()
  APP_VERSION?: string;

  @IsOptional()
  @IsString()
  APP_FORCE_UPDATE?: string;

  @IsOptional()
  @IsString()
  APP_MAINTENANCE_MODE?: string;

  @IsOptional()
  @IsString()
  APP_SUPPORT_CONTACT?: string;

  @IsOptional()
  @IsString()
  APP_DEFAULT_COUNTRY_CODE?: string;

  @IsOptional()
  @IsString()
  APP_FEATURE_FLAGS?: string;

  @IsOptional()
  @IsString()
  APP_LOGIN_METHODS_ENABLED?: string;

  @IsOptional()
  @IsString()
  APP_SPLASH_ASSETS?: string;

  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['WHATSAPP_SKIP_SEND'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  WHATSAPP_SKIP_SEND?: boolean;

  /**
   * When true, protected routes accept requests without `Authorization: Bearer` and attach a user
   * from AUTH_DEV_USER_ID, or in development the first ACTIVE user in the DB.
   * When unset, NODE_ENV=development implies the same relaxed behavior (Bearer optional).
   */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['DISABLE_JWT_AUTH'];
    if (v === undefined || v === '') return undefined;
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  DISABLE_JWT_AUTH?: boolean;

  /** User id (cuid) used when JWT auth is disabled and no Bearer token is sent. */
  @IsOptional()
  @IsString()
  AUTH_DEV_USER_ID?: string;
}

export function validateEnv(config: Record<string, unknown>) {
  const validated = plainToInstance(EnvironmentVariables, config, {
    enableImplicitConversion: true,
  });
  const errors = validateSync(validated, {
    skipMissingProperties: false,
  });
  if (errors.length > 0) {
    throw new Error(errors.toString());
  }
  return validated;
}
