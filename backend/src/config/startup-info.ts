import { Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { networkInterfaces } from 'os';

import type { DatabasePingResult } from '../modules/health/health.service';

const logger = new Logger('Startup');

/**
 * Show host, port, path, user — mask password. Safe for logs.
 * Supports `postgresql://`, `postgres://`, `redis://`, `rediss://`.
 */
export function sanitizeConnectionUrl(url: string | undefined): string {
  if (!url?.trim()) {
    return '(not set)';
  }
  const raw = url.trim();
  try {
    const u = new URL(raw);
    if (u.password) {
      u.password = '***';
    }
    return u.toString();
  } catch {
    return '(invalid URL — cannot parse; check env format)';
  }
}

function isBullDisabled(): boolean {
  const v = process.env.DISABLE_BULLMQ;
  return v === 'true' || v === '1';
}

/** First non-internal IPv4 (typical Wi‑Fi / Ethernet LAN address for mobile dev). */
function getLanIPv4(): string | undefined {
  const nics = networkInterfaces();
  for (const addrs of Object.values(nics)) {
    if (!addrs) {
      continue;
    }
    for (const a of addrs) {
      if (a.family === 'IPv4' && !a.internal) {
        return a.address;
      }
    }
  }
  return undefined;
}

/**
 * Printed after HTTP listen succeeds. Uses ConfigService for validated env.
 * `databasePing` is a live `SELECT 1` round-trip (same check as GET /health).
 */
export function logStartupSummary(
  config: ConfigService,
  listenPort: number,
  databasePing: DatabasePingResult,
): void {
  const nodeEnv =
    config.get<string>('NODE_ENV') ?? process.env.NODE_ENV ?? 'development';
  const databaseUrl = sanitizeConnectionUrl(config.get<string>('DATABASE_URL'));
  const redisRaw = config.get<string>('REDIS_URL');
  const redisDisplay = redisRaw?.trim()
    ? sanitizeConnectionUrl(redisRaw)
    : '(not set — optional for cache / queues)';
  const bullMq = isBullDisabled()
    ? 'disabled (DISABLE_BULLMQ)'
    : 'enabled (requires Redis)';
  const jwtAccess = config.get<string>('JWT_ACCESS_EXPIRES_IN') ?? '15m';
  const appPublicFromEnv = config.get<string>('APP_PUBLIC_URL')?.trim();
  const defaultLocalBase = `http://localhost:${listenPort}`;
  const apiBase = (appPublicFromEnv ?? defaultLocalBase).replace(/\/$/, '');

  const dbLine = databasePing.ok
    ? `up (${databasePing.latencyMs ?? 0} ms)`
    : `DOWN — ${databasePing.error ?? 'unknown error'}`;

  const lanIp = getLanIPv4();
  const mobileBase =
    lanIp != null
      ? `http://${lanIp}:${listenPort}/api/v1`
      : `http://<this-PC-LAN-IP>:${listenPort}/api/v1`;
  const mobileConfigUrl = `${mobileBase}/app/config`;

  const mobileHelpLines = [
    '  ────────────────────────────────────────────────────────────────',
    '  Mobile / LAN — connect timeouts (e.g. ConnectTimeoutException, ~30s)',
    '  The client opens a TCP connection but gets no response in time. That is',
    '  usually a network or firewall issue, not necessarily a bug in app code.',
    '',
    '  1) Mobile browser test — on the phone, open:',
    `       ${mobileConfigUrl}`,
    '     • Does not load → network / firewall (same Wi‑Fi? correct IP? port open?)',
    '     • Loads OK but the native app fails → check the app API base URL / env.',
    '',
    `  2) Windows Firewall — allow inbound TCP port ${listenPort} (e.g. Defender`,
    '     Firewall → Advanced Settings → Inbound Rules → New Rule → Port → TCP →',
    `     Specific local ports: ${listenPort} → Allow the connection).`,
    '',
    '  3) Server binding — the API must listen on all interfaces, not only',
    '     localhost. This server uses listen(..., "0.0.0.0") so LAN devices can',
    '     reach it. Use your PC’s LAN IP in the mobile app, not 127.0.0.1.',
    `  Mobile API base   : ${mobileBase}`,
  ];

  const lines = [
    '',
    '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━',
    '  GymTrak API — ready',
    '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━',
    `  Environment     : ${nodeEnv}`,
    `  Listening       : 0.0.0.0:${listenPort}`,
    `  Health URL      : ${apiBase}/api/v1/health`,
    `  Swagger UI      : ${apiBase}/api/v1/docs`,
    ...(process.platform === 'win32'
      ? [
          `  Swagger (IPv4)  : http://127.0.0.1:${listenPort}/api/v1/docs  (use if localhost shows another app)`,
        ]
      : []),
    `  OpenAPI JSON    : ${apiBase}/api/v1/docs/json`,
    `  Postman JSON    : ${apiBase}/api/v1/reference/postman-collection.json`,
    `  Global prefix   : /api/v1`,
    `  Database (DSN)  : ${databaseUrl}`,
    `  Database (ping) : ${dbLine}`,
    `  Redis           : ${redisDisplay}`,
    `  BullMQ / queue  : ${bullMq}`,
    `  JWT access TTL  : ${jwtAccess}`,
    `  APP_PUBLIC_URL  : ${appPublicFromEnv ?? '(not set — links above use localhost)'}`,
    `  Node.js         : ${process.version}`,
    ...mobileHelpLines,
    '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━',
    '',
  ];

  logger.log(lines.join('\n'));

  if (!databasePing.ok) {
    logger.warn(
      'Database ping failed after listen — check credentials, network, and migrations.',
    );
  }
}

export function logBootstrapError(error: unknown): void {
  const errLogger = new Logger('Bootstrap');
  errLogger.error('Application failed to start');

  if (error instanceof Error) {
    errLogger.error(`Message: ${error.message}`);
    if (error.stack) {
      errLogger.error(error.stack);
    }
    const cause = (error as Error & { cause?: unknown }).cause;
    if (cause !== undefined) {
      errLogger.error(`Cause: ${String(cause)}`);
      if (cause instanceof Error && cause.stack) {
        errLogger.error(cause.stack);
      }
    }
    return;
  }

  try {
    errLogger.error(JSON.stringify(error, null, 2));
  } catch {
    errLogger.error(String(error));
  }
}
