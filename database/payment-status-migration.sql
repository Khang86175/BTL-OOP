-- Run once on an existing MySQL movie_booking database.
-- The application keeps booking.status unchanged for backward compatibility.
USE movie_booking;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NULL;
UPDATE booking SET payment_status = 'PAID' WHERE payment_status IS NULL;
