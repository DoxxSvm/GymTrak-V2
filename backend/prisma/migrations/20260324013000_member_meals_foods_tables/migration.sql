-- Diet & meal management tables
CREATE TABLE IF NOT EXISTS "member_meals" (
  "id" TEXT PRIMARY KEY,
  "gym_id" TEXT NOT NULL REFERENCES "Gym"("id") ON DELETE CASCADE,
  "gym_user_id" TEXT NOT NULL REFERENCES "GymUser"("id") ON DELETE CASCADE,
  "meal_name" TEXT NOT NULL,
  "meal_time" TEXT NOT NULL,
  "meal_type" TEXT NOT NULL,
  "repeat_days" JSONB NOT NULL DEFAULT '[]'::jsonb,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "member_meals_gym_id_idx"
  ON "member_meals" ("gym_id");
CREATE INDEX IF NOT EXISTS "member_meals_gym_user_id_idx"
  ON "member_meals" ("gym_user_id");

CREATE TABLE IF NOT EXISTS "meal_food_items" (
  "id" TEXT PRIMARY KEY,
  "meal_id" TEXT NOT NULL REFERENCES "member_meals"("id") ON DELETE CASCADE,
  "food_name" TEXT NOT NULL,
  "quantity" INT NOT NULL,
  "calories" INT NOT NULL,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "meal_food_items_meal_id_idx"
  ON "meal_food_items" ("meal_id");
