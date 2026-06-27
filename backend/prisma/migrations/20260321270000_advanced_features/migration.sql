-- Message templates, gym profile geo, trainer leave, attendance QR signing secret

-- CreateEnum
CREATE TYPE "MessageTemplateKind" AS ENUM ('WELCOME', 'EXPIRY_REMINDER', 'PAYMENT_CONFIRMATION');

-- CreateEnum
CREATE TYPE "TrainerLeaveStatus" AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');

-- AlterTable
ALTER TABLE "Gym" ADD COLUMN "address" TEXT;
ALTER TABLE "Gym" ADD COLUMN "latitude" DECIMAL(10, 7);
ALTER TABLE "Gym" ADD COLUMN "longitude" DECIMAL(10, 7);
ALTER TABLE "Gym" ADD COLUMN "qrSigningSecret" TEXT NOT NULL DEFAULT gen_random_uuid()::text;

-- CreateTable
CREATE TABLE "GymMessageTemplate" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "kind" "MessageTemplateKind" NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT true,
    "overrideBody" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "GymMessageTemplate_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TrainerLeave" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "trainerGymUserId" TEXT NOT NULL,
    "startsAt" TIMESTAMP(3) NOT NULL,
    "endsAt" TIMESTAMP(3) NOT NULL,
    "reason" TEXT,
    "status" "TrainerLeaveStatus" NOT NULL DEFAULT 'PENDING',
    "decidedAt" TIMESTAMP(3),
    "decidedByUserId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "TrainerLeave_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "GymMessageTemplate_gymId_kind_key" ON "GymMessageTemplate"("gymId", "kind");

-- CreateIndex
CREATE INDEX "GymMessageTemplate_gymId_idx" ON "GymMessageTemplate"("gymId");

-- CreateIndex
CREATE INDEX "TrainerLeave_gymId_status_idx" ON "TrainerLeave"("gymId", "status");

-- CreateIndex
CREATE INDEX "TrainerLeave_trainerGymUserId_idx" ON "TrainerLeave"("trainerGymUserId");

-- AddForeignKey
ALTER TABLE "GymMessageTemplate" ADD CONSTRAINT "GymMessageTemplate_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerLeave" ADD CONSTRAINT "TrainerLeave_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrainerLeave" ADD CONSTRAINT "TrainerLeave_trainerGymUserId_fkey" FOREIGN KEY ("trainerGymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;
