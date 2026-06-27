ALTER TABLE "Enquiry"
  ADD COLUMN IF NOT EXISTS "firstName" TEXT,
  ADD COLUMN IF NOT EXISTS "lastName" TEXT,
  ADD COLUMN IF NOT EXISTS "photoUrl" TEXT,
  ADD COLUMN IF NOT EXISTS "gender" TEXT,
  ADD COLUMN IF NOT EXISTS "address" TEXT,
  ADD COLUMN IF NOT EXISTS "medium" TEXT,
  ADD COLUMN IF NOT EXISTS "interestedIn" TEXT,
  ADD COLUMN IF NOT EXISTS "enquiryDate" DATE;

CREATE INDEX IF NOT EXISTS "Enquiry_gymId_enquiryDate_idx"
  ON "Enquiry"("gymId", "enquiryDate");
