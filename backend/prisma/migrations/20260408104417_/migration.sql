/*
  Warnings:

  - You are about to drop the `exercise_master` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `gym_products` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `meal_food_items` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `member_diets` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `member_meals` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `member_workout_plans` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `member_workouts` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `subscription_history_events` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `workout_exercise_sets` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `workout_plan_exercises` table. If the table is not empty, all the data it contains will be lost.

*/
-- DropForeignKey
ALTER TABLE "MemberSubscription" DROP CONSTRAINT "MemberSubscription_planId_fkey";

-- DropForeignKey
ALTER TABLE "exercise_master" DROP CONSTRAINT "exercise_master_created_by_user_id_fkey";

-- DropForeignKey
ALTER TABLE "gym_products" DROP CONSTRAINT "gym_products_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "meal_food_items" DROP CONSTRAINT "meal_food_items_meal_id_fkey";

-- DropForeignKey
ALTER TABLE "member_diets" DROP CONSTRAINT "member_diets_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "member_diets" DROP CONSTRAINT "member_diets_gym_user_id_fkey";

-- DropForeignKey
ALTER TABLE "member_meals" DROP CONSTRAINT "member_meals_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "member_meals" DROP CONSTRAINT "member_meals_gym_user_id_fkey";

-- DropForeignKey
ALTER TABLE "member_workout_plans" DROP CONSTRAINT "member_workout_plans_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "member_workout_plans" DROP CONSTRAINT "member_workout_plans_gym_user_id_fkey";

-- DropForeignKey
ALTER TABLE "member_workouts" DROP CONSTRAINT "member_workouts_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "member_workouts" DROP CONSTRAINT "member_workouts_gym_user_id_fkey";

-- DropForeignKey
ALTER TABLE "subscription_history_events" DROP CONSTRAINT "subscription_history_events_actor_user_id_fkey";

-- DropForeignKey
ALTER TABLE "subscription_history_events" DROP CONSTRAINT "subscription_history_events_gym_id_fkey";

-- DropForeignKey
ALTER TABLE "subscription_history_events" DROP CONSTRAINT "subscription_history_events_subscription_id_fkey";

-- DropForeignKey
ALTER TABLE "workout_exercise_sets" DROP CONSTRAINT "workout_exercise_sets_workout_exercise_id_fkey";

-- DropForeignKey
ALTER TABLE "workout_plan_exercises" DROP CONSTRAINT "workout_plan_exercises_exercise_id_fkey";

-- DropForeignKey
ALTER TABLE "workout_plan_exercises" DROP CONSTRAINT "workout_plan_exercises_workout_id_fkey";

-- DropIndex
DROP INDEX "GymPlan_name_trgm_idx";

-- DropIndex
DROP INDEX "User_fullName_trgm_idx";

-- DropIndex
DROP INDEX "User_phone_trgm_idx";

-- AlterTable
ALTER TABLE "Enquiry" ALTER COLUMN "updatedAt" DROP DEFAULT;

-- AlterTable
ALTER TABLE "Expense" ALTER COLUMN "updatedAt" DROP DEFAULT;

-- AlterTable
ALTER TABLE "Gym" ALTER COLUMN "qrSigningSecret" DROP DEFAULT;

-- AlterTable
ALTER TABLE "GymFeature" ALTER COLUMN "updatedAt" DROP DEFAULT;

-- AlterTable
ALTER TABLE "Payment" ALTER COLUMN "updatedAt" DROP DEFAULT;

-- AlterTable
ALTER TABLE "TrainerProfile" ALTER COLUMN "updatedAt" DROP DEFAULT;

-- AlterTable
ALTER TABLE "TrainerSalaryPayment" ALTER COLUMN "updatedAt" DROP DEFAULT;

-- DropTable
DROP TABLE "exercise_master";

-- DropTable
DROP TABLE "gym_products";

-- DropTable
DROP TABLE "meal_food_items";

-- DropTable
DROP TABLE "member_diets";

-- DropTable
DROP TABLE "member_meals";

-- DropTable
DROP TABLE "member_workout_plans";

-- DropTable
DROP TABLE "member_workouts";

-- DropTable
DROP TABLE "subscription_history_events";

-- DropTable
DROP TABLE "workout_exercise_sets";

-- DropTable
DROP TABLE "workout_plan_exercises";

-- DropEnum
DROP TYPE "GymProductStatus";

-- AddForeignKey
ALTER TABLE "MemberSubscription" ADD CONSTRAINT "MemberSubscription_planId_fkey" FOREIGN KEY ("planId") REFERENCES "SubscriptionPlan"("id") ON DELETE SET NULL ON UPDATE CASCADE;
