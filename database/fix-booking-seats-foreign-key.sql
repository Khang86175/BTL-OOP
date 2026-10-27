-- Fixes an existing schema where booking_seats.booking_id points to `bookings`
-- while the application stores Booking entities in `booking`.
USE movie_booking;

ALTER TABLE booking_seats
    MODIFY COLUMN showtime_id BIGINT NULL;

ALTER TABLE booking_seats
    DROP FOREIGN KEY FKmbi9ciapn0nvat63t0a8tv478;

ALTER TABLE booking_seats
    ADD CONSTRAINT fk_booking_seats_booking
    FOREIGN KEY (booking_id) REFERENCES booking(id);
