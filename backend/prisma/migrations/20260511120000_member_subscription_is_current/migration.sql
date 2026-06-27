-- Per-period flag: at most one row per gym member should be true (latest endsAt in the active window).
ALTER TABLE "MemberSubscription" ADD COLUMN "isCurrentSubscription" BOOLEAN NOT NULL DEFAULT false;
