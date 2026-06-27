-- CreateEnum
CREATE TYPE "MemberSubscriptionStatus" AS ENUM ('SCHEDULED', 'ACTIVE', 'ENDED', 'CANCELED');

-- AlterTable
ALTER TABLE "GymUser" ADD COLUMN "isLead" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "GymUser" ADD COLUMN "notes" TEXT;
ALTER TABLE "GymUser" ADD COLUMN "emergencyContactName" TEXT;
ALTER TABLE "GymUser" ADD COLUMN "emergencyContactPhone" TEXT;
ALTER TABLE "GymUser" ADD COLUMN "dateOfBirth" DATE;
ALTER TABLE "GymUser" ADD COLUMN "gender" TEXT;

-- CreateIndex
CREATE INDEX "GymUser_gymId_isLead_idx" ON "GymUser"("gymId", "isLead");

-- CreateTable
CREATE TABLE "MemberSubscription" (
    "id" TEXT NOT NULL,
    "gymUserId" TEXT NOT NULL,
    "planId" TEXT NOT NULL,
    "status" "MemberSubscriptionStatus" NOT NULL DEFAULT 'ACTIVE',
    "startsAt" TIMESTAMP(3) NOT NULL,
    "endsAt" TIMESTAMP(3) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "MemberSubscription_pkey" PRIMARY KEY ("id")
);

-- AlterTable
ALTER TABLE "Payment" ADD COLUMN "memberSubscriptionId" TEXT;

-- CreateIndex
CREATE INDEX "MemberSubscription_gymUserId_startsAt_idx" ON "MemberSubscription"("gymUserId", "startsAt");
CREATE INDEX "MemberSubscription_gymUserId_endsAt_idx" ON "MemberSubscription"("gymUserId", "endsAt");
CREATE INDEX "Payment_memberSubscriptionId_idx" ON "Payment"("memberSubscriptionId");

-- AddForeignKey
ALTER TABLE "MemberSubscription" ADD CONSTRAINT "MemberSubscription_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "MemberSubscription" ADD CONSTRAINT "MemberSubscription_planId_fkey" FOREIGN KEY ("planId") REFERENCES "SubscriptionPlan"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "Payment" ADD CONSTRAINT "Payment_memberSubscriptionId_fkey" FOREIGN KEY ("memberSubscriptionId") REFERENCES "MemberSubscription"("id") ON DELETE SET NULL ON UPDATE CASCADE;
