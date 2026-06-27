-- Fast ILIKE / substring search (pg_trgm). Safe to run once per database.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS "User_fullName_trgm_idx" ON "User" USING gin ("fullName" gin_trgm_ops);
CREATE INDEX IF NOT EXISTS "User_phone_trgm_idx" ON "User" USING gin (phone gin_trgm_ops);
CREATE INDEX IF NOT EXISTS "GymPlan_name_trgm_idx" ON "GymPlan" USING gin (name gin_trgm_ops);
