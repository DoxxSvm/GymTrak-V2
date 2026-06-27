import 'dotenv/config';

import { BadRequestException, Logger, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import type { NestExpressApplication } from '@nestjs/platform-express';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import type { OpenAPIObject } from '@nestjs/swagger';
import type { Request, Response } from 'express';
import { existsSync, readFileSync } from 'fs';
import { join } from 'path';
import { AppModule } from './app.module';
import { runPreBootstrapTasks } from './config/startup-tasks';
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

async function bootstrap() {
  try {
    await runPreBootstrapTasks();

    const app = await NestFactory.create<NestExpressApplication>(AppModule);
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
    let document: OpenAPIObject;
    if (openApiPath) {
      document = JSON.parse(readFileSync(openApiPath, 'utf8')) as OpenAPIObject;
    } else {
      new Logger('Bootstrap').warn(
        'docs/gymtrak-api.openapi.json not found (check cwd or copy docs/ on deploy). Serving Swagger from generated OpenAPI.',
      );
      document = SwaggerModule.createDocument(
        app,
        new DocumentBuilder()
          .setTitle('GymTrak API')
          .setDescription(
            'Generated from Nest decorators. Add docs/gymtrak-api.openapi.json for the full hand-maintained spec.',
          )
          .setVersion('1.0')
          .addBearerAuth()
          .build(),
      );
    }

    const publicBase = config.get<string>('APP_PUBLIC_URL')?.trim();
    if (publicBase) {
      const origin = publicBase.replace(/\/$/, '');
      document.servers = [
        {
          url: `${origin}/api/v1`,
          description: 'APP_PUBLIC_URL',
        },
        {
          url: `http://192.168.1.73:3000/api/v1`,
          description: 'Localhost - 192.168.1.73',
        },
      ];
    }

    SwaggerModule.setup('docs', app, document, {
      useGlobalPrefix: true,
      jsonDocumentUrl: 'docs/json',
      swaggerOptions: {
        docExpansion: 'none',
        persistAuthorization: true,
        patchDocumentOnRequest: !publicBase
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
                  {
                    url: `http://192.168.1.73:3000/api/v1`,
                    description: 'Localhost - 192.168.1.73',
                  },
                ],
              };
            }
          : undefined,
      },
    });

    const rawPort = config.get<number>('PORT') ?? Number(process.env.PORT);
    const port =
      typeof rawPort === 'number' && Number.isFinite(rawPort) && rawPort > 0
        ? rawPort
        : 3000;
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
