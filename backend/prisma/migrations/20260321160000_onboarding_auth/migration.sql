-- CreateEnum
CREATE TYPE "AppOnboardingRole" AS ENUM ('OWNER', 'MEMBER');

-- AlterTable
ALTER TABLE "User" ADD COLUMN "selectedOnboardingRole" "AppOnboardingRole",
ADD COLUMN "onboardingCompletedAt" TIMESTAMP(3),
ADD COLUMN "fullName" TEXT,
ADD COLUMN "heightCm" DECIMAL(5,2),
ADD COLUMN "weightKg" DECIMAL(5,2);
