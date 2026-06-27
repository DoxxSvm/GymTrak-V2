-- CreateTable
CREATE TABLE "member_workout_plans" (
    "id" TEXT NOT NULL,
    "gym_id" TEXT NOT NULL,
    "gym_user_id" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "notes" TEXT,
    "started_at" TIMESTAMP(3),
    "ended_at" TIMESTAMP(3),
    "completed" BOOLEAN NOT NULL DEFAULT false,
    "total_volume" INTEGER NOT NULL DEFAULT 0,
    "total_sets" INTEGER NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "member_workout_plans_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "workout_plan_exercises" (
    "id" TEXT NOT NULL,
    "workout_id" TEXT NOT NULL,
    "exercise_id" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "workout_plan_exercises_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "workout_exercise_sets" (
    "id" TEXT NOT NULL,
    "workout_exercise_id" TEXT NOT NULL,
    "set_number" INTEGER NOT NULL,
    "reps" INTEGER NOT NULL DEFAULT 0,
    "weight" INTEGER NOT NULL DEFAULT 0,
    "completed" BOOLEAN NOT NULL DEFAULT false,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "workout_exercise_sets_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "member_workout_plans_gym_user_id_idx" ON "member_workout_plans"("gym_user_id");

-- CreateIndex
CREATE INDEX "member_workout_plans_gym_id_idx" ON "member_workout_plans"("gym_id");

-- CreateIndex
CREATE INDEX "workout_plan_exercises_workout_id_idx" ON "workout_plan_exercises"("workout_id");

-- CreateIndex
CREATE INDEX "workout_plan_exercises_exercise_id_idx" ON "workout_plan_exercises"("exercise_id");

-- CreateIndex
CREATE INDEX "workout_exercise_sets_workout_exercise_id_idx" ON "workout_exercise_sets"("workout_exercise_id");

-- AddForeignKey
ALTER TABLE "member_workout_plans" ADD CONSTRAINT "member_workout_plans_gym_id_fkey" FOREIGN KEY ("gym_id") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_workout_plans" ADD CONSTRAINT "member_workout_plans_gym_user_id_fkey" FOREIGN KEY ("gym_user_id") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "workout_plan_exercises" ADD CONSTRAINT "workout_plan_exercises_workout_id_fkey" FOREIGN KEY ("workout_id") REFERENCES "member_workout_plans"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "workout_plan_exercises" ADD CONSTRAINT "workout_plan_exercises_exercise_id_fkey" FOREIGN KEY ("exercise_id") REFERENCES "exercises"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "workout_exercise_sets" ADD CONSTRAINT "workout_exercise_sets_workout_exercise_id_fkey" FOREIGN KEY ("workout_exercise_id") REFERENCES "workout_plan_exercises"("id") ON DELETE CASCADE ON UPDATE CASCADE;
