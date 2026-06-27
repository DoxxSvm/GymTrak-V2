-- CreateEnum
CREATE TYPE "SalaryPeriod" AS ENUM ('HOURLY', 'WEEKLY', 'MONTHLY', 'YEARLY');

-- CreateEnum
CREATE TYPE "GymFeatureKey" AS ENUM ('trainers', 'trainer_shifts', 'trainer_attendance', 'trainer_payroll');

-- CreateTable
CREATE TABLE "GymFeature" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "key" "GymFeatureKey" NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT true,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "GymFeature_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GymExpertiseTag" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "GymExpertiseTag_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TrainerExpertise" (
    "gymUserId" TEXT NOT NULL,
    "tagId" TEXT NOT NULL,

    CONSTRAINT "TrainerExpertise_pkey" PRIMARY KEY ("gymUserId","tagId")
);

-- CreateTable
CREATE TABLE "TrainerProfile" (
    "gymUserId" TEXT NOT NULL,
    "salaryCents" INTEGER,
    "salaryPeriod" "SalaryPeriod",
    "contractStartsAt" DATE,
    "contractEndsAt" DATE,
    "notes" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "TrainerProfile_pkey" PRIMARY KEY ("gymUserId")
);

-- CreateTable
CREATE TABLE "TrainerShift" (
    "id" TEXT NOT NULL,
    "gymUserId" TEXT NOT NULL,
    "dayOfWeek" INTEGER NOT NULL,
    "startTime" TEXT NOT NULL,
    "endTime" TEXT NOT NULL,

    CONSTRAINT "TrainerShift_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TrainerPlanAssignment" (
    "gymUserId" TEXT NOT NULL,
    "planId" TEXT NOT NULL,

    CONSTRAINT "TrainerPlanAssignment_pkey" PRIMARY KEY ("gymUserId","planId")
);

-- CreateTable
CREATE TABLE "TrainerAttendanceRecord" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "trainerUserId" TEXT NOT NULL,
    "attendedOn" DATE NOT NULL,
    "checkedInAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "TrainerAttendanceRecord_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TrainerSalaryPayment" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "gymUserId" TEXT NOT NULL,
    "amountCents" INTEGER NOT NULL,
    "currency" TEXT NOT NULL DEFAULT 'USD',
    "periodStart" DATE NOT NULL,
    "periodEnd" DATE NOT NULL,
    "paidAt" TIMESTAMP(3),
    "description" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "TrainerSalaryPayment_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "GymFeature_gymId_key_key" ON "GymFeature"("gymId", "key");

-- CreateIndex
CREATE INDEX "GymFeature_gymId_idx" ON "GymFeature"("gymId");

-- CreateIndex
CREATE UNIQUE INDEX "GymExpertiseTag_gymId_name_key" ON "GymExpertiseTag"("gymId", "name");

-- CreateIndex
CREATE INDEX "GymExpertiseTag_gymId_idx" ON "GymExpertiseTag"("gymId");

-- CreateIndex
CREATE INDEX "TrainerShift_gymUserId_idx" ON "TrainerShift"("gymUserId");

-- CreateIndex
CREATE UNIQUE INDEX "TrainerAttendanceRecord_gymId_trainerUserId_attendedOn_key" ON "TrainerAttendanceRecord"("gymId", "trainerUserId", "attendedOn");

-- CreateIndex
CREATE INDEX "TrainerAttendanceRecord_gymId_attendedOn_idx" ON "TrainerAttendanceRecord"("gymId", "attendedOn");

-- CreateIndex
CREATE INDEX "TrainerSalaryPayment_gymId_gymUserId_idx" ON "TrainerSalaryPayment"("gymId", "gymUserId");

-- CreateIndex
CREATE INDEX "TrainerSalaryPayment_gymId_periodStart_idx" ON "TrainerSalaryPayment"("gymId", "periodStart");

-- AddForeignKey
ALTER TABLE "GymFeature" ADD CONSTRAINT "GymFeature_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GymExpertiseTag" ADD CONSTRAINT "GymExpertiseTag_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerExpertise" ADD CONSTRAINT "TrainerExpertise_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerExpertise" ADD CONSTRAINT "TrainerExpertise_tagId_fkey" FOREIGN KEY ("tagId") REFERENCES "GymExpertiseTag"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerProfile" ADD CONSTRAINT "TrainerProfile_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerShift" ADD CONSTRAINT "TrainerShift_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerPlanAssignment" ADD CONSTRAINT "TrainerPlanAssignment_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerPlanAssignment" ADD CONSTRAINT "TrainerPlanAssignment_planId_fkey" FOREIGN KEY ("planId") REFERENCES "SubscriptionPlan"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerAttendanceRecord" ADD CONSTRAINT "TrainerAttendanceRecord_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerAttendanceRecord" ADD CONSTRAINT "TrainerAttendanceRecord_trainerUserId_fkey" FOREIGN KEY ("trainerUserId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerSalaryPayment" ADD CONSTRAINT "TrainerSalaryPayment_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerSalaryPayment" ADD CONSTRAINT "TrainerSalaryPayment_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Seed RBAC permission codes for trainer/staff UI toggles
INSERT INTO "Permission" ("id", "code", "description", "module") VALUES
  ('cm_tr_dash_access', 'dashboard:access', 'View dashboard', 'dashboard'),
  ('cm_tr_pay_access', 'payments:access', 'View and record payments', 'payments'),
  ('cm_tr_mem_manage', 'members:manage', 'Manage members', 'members')
ON CONFLICT ("code") DO NOTHING;
