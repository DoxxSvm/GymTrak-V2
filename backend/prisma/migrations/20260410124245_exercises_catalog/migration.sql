-- CreateEnum
CREATE TYPE "ExerciseEquipment" AS ENUM ('NONE', 'BARBELL', 'DUMBBELL', 'KETTLEBELL', 'MACHINE', 'PLATE', 'RESISTANCE_BAND', 'SUSPENSION_BAND', 'OTHER');

-- CreateEnum
CREATE TYPE "Muscle" AS ENUM ('ABDOMINALS', 'ABDUCTORS', 'ADDUCTORS', 'BICEPS', 'CALVES', 'CARDIO', 'CHEST', 'FOREARMS', 'FULL_BODY', 'GLUTES', 'HAMSTRINGS', 'LATS', 'LOWER_BACK', 'NECK', 'QUADRICEPS', 'SHOULDERS', 'TRAPS', 'TRICEPS', 'UPPER_BACK', 'OTHER');

-- CreateEnum
CREATE TYPE "ExerciseType" AS ENUM ('WEIGHT_REPS', 'BODYWEIGHT_REPS', 'WEIGHTED_BODYWEIGHT', 'ASSISTED_BODYWEIGHT', 'DURATION', 'DURATION_WEIGHT', 'DISTANCE_DURATION', 'WEIGHT_DISTANCE');

-- CreateTable
CREATE TABLE "exercises" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "assetUrl" TEXT,
    "equipment" "ExerciseEquipment" NOT NULL,
    "primaryMuscle" "Muscle" NOT NULL,
    "exerciseType" "ExerciseType" NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "exercises_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "exercise_secondary_muscles" (
    "id" TEXT NOT NULL,
    "exerciseId" TEXT NOT NULL,
    "muscle" "Muscle" NOT NULL,

    CONSTRAINT "exercise_secondary_muscles_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "exercises_gymId_name_idx" ON "exercises"("gymId", "name");

-- CreateIndex
CREATE INDEX "exercises_gymId_equipment_idx" ON "exercises"("gymId", "equipment");

-- CreateIndex
CREATE INDEX "exercises_gymId_primaryMuscle_idx" ON "exercises"("gymId", "primaryMuscle");

-- CreateIndex
CREATE INDEX "exercises_gymId_isActive_idx" ON "exercises"("gymId", "isActive");

-- CreateIndex
CREATE INDEX "exercise_secondary_muscles_exerciseId_idx" ON "exercise_secondary_muscles"("exerciseId");

-- CreateIndex
CREATE UNIQUE INDEX "exercise_secondary_muscles_exerciseId_muscle_key" ON "exercise_secondary_muscles"("exerciseId", "muscle");

-- AddForeignKey
ALTER TABLE "exercises" ADD CONSTRAINT "exercises_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "exercise_secondary_muscles" ADD CONSTRAINT "exercise_secondary_muscles_exerciseId_fkey" FOREIGN KEY ("exerciseId") REFERENCES "exercises"("id") ON DELETE CASCADE ON UPDATE CASCADE;
