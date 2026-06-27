-- Migrate legacy EXPIRY_REMINDER rows (runs after enum values are committed)
UPDATE "GymMessageTemplate"
SET kind = 'EXPIRY_REMINDER_7D'
WHERE kind = 'EXPIRY_REMINDER';
