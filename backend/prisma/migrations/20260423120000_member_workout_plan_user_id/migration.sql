-- Logged workouts: who created (user_id) + optional gym; personal rows have gym_id and gym_user_id null.
ALTER TABLE "member_workout_plans" ADD COLUMN "user_id" TEXT;

UPDATE "member_workout_plans" AS m
SET "user_id" = g."ownerId"
FROM "Gym" AS g
WHERE m."gym_id" = g."id";

ALTER TABLE "member_workout_plans" ALTER COLUMN "user_id" SET NOT NULL;

ALTER TABLE "member_workout_plans" ADD CONSTRAINT "member_workout_plans_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "member_workout_plans" ALTER COLUMN "gym_id" DROP NOT NULL;
ALTER TABLE "member_workout_plans" ALTER COLUMN "gym_user_id" DROP NOT NULL;

CREATE INDEX "member_workout_plans_user_id_idx" ON "member_workout_plans"("user_id");
CREATE INDEX "member_workout_plans_gym_id_user_id_idx" ON "member_workout_plans"("gym_id", "user_id");
