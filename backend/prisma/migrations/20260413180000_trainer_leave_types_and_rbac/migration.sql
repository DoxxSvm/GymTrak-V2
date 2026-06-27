-- CreateEnum
CREATE TYPE "TrainerLeaveType" AS ENUM ('MEDICAL', 'PERSONAL', 'MATERNITY');

-- AlterTable
ALTER TABLE "TrainerLeave" ADD COLUMN "leaveType" "TrainerLeaveType" NOT NULL DEFAULT 'PERSONAL';
ALTER TABLE "TrainerLeave" ADD COLUMN "rejectionReason" TEXT;

-- Seed leave permissions
INSERT INTO "Permission" ("id", "code", "description", "module") VALUES
  ('cm_lv_create', 'leave:create', 'Apply for / create leave requests', 'leaves'),
  ('cm_lv_read', 'leave:read', 'View leave requests', 'leaves'),
  ('cm_lv_update', 'leave:update', 'Edit own pending leave', 'leaves'),
  ('cm_lv_delete', 'leave:delete', 'Delete leave requests (admin)', 'leaves'),
  ('cm_lv_approve', 'leave:approve', 'Approve leave requests', 'leaves'),
  ('cm_lv_reject', 'leave:reject', 'Reject leave requests', 'leaves')
ON CONFLICT ("code") DO NOTHING;

-- Trainer defaults: create, read, update own
INSERT INTO "GymRolePermissionDefault" ("gymRole", "permissionId") VALUES
  ('TRAINER', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:create')),
  ('TRAINER', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:read')),
  ('TRAINER', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:update'))
ON CONFLICT ("gymRole", "permissionId") DO NOTHING;

-- Staff defaults: manage queue for owner desk
INSERT INTO "GymRolePermissionDefault" ("gymRole", "permissionId") VALUES
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:create')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:read')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:approve')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:reject')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'leave:delete'))
ON CONFLICT ("gymRole", "permissionId") DO NOTHING;
