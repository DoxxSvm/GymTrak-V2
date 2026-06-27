-- CreateTable
CREATE TABLE "subscription_history_events" (
    "id" TEXT NOT NULL,
    "subscription_id" TEXT NOT NULL,
    "gym_id" TEXT NOT NULL,
    "event_type" TEXT NOT NULL,
    "actor_user_id" TEXT,
    "payload_json" JSONB NOT NULL DEFAULT '{}',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "subscription_history_events_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "subscription_history_events_subscription_id_idx" ON "subscription_history_events"("subscription_id");

-- CreateIndex
CREATE INDEX "subscription_history_events_gym_id_idx" ON "subscription_history_events"("gym_id");

-- CreateIndex
CREATE INDEX "subscription_history_events_created_at_idx" ON "subscription_history_events"("created_at");

-- AddForeignKey
ALTER TABLE "subscription_history_events" ADD CONSTRAINT "subscription_history_events_subscription_id_fkey" FOREIGN KEY ("subscription_id") REFERENCES "MemberSubscription"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "subscription_history_events" ADD CONSTRAINT "subscription_history_events_gym_id_fkey" FOREIGN KEY ("gym_id") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "subscription_history_events" ADD CONSTRAINT "subscription_history_events_actor_user_id_fkey" FOREIGN KEY ("actor_user_id") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
