-- Add catalog owner and allow personal (no-gym) exercises, mirroring `diet_food` scoping.
ALTER TABLE "exercises" ADD COLUMN "userId" TEXT;

UPDATE "exercises" AS e
SET "userId" = g."ownerId"
FROM "Gym" AS g
WHERE e."gymId" = g."id";

-- Fail if any row could not be backfilled (orphan data).
ALTER TABLE "exercises" ALTER COLUMN "userId" SET NOT NULL;

ALTER TABLE "exercises" ADD CONSTRAINT "exercises_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "exercises" ALTER COLUMN "gymId" DROP NOT NULL;

CREATE INDEX "exercises_userId_idx" ON "exercises"("userId");
CREATE INDEX "exercises_gymId_userId_idx" ON "exercises"("gymId", "userId");
