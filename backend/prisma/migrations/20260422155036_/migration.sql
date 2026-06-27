-- AlterTable
ALTER TABLE "member_personal_workout_plans" ADD COLUMN     "duration" TEXT,
ADD COLUMN     "isSaved" BOOLEAN NOT NULL DEFAULT false;
