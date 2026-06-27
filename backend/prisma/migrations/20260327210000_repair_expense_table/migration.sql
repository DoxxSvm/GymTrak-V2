-- Repair drift: Expense + SaaS enums (migration history out of sync with DB)

DO $$ BEGIN
  CREATE TYPE "GymTrakSaasTier" AS ENUM ('BASIC', 'PLUS', 'PREMIUM');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE TYPE "ExpenseCategory" AS ENUM (
    'RENT', 'UTILITIES', 'EQUIPMENT', 'MAINTENANCE', 'SUPPLIES',
    'SALARY', 'MARKETING', 'SOFTWARE', 'OTHER'
  );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TYPE "PaymentMethod" ADD VALUE 'CARD';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

ALTER TABLE "SubscriptionPlan" ADD COLUMN IF NOT EXISTS "saasTier" "GymTrakSaasTier";
CREATE INDEX IF NOT EXISTS "SubscriptionPlan_saasTier_idx" ON "SubscriptionPlan"("saasTier");

CREATE TABLE IF NOT EXISTS "Expense" (
  "id" TEXT NOT NULL,
  "gymId" TEXT NOT NULL,
  "amountCents" INTEGER NOT NULL,
  "currency" TEXT NOT NULL DEFAULT 'USD',
  "category" "ExpenseCategory" NOT NULL,
  "description" TEXT,
  "occurredOn" DATE NOT NULL,
  "method" "PaymentMethod",
  "trainerGymUserId" TEXT,
  "recordedByUserId" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "Expense_pkey" PRIMARY KEY ("id")
);

CREATE INDEX IF NOT EXISTS "Expense_gymId_occurredOn_idx" ON "Expense"("gymId", "occurredOn");
CREATE INDEX IF NOT EXISTS "Expense_gymId_category_idx" ON "Expense"("gymId", "category");
CREATE INDEX IF NOT EXISTS "Expense_trainerGymUserId_idx" ON "Expense"("trainerGymUserId");

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'Expense_gymId_fkey') THEN
    ALTER TABLE "Expense" ADD CONSTRAINT "Expense_gymId_fkey"
      FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'Expense_recordedByUserId_fkey') THEN
    ALTER TABLE "Expense" ADD CONSTRAINT "Expense_recordedByUserId_fkey"
      FOREIGN KEY ("recordedByUserId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'Expense_trainerGymUserId_fkey') THEN
    ALTER TABLE "Expense" ADD CONSTRAINT "Expense_trainerGymUserId_fkey"
      FOREIGN KEY ("trainerGymUserId") REFERENCES "GymUser"("id") ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;
