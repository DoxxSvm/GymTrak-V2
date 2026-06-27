-- AlterTable
ALTER TABLE "diet_meal_food_lines"
ADD COLUMN "protein" DECIMAL(10,2),
ADD COLUMN "carbs" DECIMAL(10,2),
ADD COLUMN "fat" DECIMAL(10,2),
ADD COLUMN "unitType" "DietFoodUnitType";

-- Backfill from linked catalog rows
UPDATE "diet_meal_food_lines" AS l
SET
  "protein" = f."protein",
  "carbs" = f."carbs",
  "fat" = f."fat",
  "unitType" = f."unitType"
FROM "diet_foods" AS f
WHERE l."dietFoodId" = f."id";
