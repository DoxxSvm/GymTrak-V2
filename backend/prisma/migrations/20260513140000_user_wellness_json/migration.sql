-- Optional JSON snapshot: BMI, bmiCategory, maintenanceCalories (see member-wellness.util).
ALTER TABLE "User" ADD COLUMN "wellness" JSONB;
