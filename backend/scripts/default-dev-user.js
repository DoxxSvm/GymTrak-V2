/**
 * Shows which User the API uses when no Bearer token is sent in development
 * (matches JwtAuthGuard: AUTH_DEV_USER_ID, else first ACTIVE by createdAt).
 *
 * Run: npm run db:default-user  (from backend/)
 */
require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const { PrismaClient } = require('@prisma/client');

async function main() {
  const prisma = new PrismaClient();
  try {
    const explicitId = process.env.AUTH_DEV_USER_ID?.trim();
    if (explicitId) {
      const u = await prisma.user.findUnique({
        where: { id: explicitId },
        select: {
          id: true,
          phone: true,
          status: true,
          globalRole: true,
          selectedOnboardingRole: true,
          createdAt: true,
        },
      });
      console.log('AUTH_DEV_USER_ID is set.\n');
      if (!u) {
        console.log(`No user with id "${explicitId}".`);
        return;
      }
      console.log(JSON.stringify(u, null, 2));
      console.log(
        u.status === 'ACTIVE'
          ? '\n→ This user would be attached when JWT is relaxed and no Bearer is sent.'
          : '\n→ User is not ACTIVE; guard will not use this row.',
      );
      return;
    }

    console.log('AUTH_DEV_USER_ID is not set; using first ACTIVE user by createdAt:\n');
    const first = await prisma.user.findFirst({
      where: { status: 'ACTIVE' },
      orderBy: { createdAt: 'asc' },
      select: {
        id: true,
        phone: true,
        status: true,
        globalRole: true,
        selectedOnboardingRole: true,
        createdAt: true,
      },
    });
    if (!first) {
      console.log('No ACTIVE users in the database.\n');
      console.log('Create one, then re-run this script:');
      console.log(
        '  1) Add to .env and restart the API (runs on startup):',
      );
      console.log(
        '       SUPER_ADMIN_BOOTSTRAP=1',
      );
      console.log(
        '       SUPER_ADMIN_BOOTSTRAP_PHONE=+919876543210',
      );
      console.log(
        '       SUPER_ADMIN_BOOTSTRAP_USERNAME=demo_admin',
      );
      console.log(
        '       SUPER_ADMIN_BOOTSTRAP_PASSWORD=YourPassword8chars+',
      );
      console.log('  2) Or insert a row in Prisma Studio: npm run prisma:studio');
      return;
    }
    console.log(JSON.stringify(first, null, 2));
    console.log(
      '\n→ Set AUTH_DEV_USER_ID=' +
        first.id +
        ' in .env to pin this user.',
    );
  } finally {
    await prisma.$disconnect();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
