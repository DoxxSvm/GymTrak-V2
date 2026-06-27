/*
  Warnings:

  - You are about to drop the `member_workout_plans` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `workout_exercise_sets` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `workout_plan_exercises` table. If the table is not empty, all the data it contains will be lost.

*/
-- DropForeignKey
ALTER TABLE "member_workout_plans" DROP CONSTRAINT "member_workout_plans_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "member_workout_plans" DROP CONSTRAINT "member_workout_plans_gym_user_id_fkey";

-- DropForeignKey
ALTER TABLE "workout_exercise_sets" DROP CONSTRAINT "workout_exercise_sets_workout_exercise_id_fkey";

-- DropForeignKey
ALTER TABLE "workout_plan_exercises" DROP CONSTRAINT "workout_plan_exercises_exercise_id_fkey";

-- DropForeignKey
ALTER TABLE "workout_plan_exercises" DROP CONSTRAINT "workout_plan_exercises_workout_id_fkey";

-- AlterTable
ALTER TABLE "Expense" ALTER COLUMN "currency" SET DEFAULT 'INR';

-- DropTable
DROP TABLE "member_workout_plans";

-- DropTable
DROP TABLE "workout_exercise_sets";

-- DropTable
DROP TABLE "workout_plan_exercises";
