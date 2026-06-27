import { Logger } from '@nestjs/common';
import { spawnSync } from 'child_process';
import * as bcrypt from 'bcrypt';
import { GlobalRole, PrismaClient } from '@prisma/client';

const logger = new Logger('StartupTasks');

function shouldSkipMigrations(): boolean {
  const v = process.env.SKIP_MIGRATIONS_ON_STARTUP;
  return v === 'true' || v === '1';
}

/**
 * Applies pending SQL migrations (`prisma/migrations`). Safe for production (`migrate deploy`).
 * Set `SKIP_MIGRATIONS_ON_STARTUP=true` if your deploy pipeline runs migrations separately.
 */
export function runPrismaMigrateDeploy(): void {
  logger.log('Running database migrations (prisma migrate deploy)…');

  const result = spawnSync('npx', ['prisma', 'migrate', 'deploy'], {
    cwd: process.cwd(),
    stdio: 'inherit',
    env: process.env,
    shell: true,
  });

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    logMigrationFailureHelp();
    throw new Error(
      `prisma migrate deploy exited with code ${result.status ?? 'unknown'}`,
    );
  }

  logger.log('Database migrations applied.');
}

/**
 * Prisma P3009 / failed migration recovery — printed after a failed deploy.
 * @see https://www.prisma.io/docs/guides/migrate/production-troubleshooting
 */
function logMigrationFailureHelp(): void {
  logger.error(
    [
      '',
      '── Prisma migrate deploy failed ──',
      'If the error mentions P3009 ("failed migrations"), the DB has a migration marked failed.',
      'Fix the database (or confirm changes are already applied), then mark the migration:',
      '',
      '  1) Inspect: connect with psql and check tables/enums vs prisma/migrations/*.sql',
      '  2) If that migration’s SQL is ALREADY fully applied, mark it applied:',
      '       npx prisma migrate resolve --applied 20260321120000_notification_system',
      '  3) If it was NOT applied or only partly applied, fix SQL manually, then:',
      '       npx prisma migrate resolve --rolled-back 20260321120000_notification_system',
      '     then run deploy again (or restart the app).',
      '',
      'Common cause for 20260321120000_notification_system: UNIQUE index on "dedupeKey"',
      'fails if duplicate non-null dedupeKey rows exist — dedupe or null them first.',
      '',
      'Docs: https://pris.ly/d/migrate-resolve',
      '',
    ].join('\n'),
  );
}

function shouldBootstrapSuperAdmin(): boolean {
  const v = process.env.SUPER_ADMIN_BOOTSTRAP;
  return v === '1' || v === 'true';
}

/**
 * One-time style upsert for a platform super admin (staff login).
 * Enable with SUPER_ADMIN_BOOTSTRAP=1 plus phone, username, password env vars.
 * Remove or unset after first deploy if you prefer not to keep credentials in env.
 */
export async function bootstrapSuperAdminIfConfigured(): Promise<void> {
  if (!shouldBootstrapSuperAdmin()) {
    return;
  }

  const phone = process.env.SUPER_ADMIN_BOOTSTRAP_PHONE?.trim();
  const usernameRaw = process.env.SUPER_ADMIN_BOOTSTRAP_USERNAME?.trim();
  const password = process.env.SUPER_ADMIN_BOOTSTRAP_PASSWORD;

  if (!phone || !usernameRaw || !password) {
    logger.warn(
      'SUPER_ADMIN_BOOTSTRAP is set but SUPER_ADMIN_BOOTSTRAP_PHONE / USERNAME / PASSWORD are incomplete — skipping super admin bootstrap',
    );
    return;
  }

  const username = usernameRaw.toLowerCase();
  const prisma = new PrismaClient();

  try {
    const passwordHash = await bcrypt.hash(password, 10);

    await prisma.user.upsert({
      where: { phone },
      create: {
        phone,
        username,
        passwordHash,
        globalRole: GlobalRole.SUPER_ADMIN,
        status: 'ACTIVE',
      },
      update: {
        username,
        passwordHash,
        globalRole: GlobalRole.SUPER_ADMIN,
        status: 'ACTIVE',
      },
    });

    logger.log(
      `Super admin ready: username "${username}" (login via POST /api/v1/auth/staff/login)`,
    );
  } finally {
    await prisma.$disconnect();
  }
}

export async function runPreBootstrapTasks(): Promise<void> {
  if (shouldSkipMigrations()) {
    logger.warn(
      'SKIP_MIGRATIONS_ON_STARTUP is set — skipping prisma migrate deploy (ensure migrations ran elsewhere)',
    );
  } else {
    runPrismaMigrateDeploy();
  }

  await bootstrapSuperAdminIfConfigured();
}
