CREATE TABLE IF NOT EXISTS "gym_products" (
  "id" TEXT PRIMARY KEY,
  "gym_id" TEXT NOT NULL REFERENCES "Gym"("id") ON DELETE CASCADE,
  "name" TEXT NOT NULL,
  "category" TEXT NOT NULL,
  "stock_quantity" INTEGER NOT NULL DEFAULT 0,
  "cost_price_cents" INTEGER NOT NULL DEFAULT 0,
  "selling_price_cents" INTEGER NOT NULL DEFAULT 0,
  "description" TEXT,
  "image_url" TEXT,
  "discount_price_cents" INTEGER,
  "is_deleted" BOOLEAN NOT NULL DEFAULT FALSE,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "gym_products_gym_id_idx"
  ON "gym_products" ("gym_id");
CREATE INDEX IF NOT EXISTS "gym_products_gym_id_deleted_idx"
  ON "gym_products" ("gym_id", "is_deleted");
CREATE INDEX IF NOT EXISTS "gym_products_name_idx"
  ON "gym_products" ("name");
