-- CreateTable
CREATE TABLE "diet_food_consumptions" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "gymUserId" TEXT NOT NULL,
    "dietFoodId" TEXT,
    "name" TEXT NOT NULL,
    "weightKg" DECIMAL(10,2),
    "calories" INTEGER NOT NULL,
    "quantity" INTEGER NOT NULL DEFAULT 1,
    "portion_label" TEXT,
    "protein_g" DECIMAL(10,2),
    "carbs_g" DECIMAL(10,2),
    "fat_g" DECIMAL(10,2),
    "mealType" "DietMealType" NOT NULL,
    "consumedOn" DATE NOT NULL,
    "consumedAt" TIMESTAMP(3) NOT NULL,
    "image_url" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "diet_food_consumptions_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "diet_food_consumptions_gymUserId_consumedOn_idx" ON "diet_food_consumptions"("gymUserId", "consumedOn");

-- CreateIndex
CREATE INDEX "diet_food_consumptions_gymId_consumedOn_idx" ON "diet_food_consumptions"("gymId", "consumedOn");

-- AddForeignKey
ALTER TABLE "diet_food_consumptions" ADD CONSTRAINT "diet_food_consumptions_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_food_consumptions" ADD CONSTRAINT "diet_food_consumptions_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_food_consumptions" ADD CONSTRAINT "diet_food_consumptions_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "diet_food_consumptions" ADD CONSTRAINT "diet_food_consumptions_dietFoodId_fkey" FOREIGN KEY ("dietFoodId") REFERENCES "diet_foods"("id") ON DELETE SET NULL ON UPDATE CASCADE;
