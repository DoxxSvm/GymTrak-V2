ALTER TABLE "TrainerAttendanceRecord"
ADD COLUMN IF NOT EXISTS "checkedOutAt" TIMESTAMP(3);
