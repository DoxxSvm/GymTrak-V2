-- Repair drift: Enquiry pipeline columns / enums expected by Prisma but missing on DB
-- (e.g. migration marked applied without ALTER TABLE succeeding). Idempotent where supported.

-- EnquiryStatus enum values (ignore if already present)
DO $$ BEGIN ALTER TYPE "EnquiryStatus" ADD VALUE 'CONTACTED'; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER TYPE "EnquiryStatus" ADD VALUE 'QUALIFIED'; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER TYPE "EnquiryStatus" ADD VALUE 'TRIAL'; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER TYPE "EnquiryStatus" ADD VALUE 'FOLLOW_UP'; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER TYPE "EnquiryStatus" ADD VALUE 'LOST'; EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE "Enquiry" ADD COLUMN IF NOT EXISTS "source" TEXT;
ALTER TABLE "Enquiry" ADD COLUMN IF NOT EXISTS "notes" TEXT;
ALTER TABLE "Enquiry" ADD COLUMN IF NOT EXISTS "assignedToUserId" TEXT;
ALTER TABLE "Enquiry" ADD COLUMN IF NOT EXISTS "followUpAt" TIMESTAMP(3);
ALTER TABLE "Enquiry" ADD COLUMN IF NOT EXISTS "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE "Enquiry" ADD COLUMN IF NOT EXISTS "convertedGymUserId" TEXT;

CREATE INDEX IF NOT EXISTS "Enquiry_gymId_followUpAt_idx" ON "Enquiry"("gymId", "followUpAt");
CREATE INDEX IF NOT EXISTS "Enquiry_assignedToUserId_idx" ON "Enquiry"("assignedToUserId");
CREATE UNIQUE INDEX IF NOT EXISTS "Enquiry_convertedGymUserId_key" ON "Enquiry"("convertedGymUserId");

DO $$ BEGIN
  ALTER TABLE "Enquiry" ADD CONSTRAINT "Enquiry_assignedToUserId_fkey" FOREIGN KEY ("assignedToUserId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE "Enquiry" ADD CONSTRAINT "Enquiry_convertedGymUserId_fkey" FOREIGN KEY ("convertedGymUserId") REFERENCES "GymUser"("id") ON DELETE SET NULL ON UPDATE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
