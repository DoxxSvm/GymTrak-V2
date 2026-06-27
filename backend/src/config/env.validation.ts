import { plainToInstance, Transform, Type } from 'class-transformer';
import { normalizeFirebasePrivateKey } from '../common/firebase/firebase-credentials.util';
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

  /** Plaintext OTP used in static mode (default 123456). */
  @IsOptional()
  @IsString()
  OTP_STATIC_CODE?: string;

  /**
   * Master switch: `true` → send OTP via MSG91 SMS; `false` → static `OTP_STATIC_CODE` (default 123456).
   * When unset, falls back to `OTP_PROVIDER=msg91`.
   */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['IS_MSG91_ENABLED'];
    if (v === true || v === 'true' || v === '1') return true;
    if (v === false || v === 'false' || v === '0') return false;
    return undefined;
  })
  @IsBoolean()
  IS_MSG91_ENABLED?: boolean;

  /** `local` (default bcrypt OTP) or `msg91` (SMS via MSG91). */
  @IsOptional()
  @ValidateIf((_, v) => v != null && String(v).trim() !== '')
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['OTP_PROVIDER'];
    if (v == null || v === '') return undefined;
    return String(v).trim().toLowerCase();
  })
  @IsIn(['local', 'msg91'])
  OTP_PROVIDER?: 'local' | 'msg91';

  @IsOptional()
  @IsString()
  MSG91_AUTH_KEY?: string;

  @IsOptional()
  @IsString()
  MSG91_WIDGET_ID?: string;

  /** Widget token from MSG91 dashboard (required when MSG91_USE_WIDGET=true). */
  @IsOptional()
  @IsString()
  MSG91_TOKEN_AUTH?: string;

  /** Use widget sendOtpMobile API instead of v5 /otp (enable Mobile Integration on widget). */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['MSG91_USE_WIDGET'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  MSG91_USE_WIDGET?: boolean;

  /** Optional MSG91 template id; required for reliable India DLT SMS delivery. */
  @IsOptional()
  @IsString()
  MSG91_TEMPLATE_ID?: string;

  /** DLT content template id from Indian DLT portal (mapped in MSG91). */
  @IsOptional()
  @IsString()
  MSG91_DLT_TEMPLATE_ID?: string;

  /** MSG91 sender / header id (when required by your template). */
  @IsOptional()
  @IsString()
  MSG91_SENDER_ID?: string;

  /** OTP validity in minutes (MSG91 `otp_expiry`). */
  @IsOptional()
  @Transform(({ value }) => {
    if (value === undefined || value === '') return undefined;
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
  })
  MSG91_OTP_EXPIRY_MINUTES?: number;

  /** Number of digits in MSG91 OTP (4–9, default 6). */
  @IsOptional()
  @Transform(({ value }) => {
    if (value === undefined || value === '') return undefined;
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
  })
  MSG91_OTP_LENGTH?: number;

  /** When true, refuse send if MSG91_TEMPLATE_ID is unset. */
  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['MSG91_REQUIRE_TEMPLATE_ID'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  MSG91_REQUIRE_TEMPLATE_ID?: boolean;

  @IsOptional()
  @Transform(({ obj }: { obj: Record<string, unknown> }) => {
    const v = obj['MSG91_SKIP_SEND'];
    return v === true || v === 'true' || v === '1';
  })
  @IsBoolean()
  MSG91_SKIP_SEND?: boolean;

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

  /** Optional Swagger server entry for local/dev testing (no trailing slash) */
  @IsOptional()
  @IsString()
  SWAGGER_LOCAL_SERVER_URL?: string;

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

  /** Header image URL for Meta onboarding template */
  @IsOptional()
  @IsString()
  WHATSAPP_ONBOARDING_HEADER_IMAGE_URL?: string;

  /** Override Meta template name for onboarding (default: onboarding) */
  @IsOptional()
  @IsString()
  WHATSAPP_TEMPLATE_ONBOARDING_NAME?: string;

  /** Override Meta template name for payment confirmation (default: payment_confirmation_gt) */
  @IsOptional()
  @IsString()
  WHATSAPP_TEMPLATE_PAYMENT_NAME?: string;

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

  /** Firebase service account JSON (single line). Alternative: discrete FIREBASE_* vars or FIREBASE_SERVICE_ACCOUNT_PATH. */
  @IsOptional()
  @IsString()
  FIREBASE_SERVICE_ACCOUNT_JSON?: string;

  /** Path to Firebase service account JSON file (server filesystem). */
  @IsOptional()
  @IsString()
  FIREBASE_SERVICE_ACCOUNT_PATH?: string;

  /** Discrete service account fields (same names as JSON keys, upper snake). Prefer with FIREBASE_PRIVATE_KEY. */
  @IsOptional()
  @IsString()
  FIREBASE_TYPE?: string;

  @IsOptional()
  @IsString()
  FIREBASE_PROJECT_ID?: string;

  @IsOptional()
  @IsString()
  FIREBASE_PRIVATE_KEY_ID?: string;

  @IsOptional()
  @Transform(({ value }) => {
    if (value == null || String(value).trim() === '') return value;
    return normalizeFirebasePrivateKey(String(value)) ?? value;
  })
  @IsString()
  FIREBASE_PRIVATE_KEY?: string;

  @IsOptional()
  @IsString()
  FIREBASE_CLIENT_EMAIL?: string;

  @IsOptional()
  @IsString()
  FIREBASE_CLIENT_ID?: string;

  @IsOptional()
  @IsString()
  FIREBASE_AUTH_URI?: string;

  @IsOptional()
  @IsString()
  FIREBASE_TOKEN_URI?: string;

  @IsOptional()
  @IsString()
  FIREBASE_AUTH_PROVIDER_CERT_URL?: string;

  @IsOptional()
  @IsString()
  FIREBASE_CLIENT_CERT_URL?: string;

  @IsOptional()
  @IsString()
  FIREBASE_UNIVERSE_DOMAIN?: string;
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
