-- Notification domain types and deep-link metadata

ALTER TYPE "NotificationType" ADD VALUE 'MEMBER_ADDED';
ALTER TYPE "NotificationType" ADD VALUE 'PAYMENT_RECEIVED';
ALTER TYPE "NotificationType" ADD VALUE 'PLAN_ASSIGNED';
ALTER TYPE "NotificationType" ADD VALUE 'EXPIRY_ALERT';

ALTER TYPE "NotificationEntityType" ADD VALUE 'MEMBER_SUBSCRIPTION';

ALTER TABLE "Notification" ADD COLUMN "metadata" JSONB;
ALTER TABLE "Notification" ADD COLUMN "dedupeKey" TEXT;

CREATE UNIQUE INDEX "Notification_dedupeKey_key" ON "Notification"("dedupeKey");
