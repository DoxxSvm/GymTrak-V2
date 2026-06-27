/*
  Warnings:

  - Added the required column `userId` to the `diet_foods` table without a default value. This is not possible if the table is not empty.
  - Added the required column `userId` to the `diet_meal_food_lines` table without a default value. This is not possible if the table is not empty.
  - Added the required column `userId` to the `diet_meals` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "diet_foods" ADD COLUMN     "userId" TEXT NOT NULL,
ALTER COLUMN "gymId" DROP NOT NULL;

-- AlterTable
ALTER TABLE "diet_meal_food_lines" ADD COLUMN     "userId" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "diet_meals" ADD COLUMN     "userId" TEXT NOT NULL,
ALTER COLUMN "gymId" DROP NOT NULL;
