-- Shop catalog + per-user favorites (RBAC: product:* permissions seeded below)

CREATE TYPE "GymProductUnit" AS ENUM ('KG', 'PCS');

CREATE TABLE "gym_products" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "category" TEXT NOT NULL,
    "stockQuantity" INTEGER NOT NULL DEFAULT 0,
    "price" DECIMAL(12,2) NOT NULL,
    "discountPrice" DECIMAL(12,2) NOT NULL,
    "unit" "GymProductUnit" NOT NULL,
    "description" TEXT,
    "images" TEXT[] DEFAULT ARRAY[]::TEXT[],
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "isDeleted" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "gym_products_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "gym_product_favorites" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "productId" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "gym_product_favorites_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "gym_product_favorites_userId_productId_key" ON "gym_product_favorites"("userId", "productId");

CREATE INDEX "gym_products_gymId_isDeleted_idx" ON "gym_products"("gymId", "isDeleted");
CREATE INDEX "gym_products_gymId_category_idx" ON "gym_products"("gymId", "category");
CREATE INDEX "gym_product_favorites_userId_gymId_idx" ON "gym_product_favorites"("userId", "gymId");

ALTER TABLE "gym_products" ADD CONSTRAINT "gym_products_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "gym_product_favorites" ADD CONSTRAINT "gym_product_favorites_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "gym_product_favorites" ADD CONSTRAINT "gym_product_favorites_productId_fkey" FOREIGN KEY ("productId") REFERENCES "gym_products"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "gym_product_favorites" ADD CONSTRAINT "gym_product_favorites_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

INSERT INTO "Permission" ("id", "code", "description", "module") VALUES
  ('cm_pr_create', 'product:create', 'Create gym shop products', 'products'),
  ('cm_pr_read', 'product:read', 'View gym shop products', 'products'),
  ('cm_pr_update', 'product:update', 'Update gym shop products', 'products'),
  ('cm_pr_delete', 'product:delete', 'Delete gym shop products', 'products')
ON CONFLICT ("code") DO NOTHING;

INSERT INTO "GymRolePermissionDefault" ("gymRole", "permissionId") VALUES
  ('TRAINER', (SELECT "id" FROM "Permission" WHERE "code" = 'product:read')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'product:create')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'product:read')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'product:update')),
  ('STAFF', (SELECT "id" FROM "Permission" WHERE "code" = 'product:delete'))
ON CONFLICT ("gymRole", "permissionId") DO NOTHING;
