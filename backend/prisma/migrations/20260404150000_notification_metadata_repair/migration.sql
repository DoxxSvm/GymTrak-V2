-- Idempotent repair: some databases never got these columns (e.g. failed migration
-- before ALTER TABLE, or drift vs 20260321200100_notification_system).
ALTER TABLE "Notification" ADD COLUMN IF NOT EXISTS "metadata" JSONB;
ALTER TABLE "Notification" ADD COLUMN IF NOT EXISTS "dedupeKey" TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS "Notification_dedupeKey_key" ON "Notification"("dedupeKey");
