-- CreateEnum
CREATE TYPE "PaymentMethod" AS ENUM ('CASH', 'UPI');

-- CreateEnum
CREATE TYPE "InvoiceStatus" AS ENUM ('DRAFT', 'ISSUED', 'VOID');

-- AlterEnum
ALTER TYPE "MemberSubscriptionStatus" ADD VALUE 'FROZEN';

-- AlterTable
ALTER TABLE "MemberSubscription" ADD COLUMN "priceCents" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "MemberSubscription" ADD COLUMN "paidCents" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "MemberSubscription" ADD COLUMN "currency" TEXT NOT NULL DEFAULT 'USD';
ALTER TABLE "MemberSubscription" ADD COLUMN "freezeStartedAt" TIMESTAMP(3);
ALTER TABLE "MemberSubscription" ADD COLUMN "freezeEndsAt" TIMESTAMP(3);

-- Backfill price/currency from catalog
UPDATE "MemberSubscription" AS ms
SET
  "priceCents" = sp."priceCents",
  "currency" = sp."currency"
FROM "SubscriptionPlan" AS sp
WHERE ms."planId" = sp."id";

-- Backfill paidCents from completed payments
UPDATE "MemberSubscription" AS ms
SET "paidCents" = COALESCE(pay."s", 0)
FROM (
  SELECT "memberSubscriptionId", SUM("amountCents")::INTEGER AS s
  FROM "Payment"
  WHERE "memberSubscriptionId" IS NOT NULL AND "status" = 'COMPLETED'
  GROUP BY "memberSubscriptionId"
) AS pay
WHERE ms."id" = pay."memberSubscriptionId";

-- AlterTable
ALTER TABLE "Payment" ADD COLUMN "method" "PaymentMethod";
ALTER TABLE "Payment" ADD COLUMN "invoiceId" TEXT;

-- CreateTable
CREATE TABLE "GymInvoiceSequence" (
    "gymId" TEXT NOT NULL,
    "year" INTEGER NOT NULL,
    "lastNumber" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "GymInvoiceSequence_pkey" PRIMARY KEY ("gymId","year")
);

-- CreateTable
CREATE TABLE "Invoice" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "gymUserId" TEXT NOT NULL,
    "memberSubscriptionId" TEXT NOT NULL,
    "invoiceYear" INTEGER NOT NULL,
    "invoiceNumber" INTEGER NOT NULL,
    "subtotalCents" INTEGER NOT NULL,
    "totalCents" INTEGER NOT NULL,
    "currency" TEXT NOT NULL,
    "status" "InvoiceStatus" NOT NULL DEFAULT 'ISSUED',
    "issuedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "lineSummary" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Invoice_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Invoice_gymId_invoiceYear_invoiceNumber_key" ON "Invoice"("gymId", "invoiceYear", "invoiceNumber");

-- CreateIndex
CREATE INDEX "Invoice_gymId_issuedAt_idx" ON "Invoice"("gymId", "issuedAt");

-- CreateIndex
CREATE INDEX "Invoice_memberSubscriptionId_idx" ON "Invoice"("memberSubscriptionId");

-- CreateIndex
CREATE INDEX "Payment_invoiceId_idx" ON "Payment"("invoiceId");

-- AddForeignKey
ALTER TABLE "GymInvoiceSequence" ADD CONSTRAINT "GymInvoiceSequence_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Invoice" ADD CONSTRAINT "Invoice_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Invoice" ADD CONSTRAINT "Invoice_gymUserId_fkey" FOREIGN KEY ("gymUserId") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Invoice" ADD CONSTRAINT "Invoice_memberSubscriptionId_fkey" FOREIGN KEY ("memberSubscriptionId") REFERENCES "MemberSubscription"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Payment" ADD CONSTRAINT "Payment_invoiceId_fkey" FOREIGN KEY ("invoiceId") REFERENCES "Invoice"("id") ON DELETE SET NULL ON UPDATE CASCADE;
