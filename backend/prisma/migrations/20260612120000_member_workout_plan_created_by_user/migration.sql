-- AlterTable
ALTER TABLE "member_workout_plans" ADD COLUMN "created_by_user_id" TEXT;

-- Backfill gym owner as creator for owner-assigned plans
UPDATE "member_workout_plans" AS w
SET "created_by_user_id" = g."ownerId"
FROM "Gym" AS g
WHERE w."gym_id" = g."id"
  AND w."createdByRole" = 'OWNER'
  AND w."created_by_user_id" IS NULL;

-- AddForeignKey
ALTER TABLE "member_workout_plans" ADD CONSTRAINT "member_workout_plans_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
