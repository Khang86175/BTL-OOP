-- Fix the booking_seats uniqueness rule for expired/retried bookings.
-- booking_seats is historical data, so the same showtime/seat may occur in
-- multiple bookings over time. Uniqueness belongs to one booking only.
USE movie_booking;

ALTER TABLE booking_seats
    DROP INDEX uk_booking_showtime_seat;

ALTER TABLE booking_seats
    ADD CONSTRAINT uk_booking_booking_showtime_seat
    UNIQUE (booking_id, showtime_id, seat_id);
