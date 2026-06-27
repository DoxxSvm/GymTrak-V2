-- CreateEnum
CREATE TYPE "PlanType" AS ENUM ('GYM_MEMBERSHIP', 'PT_PLAN', 'BATCH_PLAN', 'FREE_TRIAL');

-- CreateEnum
CREATE TYPE "BatchPlanGender" AS ENUM ('ANY', 'MALE', 'FEMALE', 'MIXED');

-- CreateTable
CREATE TABLE "GymPlan" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "type" "PlanType" NOT NULL,
    "name" TEXT NOT NULL,
    "durationDays" INTEGER NOT NULL,
    "priceCents" INTEGER NOT NULL,
    "currency" TEXT NOT NULL DEFAULT 'USD',
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "trainerGymUserId" TEXT,
    "batchDaysOfWeek" INTEGER[] NOT NULL DEFAULT ARRAY[]::INTEGER[],
    "batchGender" "BatchPlanGender",
    "metadata" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "GymPlan_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GymPlanShift" (
    "id" TEXT NOT NULL,
    "planId" TEXT NOT NULL,
    "startTime" TEXT NOT NULL,
    "endTime" TEXT NOT NULL,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "GymPlanShift_pkey" PRIMARY KEY ("id")
);

-- DropForeignKey
ALTER TABLE "MemberSubscription" DROP CONSTRAINT "MemberSubscription_planId_fkey";

-- AlterTable
ALTER TABLE "MemberSubscription" ALTER COLUMN "planId" DROP NOT NULL;
ALTER TABLE "MemberSubscription" ADD COLUMN     "gymPlanId" TEXT;

-- CreateIndex
CREATE INDEX "GymPlan_gymId_type_idx" ON "GymPlan"("gymId", "type");

-- CreateIndex
CREATE INDEX "GymPlan_gymId_isActive_idx" ON "GymPlan"("gymId", "isActive");

-- CreateIndex
CREATE INDEX "GymPlanShift_planId_idx" ON "GymPlanShift"("planId");

-- CreateIndex
CREATE INDEX "MemberSubscription_gymPlanId_idx" ON "MemberSubscription"("gymPlanId");

-- AddForeignKey
ALTER TABLE "GymPlan" ADD CONSTRAINT "GymPlan_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GymPlan" ADD CONSTRAINT "GymPlan_trainerGymUserId_fkey" FOREIGN KEY ("trainerGymUserId") REFERENCES "GymUser"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GymPlanShift" ADD CONSTRAINT "GymPlanShift_planId_fkey" FOREIGN KEY ("planId") REFERENCES "GymPlan"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MemberSubscription" ADD CONSTRAINT "MemberSubscription_planId_fkey" FOREIGN KEY ("planId") REFERENCES "SubscriptionPlan"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MemberSubscription" ADD CONSTRAINT "MemberSubscription_gymPlanId_fkey" FOREIGN KEY ("gymPlanId") REFERENCES "GymPlan"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
