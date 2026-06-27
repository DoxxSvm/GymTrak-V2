import 'dotenv/config';

import { BadRequestException, Logger, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import type { INestApplication } from '@nestjs/common';
import type { NestExpressApplication } from '@nestjs/platform-express';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import type { OpenAPIObject } from '@nestjs/swagger';
import type { Request, Response } from 'express';
import { existsSync, readFileSync } from 'fs';
import { join } from 'path';
import { AppModule } from './app.module';
import { runPreBootstrapTasks } from './config/startup-tasks';
import { createNestLogger } from './config/nest-logger';
import { logBootstrapError, logStartupSummary } from './config/startup-info';
import { GymContextInterceptor } from './common/interceptors/gym-context.interceptor';
import { AuditContextInterceptor } from './modules/audit/audit-context.interceptor';
import { HealthService } from './modules/health/health.service';
import type { ValidationError } from 'class-validator';

function resolveBundledOpenApiPath(): string | null {
  const file = 'gymtrak-api.openapi.json';
  const candidates = [
    join(process.cwd(), 'docs', file),
    join(__dirname, '..', 'docs', file),
  ];
  for (const p of candidates) {
    if (existsSync(p)) {
      return p;
    }
  }
  return null;
}

/** Merge record keys; `preferSecond` wins on duplicate keys. */
function mergeOpenApiRecordSections<T extends Record<string, unknown>>(
  a: T | undefined,
  b: T | undefined,
  preferSecond: boolean,
): T | undefined {
  if (!a) return b;
  if (!b) return a;
  return (preferSecond ? { ...a, ...b } : { ...b, ...a }) as T;
}

/**
 * Nest’s Swagger doc uses `useGlobalPrefix: true`, so path keys are like
 * `/api/v1/members/...` while the bundled `gymtrak-api.openapi.json` uses
 * `/members/...` (base URL is the server’s `/api/v1`). Without normalizing, new
 * routes (e.g. `GET /members/workouts/history`) never pass the allowlist and
 * disappear from Swagger when a bundle is merged.
 */
function stripApiV1PrefixForOpenApiPath(path: string): string {
  return path.replace(/^\/api\/v1(?=\/)/, '') || path;
}

/** Only these generated paths are spliced in (avoids duplicating routes already documented in the bundle). */
function isMemberPersonalCatalogGeneratedPath(path: string): boolean {
  const p = stripApiV1PrefixForOpenApiPath(path);
  return (
    p === '/members/exercises' ||
    p.startsWith('/members/exercises/') ||
    p === '/members/workouts' ||
    p.startsWith('/members/workouts/') ||
    p === '/members/statistics'
  );
}

/**
 * When a bundled `gymtrak-api.openapi.json` exists, Swagger would otherwise skip
 * `SwaggerModule.createDocument` entirely — Nest-decorated routes (e.g.
 * `/members/exercises`) would be missing.
 *
 * Only paths under `/members/exercises` and `/members/workouts` are taken from
 * the generated doc (they are not in the hand-maintained bundle). Everything
 * else stays bundle-only so endpoints are not listed twice. `components` are
 * merged so `$ref`s from those paths resolve; bundled wins on schema name
 * collision. The bundle’s `tags` array is left unchanged to avoid duplicate
 * tag groups in Swagger UI.
 */
function mergeBundledOpenApiWithGenerated(
  bundled: OpenAPIObject,
  generated: OpenAPIObject,
): OpenAPIObject {
  const mergedPaths: Record<string, unknown> = {
    ...(bundled.paths as Record<string, unknown> | undefined),
  };
  const genPaths = generated.paths as Record<string, unknown> | undefined;
  if (genPaths) {
    for (const [path, spec] of Object.entries(genPaths)) {
      if (!isMemberPersonalCatalogGeneratedPath(path)) {
        continue;
      }
      const key = stripApiV1PrefixForOpenApiPath(path);
      if (mergedPaths[key] === undefined) {
        mergedPaths[key] = spec;
      }
    }
  }

  const bComp = bundled.components as Record<string, unknown> | undefined;
  const gComp = generated.components as Record<string, unknown> | undefined;
  const mergedComponents = {
    ...bComp,
    ...gComp,
    schemas: mergeOpenApiRecordSections(
      bComp?.schemas as Record<string, unknown> | undefined,
      gComp?.schemas as Record<string, unknown> | undefined,
      false,
    ),
    parameters: mergeOpenApiRecordSections(
      bComp?.parameters as Record<string, unknown> | undefined,
      gComp?.parameters as Record<string, unknown> | undefined,
      false,
    ),
    responses: mergeOpenApiRecordSections(
      bComp?.responses as Record<string, unknown> | undefined,
      gComp?.responses as Record<string, unknown> | undefined,
      false,
    ),
    requestBodies: mergeOpenApiRecordSections(
      bComp?.requestBodies as Record<string, unknown> | undefined,
      gComp?.requestBodies as Record<string, unknown> | undefined,
      false,
    ),
    /** Nest defaults to `bearer`; bundled spec uses `bearerAuth`. Union both so Swagger UI sends the header for bundled paths. */
    securitySchemes: mergeOpenApiRecordSections(
      bComp?.securitySchemes as Record<string, unknown> | undefined,
      gComp?.securitySchemes as Record<string, unknown> | undefined,
      false,
    ),
  } as OpenAPIObject['components'];

  return {
    ...bundled,
    paths: mergedPaths as OpenAPIObject['paths'],
    components: mergedComponents,
  };
}

function buildSwaggerDocument(app: INestApplication): OpenAPIObject {
  return SwaggerModule.createDocument(
    app,
    new DocumentBuilder()
      .setTitle('GymTrak API')
      .setDescription(
        'Generated from Nest decorators. When `docs/gymtrak-api.openapi.json` exists, it is merged so hand-written and decorator routes both appear.',
      )
      .setVersion('1.0')
      .addBearerAuth(
        { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
        'bearerAuth',
      )
      .build(),
  );
}

function flattenValidationMessages(errors: ValidationError[]): string[] {
  const out: string[] = [];
  for (const e of errors) {
    if (e.constraints) {
      out.push(...Object.values(e.constraints));
    }
    if (e.children?.length) {
      out.push(...flattenValidationMessages(e.children));
    }
  }
  return out;
}

function resolveSwaggerServers(
  config: ConfigService,
  port: number,
): OpenAPIObject['servers'] {
  const servers: NonNullable<OpenAPIObject['servers']> = [];

  const publicBase = config.get<string>('APP_PUBLIC_URL')?.trim();
  if (publicBase) {
    const origin = publicBase.replace(/\/$/, '');
    servers.push({
      url: `${origin}/api/v1`,
      description: 'APP_PUBLIC_URL',
    });
  }

  const localBase =
    config.get<string>('SWAGGER_LOCAL_SERVER_URL')?.trim() ||
    (config.get<string>('NODE_ENV') === 'development'
      ? `http://localhost:${port}`
      : undefined);
  if (localBase) {
    const origin = localBase.replace(/\/$/, '');
    servers.push({
      url: `${origin}/api/v1`,
      description: config.get<string>('SWAGGER_LOCAL_SERVER_URL')?.trim()
        ? 'SWAGGER_LOCAL_SERVER_URL'
        : 'Local development',
    });
  }

  return servers.length > 0 ? servers : undefined;
}

async function bootstrap() {
  try {
    await runPreBootstrapTasks();

    const app = await NestFactory.create<NestExpressApplication>(AppModule, {
      logger: createNestLogger(),
    });
    app.set('trust proxy', 1);
    const config = app.get(ConfigService);

    app.useGlobalInterceptors(
      new GymContextInterceptor(),
      new AuditContextInterceptor(),
    );
    app.enableCors({
      origin: true,
      credentials: true,
      allowedHeaders: ['Content-Type', 'Authorization', 'X-Gym-Id'],
    });
    app.useStaticAssets(join(process.cwd(), 'uploads'), {
      prefix: '/uploads/',
    });
    app.setGlobalPrefix('api/v1');
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: false,
        forbidNonWhitelisted: false,
        transform: false,
        exceptionFactory: (errors) => {
          const msgs = flattenValidationMessages(errors);
          const message =
            msgs.length === 0 ? 'Validation failed' : msgs.join('; ');
          return new BadRequestException(message);
        },
      }),
    );

    const openApiPath = resolveBundledOpenApiPath();
    const generated = buildSwaggerDocument(app);
    let document: OpenAPIObject;
    if (openApiPath) {
      const bundled = JSON.parse(
        readFileSync(openApiPath, 'utf8'),
      ) as OpenAPIObject;
      document = mergeBundledOpenApiWithGenerated(bundled, generated);
    } else {
      new Logger('Bootstrap').warn(
        'docs/gymtrak-api.openapi.json not found (check cwd or copy docs/ on deploy). Serving Swagger from generated OpenAPI only.',
      );
      document = generated;
    }

    const rawPort = config.get<number>('PORT') ?? Number(process.env.PORT);
    const port =
      typeof rawPort === 'number' && Number.isFinite(rawPort) && rawPort > 0
        ? rawPort
        : 3000;

    const swaggerServers = resolveSwaggerServers(config, port);
    if (swaggerServers) {
      document.servers = swaggerServers;
    }

    SwaggerModule.setup('docs', app, document, {
      useGlobalPrefix: true,
      jsonDocumentUrl: 'docs/json',
      customJsStr: [
        `
          (function attachAutoBearerFromLogin() {
            var applied = false;
            function tryBind() {
              if (applied) return;
              var ui = window.ui;
              if (!ui || typeof ui.preauthorizeApiKey !== 'function') return;
              applied = true;

              var originalFetch = window.fetch;
              window.fetch = async function(input, init) {
                var res = await originalFetch(input, init);
                try {
                  var reqUrl = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                  var reqMethod = (init && init.method ? String(init.method) : 'GET').toUpperCase();
                  if (reqMethod !== 'POST' || !/\\/auth\\/login(?:\\?|$)/.test(reqUrl)) {
                    return res;
                  }

                  var copy = res.clone();
                  var body = await copy.json();
                  if (body && typeof body.access_token === 'string' && body.access_token.length > 0) {
                    ui.preauthorizeApiKey('bearerAuth', body.access_token);
                    try {
                      localStorage.setItem('gymtrak:last_access_token', body.access_token);
                    } catch (_e) {}
                  }
                } catch (_e) {}
                return res;
              };
            }
            tryBind();
            setInterval(tryBind, 1000);
          })();
        `,
      ],
      swaggerOptions: {
        docExpansion: 'none',
        persistAuthorization: true,
        patchDocumentOnRequest: !swaggerServers
          ? (req: Request, _res: Response, doc: OpenAPIObject) => {
              const xfProto = req.headers['x-forwarded-proto'];
              const proto =
                typeof xfProto === 'string'
                  ? xfProto.split(',')[0]?.trim()
                  : Array.isArray(xfProto)
                    ? xfProto[0]?.trim()
                    : req.protocol;
              const xfHost = req.headers['x-forwarded-host'];
              const host =
                typeof xfHost === 'string'
                  ? xfHost.split(',')[0]?.trim()
                  : Array.isArray(xfHost)
                    ? xfHost[0]?.trim()
                    : req.get('host');
              if (!host) {
                return doc;
              }
              const safeProto =
                proto === 'http' || proto === 'https' ? proto : 'https';
              return {
                ...doc,
                servers: [
                  {
                    url: `${safeProto}://${host}/api/v1`,
                    description: 'Detected from this request (proxy-aware)',
                  },
                ],
              };
            }
          : undefined,
      },
    });

    await app.listen(port, '0.0.0.0');

    const health = app.get(HealthService);
    const databasePing = await health.pingDatabase();
    logStartupSummary(config, port, databasePing);
  } catch (error: unknown) {
    logBootstrapError(error);
    process.exit(1);
  }
}

void bootstrap();
