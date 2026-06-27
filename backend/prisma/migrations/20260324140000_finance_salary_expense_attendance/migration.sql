-- PaymentMethod: CARD
ALTER TYPE "PaymentMethod" ADD VALUE 'CARD';

-- Audit enums
ALTER TYPE "AuditAction" ADD VALUE 'EXPENSE_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'EXPENSE_DELETED';
ALTER TYPE "AuditAction" ADD VALUE 'TRAINER_SALARY_PAID';
ALTER TYPE "AuditEntityType" ADD VALUE 'EXPENSE';

-- Member attendance: checkout timestamp (biometric / kiosk style)
ALTER TABLE "AttendanceRecord" ADD COLUMN IF NOT EXISTS "checkedOutAt" TIMESTAMP(3);

-- Trainer salary payments: method
ALTER TABLE "TrainerSalaryPayment" ADD COLUMN IF NOT EXISTS "method" "PaymentMethod";

-- Expenses: payment method + optional trainer attribution
ALTER TABLE "Expense" ADD COLUMN IF NOT EXISTS "method" "PaymentMethod";
ALTER TABLE "Expense" ADD COLUMN IF NOT EXISTS "trainerGymUserId" TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'Expense_trainerGymUserId_fkey'
  ) THEN
    ALTER TABLE "Expense"
      ADD CONSTRAINT "Expense_trainerGymUserId_fkey"
      FOREIGN KEY ("trainerGymUserId") REFERENCES "GymUser"("id") ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS "Expense_trainerGymUserId_idx" ON "Expense"("trainerGymUserId");
