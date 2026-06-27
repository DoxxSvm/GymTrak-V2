-- CreateTable
CREATE TABLE "broadcast_channels" (
    "id" TEXT NOT NULL,
    "gymId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT,
    "image_url" TEXT,
    "created_by_user_id" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "broadcast_channels_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "broadcast_channel_members" (
    "id" TEXT NOT NULL,
    "channel_id" TEXT NOT NULL,
    "gym_user_id" TEXT NOT NULL,
    "joined_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "broadcast_channel_members_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "broadcast_messages" (
    "id" TEXT NOT NULL,
    "channel_id" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "description" TEXT,
    "image_url" TEXT,
    "created_by_user_id" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "broadcast_messages_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "broadcast_channels_gymId_created_at_idx" ON "broadcast_channels"("gymId", "created_at" DESC);

-- CreateIndex
CREATE INDEX "broadcast_channels_gymId_name_idx" ON "broadcast_channels"("gymId", "name");

-- CreateIndex
CREATE INDEX "broadcast_channel_members_gym_user_id_idx" ON "broadcast_channel_members"("gym_user_id");

-- CreateIndex
CREATE UNIQUE INDEX "broadcast_channel_members_channel_id_gym_user_id_key" ON "broadcast_channel_members"("channel_id", "gym_user_id");

-- CreateIndex
CREATE INDEX "broadcast_messages_channel_id_created_at_idx" ON "broadcast_messages"("channel_id", "created_at" DESC);

-- AddForeignKey
ALTER TABLE "broadcast_channels" ADD CONSTRAINT "broadcast_channels_gymId_fkey" FOREIGN KEY ("gymId") REFERENCES "Gym"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "broadcast_channels" ADD CONSTRAINT "broadcast_channels_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "broadcast_channel_members" ADD CONSTRAINT "broadcast_channel_members_channel_id_fkey" FOREIGN KEY ("channel_id") REFERENCES "broadcast_channels"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "broadcast_channel_members" ADD CONSTRAINT "broadcast_channel_members_gym_user_id_fkey" FOREIGN KEY ("gym_user_id") REFERENCES "GymUser"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "broadcast_messages" ADD CONSTRAINT "broadcast_messages_channel_id_fkey" FOREIGN KEY ("channel_id") REFERENCES "broadcast_channels"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "broadcast_messages" ADD CONSTRAINT "broadcast_messages_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
