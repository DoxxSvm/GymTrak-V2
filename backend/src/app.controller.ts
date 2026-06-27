import {
  Controller,
  Get,
  NotFoundException,
  StreamableFile,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createReadStream, existsSync } from 'fs';
import { join } from 'path';
import { Public } from './common/decorators/public.decorator';
import { HealthService } from './modules/health/health.service';
import { WORKOUT_META_ALL } from './modules/workouts/workout-meta.constants';

@Controller()
export class AppController {
  constructor(
    private readonly healthService: HealthService,
    private readonly config: ConfigService,
  ) {}

  /** Public: DB ping + API metadata (for probes and ops). */
  @Public()
  @Get('health')
  getHealth() {
    return this.healthService.getOverallHealth();
  }

  /** Public: exercise enum labels + field hints for trainer/member apps. */
  @Public()
  @Get('meta/all')
  getWorkoutMetaAll() {
    return WORKOUT_META_ALL;
  }

  /** Public: Postman Collection v2.1 JSON (Import → Link in Postman). */
  @Public()
  @Get('reference/postman-collection.json')
  getPostmanCollection(): StreamableFile {
    const path = join(
      process.cwd(),
      'docs',
      'gymtrak-api.postman_collection.json',
    );
    if (!existsSync(path)) {
      throw new NotFoundException('Postman collection file is not available');
    }
    return new StreamableFile(createReadStream(path), {
      type: 'application/json',
    });
  }

  @Public()
  @Get(['app/config', 'config'])
  getAppConfig() {
    return {
      app_name: this.config.get<string>('APP_NAME') ?? 'GymTrak',
      app_version: this.config.get<string>('APP_VERSION') ?? '1.0.0',
      force_update: this.toBoolean(this.config.get<string>('APP_FORCE_UPDATE')),
      maintenance_mode: this.toBoolean(
        this.config.get<string>('APP_MAINTENANCE_MODE'),
      ),
      support_contact: this.config.get<string>('APP_SUPPORT_CONTACT') ?? '',
      default_country_code:
        this.config.get<string>('APP_DEFAULT_COUNTRY_CODE') ?? '+91',
      feature_flags: this.safeJson(
        this.config.get<string>('APP_FEATURE_FLAGS'),
        {},
      ),
      login_methods_enabled: this.safeJson(
        this.config.get<string>('APP_LOGIN_METHODS_ENABLED'),
        {
          phone: true,
          email: false,
          google: false,
          apple: false,
        },
      ),
      splash_assets: this.safeJson(
        this.config.get<string>('APP_SPLASH_ASSETS'),
        {},
      ),
    };
  }

  private toBoolean(value: string | undefined): boolean {
    return value === 'true' || value === '1';
  }

  private safeJson(
    value: string | undefined,
    fallback: Record<string, unknown>,
  ) {
    if (!value) {
      return fallback;
    }
    try {
      const parsed = JSON.parse(value);
      return parsed && typeof parsed === 'object' ? parsed : fallback;
    } catch {
      return fallback;
    }
  }
}
