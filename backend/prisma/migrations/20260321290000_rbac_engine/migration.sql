-- Gym-level module toggles (stack with per-user RBAC)
ALTER TYPE "GymFeatureKey" ADD VALUE 'payments';
ALTER TYPE "GymFeatureKey" ADD VALUE 'dashboard';

-- New permission
INSERT INTO "Permission" ("id", "code", "description", "module") VALUES
  ('cm_perm_admin', 'admin:access', 'Admin: manage staff and sensitive gym operations', 'admin')
ON CONFLICT ("code") DO NOTHING;

-- Default RBAC templates for TRAINER vs STAFF (used when seeding new memberships; owner bypasses)
CREATE TABLE "GymRolePermissionDefault" (
    "gymRole" "GymRole" NOT NULL,
    "permissionId" TEXT NOT NULL,

    CONSTRAINT "GymRolePermissionDefault_pkey" PRIMARY KEY ("gymRole","permissionId")
);

CREATE INDEX "GymRolePermissionDefault_gymRole_idx" ON "GymRolePermissionDefault"("gymRole");

ALTER TABLE "GymRolePermissionDefault" ADD CONSTRAINT "GymRolePermissionDefault_permissionId_fkey" FOREIGN KEY ("permissionId") REFERENCES "Permission"("id") ON DELETE CASCADE ON UPDATE CASCADE;

INSERT INTO "GymRolePermissionDefault" ("gymRole", "permissionId") VALUES
  ('TRAINER', (SELECT "id" FROM "Permission" WHERE "code" = 'dashboard:access')),
  ('TRAINER', (SELECT "id" FROM "Permission" WHERE "code" = 'members:manage')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'dashboard:access')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'members:manage')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'payments:access'))
ON CONFLICT ("gymRole", "permissionId") DO NOTHING;
