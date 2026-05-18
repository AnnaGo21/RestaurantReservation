-- Add grace period configuration to restaurants
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS grace_period_minutes INTEGER DEFAULT 15;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS default_reservation_minutes INTEGER DEFAULT 90;

-- Update existing restaurants to have default values
UPDATE restaurants SET grace_period_minutes = 15 WHERE grace_period_minutes IS NULL;
UPDATE restaurants SET default_reservation_minutes = 90 WHERE default_reservation_minutes IS NULL;
