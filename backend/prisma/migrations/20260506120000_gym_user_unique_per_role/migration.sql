-- Allow same user at same gym with different roles (owner self-member persona).
DROP INDEX IF EXISTS "GymUser_userId_gymId_key";

CREATE UNIQUE INDEX "GymUser_userId_gymId_role_key" ON "GymUser"("userId", "gymId", "role");
