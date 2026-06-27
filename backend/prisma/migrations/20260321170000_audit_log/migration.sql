-- Enterprise audit trail
CREATE TYPE "AuditAction" AS ENUM (
  'MEMBER_ADDED',
  'PAYMENT_RECORDED',
  'PLAN_ASSIGNED',
  'SUBSCRIPTION_RENEWED',
  'SUBSCRIPTION_EXTENDED',
  'SUBSCRIPTION_UPGRADED',
  'SUBSCRIPTION_FROZEN'
);

CREATE TYPE "AuditEntityType" AS ENUM (
  'GYM_USER',
  'PAYMENT',
  'MEMBER_SUBSCRIPTION'
);

CREATE TABLE "AuditLog" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "actorUserId" TEXT NOT NULL,
    "action" "AuditAction" NOT NULL,
    "entityType" "AuditEntityType",
    "entityId" TEXT,
    "metadata" JSONB,
    "ipAddress" TEXT,
    "userAgent" VARCHAR(512),
    "requestId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "AuditLog_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "AuditLog_gymId_createdAt_idx" ON "AuditLog"("gymId", "createdAt");
CREATE INDEX "AuditLog_gymId_action_createdAt_idx" ON "AuditLog"("gymId", "action", "createdAt");
CREATE INDEX "AuditLog_actorUserId_createdAt_idx" ON "AuditLog"("actorUserId", "createdAt");

ALTER TABLE "AuditLog" ADD CONSTRAINT "AuditLog_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "AuditLog" ADD CONSTRAINT "AuditLog_actorUserId_fkey" FOREIGN KEY ("actorUserId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
