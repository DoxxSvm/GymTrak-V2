-- Subscription freeze/extend/renew history (audit-friendly metadata)
CREATE TABLE IF NOT EXISTS "subscription_history_events" (
  "id" TEXT PRIMARY KEY,
  "subscription_id" TEXT NOT NULL REFERENCES "MemberSubscription"("id") ON DELETE CASCADE,
  "gym_id" TEXT NOT NULL REFERENCES "Gym"("id") ON DELETE CASCADE,
  "event_type" TEXT NOT NULL,
  "actor_user_id" TEXT REFERENCES "User"("id") ON DELETE SET NULL,
  "payload_json" JSONB NOT NULL DEFAULT '{}'::jsonb,
  "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "subscription_history_events_subscription_id_idx"
  ON "subscription_history_events" ("subscription_id");
CREATE INDEX IF NOT EXISTS "subscription_history_events_gym_id_idx"
  ON "subscription_history_events" ("gym_id");
CREATE INDEX IF NOT EXISTS "subscription_history_events_created_at_idx"
  ON "subscription_history_events" ("created_at");
