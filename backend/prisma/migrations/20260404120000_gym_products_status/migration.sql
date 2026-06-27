-- Product lifecycle for shop catalog (per-gym).
CREATE TYPE "GymProductStatus" AS ENUM ('DRAFT', 'PENDING', 'ACTIVE', 'INACTIVE');

ALTER TABLE "gym_products" ADD COLUMN "status" "GymProductStatus" NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE "gym_products" ADD COLUMN "pending_visibility" BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS "gym_products_gym_id_status_idx"
  ON "gym_products" ("gym_id", "status");
