-- Workout & exercise management (owner/trainer assignment + set logging)
CREATE TABLE IF NOT EXISTS "exercise_master" (
  "id" TEXT PRIMARY KEY,
  "name" TEXT NOT NULL,
  "equipment" TEXT,
  "primary_muscle" TEXT,
  "exercise_type" TEXT NOT NULL,
  "asset_url" TEXT,
  "created_by_user_id" TEXT REFERENCES "User"("id") ON DELETE SET NULL,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "exercise_master_name_idx"
  ON "exercise_master" ("name");

CREATE TABLE IF NOT EXISTS "member_workout_plans" (
  "id" TEXT PRIMARY KEY,
  "gym_id" TEXT NOT NULL REFERENCES "Gym"("id") ON DELETE CASCADE,
  "gym_user_id" TEXT NOT NULL REFERENCES "GymUser"("id") ON DELETE CASCADE,
  "title" TEXT NOT NULL,
  "notes" TEXT,
  "started_at" TIMESTAMP(3),
  "ended_at" TIMESTAMP(3),
  "completed" BOOLEAN NOT NULL DEFAULT false,
  "total_volume" INT NOT NULL DEFAULT 0,
  "total_sets" INT NOT NULL DEFAULT 0,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "member_workout_plans_gym_user_id_idx"
  ON "member_workout_plans" ("gym_user_id");
CREATE INDEX IF NOT EXISTS "member_workout_plans_gym_id_idx"
  ON "member_workout_plans" ("gym_id");

CREATE TABLE IF NOT EXISTS "workout_plan_exercises" (
  "id" TEXT PRIMARY KEY,
  "workout_id" TEXT NOT NULL REFERENCES "member_workout_plans"("id") ON DELETE CASCADE,
  "exercise_id" TEXT NOT NULL REFERENCES "exercise_master"("id") ON DELETE RESTRICT,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "workout_plan_exercises_workout_id_idx"
  ON "workout_plan_exercises" ("workout_id");
CREATE INDEX IF NOT EXISTS "workout_plan_exercises_exercise_id_idx"
  ON "workout_plan_exercises" ("exercise_id");

CREATE TABLE IF NOT EXISTS "workout_exercise_sets" (
  "id" TEXT PRIMARY KEY,
  "workout_exercise_id" TEXT NOT NULL REFERENCES "workout_plan_exercises"("id") ON DELETE CASCADE,
  "set_number" INT NOT NULL,
  "reps" INT NOT NULL DEFAULT 0,
  "weight" INT NOT NULL DEFAULT 0,
  "completed" BOOLEAN NOT NULL DEFAULT false,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "workout_exercise_sets_workout_exercise_id_idx"
  ON "workout_exercise_sets" ("workout_exercise_id");
