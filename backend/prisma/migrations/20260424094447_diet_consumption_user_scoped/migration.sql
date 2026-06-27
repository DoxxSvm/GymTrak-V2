/*
  Warnings:

  - You are about to drop the column `gymUserId` on the `diet_food_consumptions` table. All the data in the column will be lost.

*/
-- DropForeignKey
ALTER TABLE "diet_food_consumptions" DROP CONSTRAINT "diet_food_consumptions_gymId_fkey";

-- DropForeignKey
ALTER TABLE "diet_food_consumptions" DROP CONSTRAINT "diet_food_consumptions_gymUserId_fkey";

-- DropIndex
DROP INDEX "diet_food_consumptions_gymUserId_consumedOn_idx";

-- AlterTable
ALTER TABLE "diet_food_consumptions" DROP COLUMN "gymUserId",
ALTER COLUMN "gymId" DROP NOT NULL;

-- CreateIndex
CREATE INDEX "diet_food_consumptions_userId_consumedOn_idx" ON "diet_food_consumptions"("userId", "consumedOn");

-- AddForeignKey
ALTER TABLE "diet_food_consumptions" ADD CONSTRAINT "diet_food_consumptions_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE SET NULL ON UPDATE CASCADE;
