-- Admin / super-admin list & analytics query support
CREATE INDEX IF NOT EXISTS "User_globalRole_idx" ON "User"("globalRole");
CREATE INDEX IF NOT EXISTS "User_createdAt_idx" ON "User"("createdAt");
CREATE INDEX IF NOT EXISTS "Gym_ownerId_idx" ON "Gym"("ownerId");
CREATE INDEX IF NOT EXISTS "Gym_createdAt_idx" ON "Gym"("createdAt");
CREATE INDEX IF NOT EXISTS "GymUser_role_idx" ON "GymUser"("role");
CREATE INDEX IF NOT EXISTS "GymUser_joinedAt_idx" ON "GymUser"("joinedAt");
