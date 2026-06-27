-- DropIndex
DROP INDEX "AttendanceRecord_gymId_memberUserId_attendedOn_key";

-- DropIndex
DROP INDEX "TrainerAttendanceRecord_gymId_trainerUserId_attendedOn_key";

-- CreateIndex
CREATE INDEX "AttendanceRecord_gymId_memberUserId_attendedOn_idx" ON "AttendanceRecord"("gymId", "memberUserId", "attendedOn");

-- CreateIndex
CREATE INDEX "TrainerAttendanceRecord_gymId_trainerUserId_attendedOn_idx" ON "TrainerAttendanceRecord"("gymId", "trainerUserId", "attendedOn");
