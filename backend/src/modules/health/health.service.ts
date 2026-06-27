import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { readFileSync } from 'fs';
import { join } from 'path';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

export type DatabasePingResult = {
  ok: boolean;
  latencyMs?: number;
  error?: string;
};

export type OverallHealthResponse = {
  status: 'ok' | 'degraded';
  timestamp: string;
  uptimeSeconds: number;
  api: {
    name: string;
    version: string;
    globalPrefix: string;
    environment: string;
    nodeVersion: string;
  };
  database: {
    status: 'up' | 'down';
    latencyMs?: number;
    provider: string;
    error?: string;
  };
};

@Injectable()
export class HealthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
  ) {}

  private getPackageMeta(): { name: string; version: string } {
    try {
      const raw = readFileSync(join(process.cwd(), 'package.json'), 'utf8');
      const pkg = JSON.parse(raw) as { name?: string; version?: string };
      return {
        name: pkg.name ?? 'gymtrak-backend',
        version: pkg.version ?? 'unknown',
      };
    } catch {
      return { name: 'gymtrak-backend', version: 'unknown' };
    }
  }

  /** Lightweight DB round-trip (safe for load balancers). */
  async pingDatabase(): Promise<DatabasePingResult> {
    const started = Date.now();
    try {
      await this.prisma.$queryRaw(Prisma.sql`SELECT 1 AS ok`);
      return { ok: true, latencyMs: Date.now() - started };
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e);
      return { ok: false, error: message };
    }
  }

  async getOverallHealth(): Promise<OverallHealthResponse> {
    const db = await this.pingDatabase();
    const pkg = this.getPackageMeta();
    const env =
      this.config.get<string>('NODE_ENV') ??
      process.env.NODE_ENV ??
      'development';

    return {
      status: db.ok ? 'ok' : 'degraded',
      timestamp: new Date().toISOString(),
      uptimeSeconds: Math.floor(process.uptime()),
      api: {
        name: pkg.name,
        version: pkg.version,
        globalPrefix: '/api/v1',
        environment: env,
        nodeVersion: process.version,
      },
      database: {
        status: db.ok ? 'up' : 'down',
        latencyMs: db.latencyMs,
        provider: 'postgresql',
        error: db.error,
      },
    };
  }
}
