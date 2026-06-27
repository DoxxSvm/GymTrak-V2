-- Actor who recorded the payment (JWT `sub`); legacy rows backfilled to gym owner.
ALTER TABLE "Payment" ADD COLUMN "userId" TEXT;

UPDATE "Payment" AS p
SET "userId" = g."ownerId"
FROM "Gym" AS g
WHERE p."gymId" = g."id";

ALTER TABLE "Payment" ALTER COLUMN "userId" SET NOT NULL;

ALTER TABLE "Payment" ADD CONSTRAINT "Payment_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
