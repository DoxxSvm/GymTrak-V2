-- Backfill-safe NOT NULL: existing rows get MEMBER; default is dropped so new rows must set createdByRole in app code.
ALTER TABLE "diet_meals" ADD COLUMN "createdByRole" TEXT NOT NULL DEFAULT 'MEMBER';

ALTER TABLE "member_workout_plans" ADD COLUMN "createdByRole" TEXT NOT NULL DEFAULT 'MEMBER';

ALTER TABLE "diet_meals" ALTER COLUMN "createdByRole" DROP DEFAULT;

ALTER TABLE "member_workout_plans" ALTER COLUMN "createdByRole" DROP DEFAULT;
