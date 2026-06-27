-- Member workout and diet logs for owner app detail module
CREATE TABLE IF NOT EXISTS "member_workouts" (
  "id" TEXT PRIMARY KEY,
  "gym_id" TEXT NOT NULL REFERENCES "Gym"("id") ON DELETE CASCADE,
  "gym_user_id" TEXT NOT NULL REFERENCES "GymUser"("id") ON DELETE CASCADE,
  "title" TEXT NOT NULL,
  "description" TEXT,
  "trainer_name" TEXT,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "member_workouts_gym_id_idx"
  ON "member_workouts" ("gym_id");
CREATE INDEX IF NOT EXISTS "member_workouts_gym_user_id_idx"
  ON "member_workouts" ("gym_user_id");
CREATE INDEX IF NOT EXISTS "member_workouts_created_at_idx"
  ON "member_workouts" ("created_at");

CREATE TABLE IF NOT EXISTS "member_diets" (
  "id" TEXT PRIMARY KEY,
  "gym_id" TEXT NOT NULL REFERENCES "Gym"("id") ON DELETE CASCADE,
  "gym_user_id" TEXT NOT NULL REFERENCES "GymUser"("id") ON DELETE CASCADE,
  "meal_type" TEXT NOT NULL,
  "items_json" JSONB NOT NULL,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "member_diets_gym_id_idx"
  ON "member_diets" ("gym_id");
CREATE INDEX IF NOT EXISTS "member_diets_gym_user_id_idx"
  ON "member_diets" ("gym_user_id");
CREATE INDEX IF NOT EXISTS "member_diets_created_at_idx"
  ON "member_diets" ("created_at");
