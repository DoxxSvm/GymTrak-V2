-- CreateEnum
CREATE TYPE "DietFoodUnitType" AS ENUM ('KG', 'LITER', 'GRAM');

-- AlterTable
ALTER TABLE "diet_foods"
ADD COLUMN "protein" DECIMAL(10,2),
ADD COLUMN "carbs" DECIMAL(10,2),
ADD COLUMN "fat" DECIMAL(10,2),
ADD COLUMN "unitType" "DietFoodUnitType";
