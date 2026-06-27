-- CreateEnum
CREATE TYPE "AttendanceSource" AS ENUM ('QR_TOKEN', 'BIOMETRIC', 'MANUAL');

-- AlterTable
ALTER TABLE "AttendanceRecord" ADD COLUMN     "source" "AttendanceSource" NOT NULL DEFAULT 'QR_TOKEN',
ADD COLUMN     "createdAt" TIMESTAMP(3),
ADD COLUMN     "updatedAt" TIMESTAMP(3);

UPDATE "AttendanceRecord" SET "createdAt" = "checkedInAt", "updatedAt" = "checkedInAt" WHERE "createdAt" IS NULL;

ALTER TABLE "AttendanceRecord" ALTER COLUMN "createdAt" SET NOT NULL;
ALTER TABLE "AttendanceRecord" ALTER COLUMN "updatedAt" SET NOT NULL;
ALTER TABLE "AttendanceRecord" ALTER COLUMN "createdAt" SET DEFAULT CURRENT_TIMESTAMP;

-- CreateIndex
CREATE INDEX "AttendanceRecord_memberUserId_gymId_idx" ON "AttendanceRecord"("memberUserId", "gymId");

-- AlterTable
ALTER TABLE "GymUser" ADD COLUMN     "attendanceBlocked" BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN     "attendanceBlockedReason" TEXT,
ADD COLUMN     "attendanceBlockedAt" TIMESTAMP(3);

-- CreateTable
CREATE TABLE "MemberBiometricCredential" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "memberUserId" TEXT NOT NULL,
    "deviceId" TEXT NOT NULL,
    "secretHash" TEXT NOT NULL,
    "label" TEXT,
    "revokedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "MemberBiometricCredential_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "MemberBiometricCredential_deviceId_key" ON "MemberBiometricCredential"("deviceId");

CREATE INDEX "MemberBiometricCredential_gymId_memberUserId_idx" ON "MemberBiometricCredential"("gymId", "memberUserId");

ALTER TABLE "MemberBiometricCredential" ADD CONSTRAINT "MemberBiometricCredential_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "MemberBiometricCredential" ADD CONSTRAINT "MemberBiometricCredential_memberUserId_fkey" FOREIGN KEY ("memberUserId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
