/**
 * Upserts the super admin from .env (same as app startup when SUPER_ADMIN_BOOTSTRAP=1).
 * Run: npm run db:bootstrap-super-admin
 */
require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const bcrypt = require('bcrypt');
const { PrismaClient, GlobalRole } = require('@prisma/client');

async function main() {
  const phone = process.env.SUPER_ADMIN_BOOTSTRAP_PHONE?.trim();
  const usernameRaw = process.env.SUPER_ADMIN_BOOTSTRAP_USERNAME?.trim();
  const password = process.env.SUPER_ADMIN_BOOTSTRAP_PASSWORD;

  if (!phone || !usernameRaw || !password) {
    console.error(
      'Missing env: SUPER_ADMIN_BOOTSTRAP_PHONE, SUPER_ADMIN_BOOTSTRAP_USERNAME, SUPER_ADMIN_BOOTSTRAP_PASSWORD',
    );
    process.exit(1);
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

    const u = await prisma.user.findUnique({
      where: { phone },
      select: { id: true, phone: true, username: true, status: true, globalRole: true },
    });
    console.log('Super admin ready:\n', JSON.stringify(u, null, 2));
    console.log('\nStaff login: POST /api/v1/auth/staff/login');
    console.log('Optional .env line:\n  AUTH_DEV_USER_ID=' + u.id);
  } finally {
    await prisma.$disconnect();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
