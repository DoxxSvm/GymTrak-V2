-- CreateEnum
CREATE TYPE "LastActiveRole" AS ENUM ('OWNER', 'MEMBER');

-- AlterTable
ALTER TABLE "User" ADD COLUMN     "lastActiveRole" "LastActiveRole" NOT NULL DEFAULT 'OWNER';

-- CreateTable
CREATE TABLE "owner_profiles" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "owner_profiles_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "member_profiles" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "ageYears" INTEGER NOT NULL,
    "gender" VARCHAR(16) NOT NULL,
    "heightCm" DECIMAL(5,2) NOT NULL,
    "weightKg" DECIMAL(5,2) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "member_profiles_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "owner_profiles_userId_key" ON "owner_profiles"("userId");

-- CreateIndex
CREATE UNIQUE INDEX "member_profiles_userId_key" ON "member_profiles"("userId");

-- AddForeignKey
ALTER TABLE "owner_profiles" ADD CONSTRAINT "owner_profiles_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "member_profiles" ADD CONSTRAINT "member_profiles_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Backfill owner_profiles for users who already own a gym (pre-feature rows).
INSERT INTO "owner_profiles" ("id", "userId", "createdAt")
SELECT md5(random()::text || clock_timestamp()::text)::text, u.id, CURRENT_TIMESTAMP
FROM "User" u
WHERE EXISTS (SELECT 1 FROM "Gym" g WHERE g."ownerId" = u.id)
  AND NOT EXISTS (
    SELECT 1 FROM "owner_profiles" o WHERE o."userId" = u.id
  );
