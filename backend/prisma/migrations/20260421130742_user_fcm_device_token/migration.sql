-- AlterTable
ALTER TABLE "User" ADD COLUMN     "fcmDeviceToken" TEXT,
ADD COLUMN     "fcmTokenUpdatedAt" TIMESTAMP(3);
