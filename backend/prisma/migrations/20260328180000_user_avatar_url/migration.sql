-- Optional profile image for dashboard owner display and future mobile avatars
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "avatarUrl" TEXT;
