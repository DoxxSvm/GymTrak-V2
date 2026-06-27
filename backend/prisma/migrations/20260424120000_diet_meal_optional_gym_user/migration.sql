-- DietMeal: allow user-scoped rows (gymId null) without a GymUser link
ALTER TABLE "diet_meals" ALTER COLUMN "gymUserId" DROP NOT NULL;
