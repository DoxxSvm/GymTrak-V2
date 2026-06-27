-- CreateEnum
CREATE TYPE "DietMealType" AS ENUM ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK');

-- CreateTable
CREATE TABLE "diet_foods" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "weightKg" DECIMAL(10,2),
    "calories" INTEGER NOT NULL,
    "quantity" INTEGER NOT NULL DEFAULT 1,
    "imageUrl" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "diet_foods_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "diet_meals" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "gymUserId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "mealTime" TEXT NOT NULL,
    "mealType" "DietMealType" NOT NULL,
    "repeatEnabled" BOOLEAN NOT NULL DEFAULT false,
    "repeatDays" INTEGER[] DEFAULT ARRAY[]::INTEGER[],
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "diet_meals_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "diet_meal_food_lines" (
    "id" TEXT NOT NULL,
    "mealId" TEXT NOT NULL,
    "dietFoodId" TEXT,
    "name" TEXT NOT NULL,
    "weightKg" DECIMAL(10,2),
    "calories" INTEGER NOT NULL,
    "quantity" INTEGER NOT NULL DEFAULT 1,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "diet_meal_food_lines_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "diet_foods_gymId_name_idx" ON "diet_foods"("gymId", "name");

-- CreateIndex
CREATE INDEX "diet_meals_gymId_gymUserId_idx" ON "diet_meals"("gymId", "gymUserId");

-- CreateIndex
CREATE INDEX "diet_meal_food_lines_mealId_idx" ON "diet_meal_food_lines"("mealId");

-- AddForeignKey
ALTER TABLE "diet_foods" ADD CONSTRAINT "diet_foods_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_meals" ADD CONSTRAINT "diet_meals_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_meals" ADD CONSTRAINT "diet_meals_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_meal_food_lines" ADD CONSTRAINT "diet_meal_food_lines_mealId_fkey" FOREIGN KEY ("mealId") REFERENCES "diet_meals"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_meal_food_lines" ADD CONSTRAINT "diet_meal_food_lines_dietFoodId_fkey" FOREIGN KEY ("dietFoodId") REFERENCES "diet_foods"("id") ON DELETE SET NULL ON UPDATE CASCADE;
