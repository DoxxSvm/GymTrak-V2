-- GymTrak SaaS tiers + expense tracking + lead pipeline fields

-- CreateEnum
CREATE TYPE "GymTrakSaasTier" AS ENUM ('BASIC', 'PLUS', 'PREMIUM');

-- CreateEnum
CREATE TYPE "ExpenseCategory" AS ENUM (
  'RENT',
  'UTILITIES',
  'EQUIPMENT',
  'MAINTENANCE',
  'SUPPLIES',
  'SALARY',
  'MARKETING',
  'SOFTWARE',
  'OTHER'
);

-- AlterEnum (append values; existing rows keep OPEN / CONVERTED / CLOSED)
ALTER TYPE "EnquiryStatus" ADD VALUE 'CONTACTED';
ALTER TYPE "EnquiryStatus" ADD VALUE 'QUALIFIED';
ALTER TYPE "EnquiryStatus" ADD VALUE 'TRIAL';
ALTER TYPE "EnquiryStatus" ADD VALUE 'FOLLOW_UP';
ALTER TYPE "EnquiryStatus" ADD VALUE 'LOST';

-- AlterTable
ALTER TABLE "SubscriptionPlan" ADD COLUMN "saasTier" "GymTrakSaasTier";

-- CreateIndex
CREATE INDEX "SubscriptionPlan_saasTier_idx" ON "SubscriptionPlan"("saasTier");

-- AlterTable
ALTER TABLE "Enquiry" ADD COLUMN "source" TEXT;
ALTER TABLE "Enquiry" ADD COLUMN "notes" TEXT;
ALTER TABLE "Enquiry" ADD COLUMN "assignedToUserId" TEXT;
ALTER TABLE "Enquiry" ADD COLUMN "followUpAt" TIMESTAMP(3);
ALTER TABLE "Enquiry" ADD COLUMN "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE "Enquiry" ADD COLUMN "convertedGymUserId" TEXT;

-- CreateIndex
CREATE INDEX "Enquiry_gymId_followUpAt_idx" ON "Enquiry"("gymId", "followUpAt");
CREATE INDEX "Enquiry_assignedToUserId_idx" ON "Enquiry"("assignedToUserId");

-- CreateIndex
CREATE UNIQUE INDEX "Enquiry_convertedGymUserId_key" ON "Enquiry"("convertedGymUserId");

-- AddForeignKey
ALTER TABLE "Enquiry" ADD CONSTRAINT "Enquiry_assignedToUserId_fkey" FOREIGN KEY ("assignedToUserId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE "Enquiry" ADD CONSTRAINT "Enquiry_convertedGymUserId_fkey" FOREIGN KEY ("convertedGymUserId") REFERENCES "GymUser"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- CreateTable
CREATE TABLE "Expense" (
  "id" TEXT NOT NULL,
  "gymId" TEXT NOT NULL,
  "amountCents" INTEGER NOT NULL,
  "currency" TEXT NOT NULL DEFAULT 'USD',
  "category" "ExpenseCategory" NOT NULL,
  "description" TEXT,
  "occurredOn" DATE NOT NULL,
  "recordedByUserId" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT "Expense_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "Expense_gymId_occurredOn_idx" ON "Expense"("gymId", "occurredOn");
CREATE INDEX "Expense_gymId_category_idx" ON "Expense"("gymId", "category");

-- AddForeignKey
ALTER TABLE "Expense" ADD CONSTRAINT "Expense_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "Expense" ADD CONSTRAINT "Expense_recordedByUserId_fkey" FOREIGN KEY ("recordedByUserId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
