-- Optional: owner signup flow marks phoneVerified after OTP + completion
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "phoneVerified" BOOLEAN NOT NULL DEFAULT false;
