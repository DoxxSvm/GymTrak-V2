-- Analytics: time-range scans on payments, member joins
CREATE INDEX IF NOT EXISTS "GymUser_gymId_joinedAt_idx" ON "GymUser"("gymId", "joinedAt");
CREATE INDEX IF NOT EXISTS "Payment_gymId_status_completedAt_idx" ON "Payment"("gymId", "status", "completedAt");
