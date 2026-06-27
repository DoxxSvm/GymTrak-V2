-- CreateTable
CREATE TABLE "member_personal_exercises" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "assetUrl" TEXT,
    "equipment" "ExerciseEquipment" NOT NULL,
    "primaryMuscle" "Muscle" NOT NULL,
    "exerciseType" "ExerciseType" NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "member_personal_exercises_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "member_personal_exercise_secondary_muscles" (
    "id" TEXT NOT NULL,
    "exerciseId" TEXT NOT NULL,
    "muscle" "Muscle" NOT NULL,

    CONSTRAINT "member_personal_exercise_secondary_muscles_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "member_personal_workout_plans" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "notes" TEXT,
    "startedAt" TIMESTAMP(3),
    "endedAt" TIMESTAMP(3),
    "completed" BOOLEAN NOT NULL DEFAULT false,
    "totalVolume" INTEGER NOT NULL DEFAULT 0,
    "totalSets" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "member_personal_workout_plans_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "member_personal_workout_plan_exercises" (
    "id" TEXT NOT NULL,
    "workoutId" TEXT NOT NULL,
    "exerciseId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "member_personal_workout_plan_exercises_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "member_personal_workout_exercise_sets" (
    "id" TEXT NOT NULL,
    "workoutExerciseId" TEXT NOT NULL,
    "setNumber" INTEGER NOT NULL,
    "reps" INTEGER NOT NULL DEFAULT 0,
    "weight" INTEGER NOT NULL DEFAULT 0,
    "completed" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "member_personal_workout_exercise_sets_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "member_personal_exercises_userId_name_idx" ON "member_personal_exercises"("userId", "name");

-- CreateIndex
CREATE INDEX "member_personal_exercises_userId_isActive_idx" ON "member_personal_exercises"("userId", "isActive");

-- CreateIndex
CREATE INDEX "member_personal_exercise_secondary_muscles_exerciseId_idx" ON "member_personal_exercise_secondary_muscles"("exerciseId");

-- CreateIndex
CREATE UNIQUE INDEX "member_personal_exercise_secondary_muscles_exerciseId_muscl_key" ON "member_personal_exercise_secondary_muscles"("exerciseId", "muscle");

-- CreateIndex
CREATE INDEX "member_personal_workout_plans_userId_idx" ON "member_personal_workout_plans"("userId");

-- CreateIndex
CREATE INDEX "member_personal_workout_plan_exercises_workoutId_idx" ON "member_personal_workout_plan_exercises"("workoutId");

-- CreateIndex
CREATE INDEX "member_personal_workout_plan_exercises_exerciseId_idx" ON "member_personal_workout_plan_exercises"("exerciseId");

-- CreateIndex
CREATE INDEX "member_personal_workout_exercise_sets_workoutExerciseId_idx" ON "member_personal_workout_exercise_sets"("workoutExerciseId");

-- AddForeignKey
ALTER TABLE "member_personal_exercises" ADD CONSTRAINT "member_personal_exercises_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_personal_exercise_secondary_muscles" ADD CONSTRAINT "member_personal_exercise_secondary_muscles_exerciseId_fkey" FOREIGN KEY ("exerciseId") REFERENCES "member_personal_exercises"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_personal_workout_plans" ADD CONSTRAINT "member_personal_workout_plans_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_personal_workout_plan_exercises" ADD CONSTRAINT "member_personal_workout_plan_exercises_workoutId_fkey" FOREIGN KEY ("workoutId") REFERENCES "member_personal_workout_plans"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_personal_workout_plan_exercises" ADD CONSTRAINT "member_personal_workout_plan_exercises_exerciseId_fkey" FOREIGN KEY ("exerciseId") REFERENCES "member_personal_exercises"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_personal_workout_exercise_sets" ADD CONSTRAINT "member_personal_workout_exercise_sets_workoutExerciseId_fkey" FOREIGN KEY ("workoutExerciseId") REFERENCES "member_personal_workout_plan_exercises"("id") ON DELETE CASCADE ON UPDATE CASCADE;
