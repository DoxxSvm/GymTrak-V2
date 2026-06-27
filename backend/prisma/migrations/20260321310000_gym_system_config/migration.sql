-- Per-gym system configuration (currency, GST, default plan presets)

CREATE TABLE "GymSystemConfig" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "currency" TEXT NOT NULL DEFAULT 'USD',
    "gstEnabled" BOOLEAN NOT NULL DEFAULT false,
    "gstRatePercent" DECIMAL(5,2),
    "gstInclusive" BOOLEAN NOT NULL DEFAULT false,
    "gstStateCode" TEXT,
    "defaultPlanConfig" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "GymSystemConfig_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "GymSystemConfig_gymId_key" ON "GymSystemConfig"("gymId");

ALTER TABLE "GymSystemConfig" ADD CONSTRAINT "GymSystemConfig_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;
