-- Payment schema migration for CineTicket / movie_booking.
-- MySQL 8.0+. Safe to run again: it preserves existing payment history.
USE movie_booking;
SET NAMES utf8mb4;

-- Booking payment state and temporary seat-hold deadline.
ALTER TABLE booking MODIFY COLUMN status VARCHAR(30) NOT NULL;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NULL;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS hold_expires_at DATETIME(6) NULL;

UPDATE booking
SET payment_status = CASE
    WHEN status = 'CONFIRMED' OR payment_status = 'PAID' THEN 'SUCCESS'
    WHEN status IN ('EXPIRED', 'CANCELLED') THEN 'FAILED'
    WHEN payment_status IS NULL THEN 'PENDING'
    ELSE payment_status
END
WHERE payment_status IS NULL
   OR payment_status = 'PAID'
   OR status IN ('CONFIRMED', 'EXPIRED', 'CANCELLED');

-- One row per payment attempt. A booking may have multiple attempts.
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    method VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_ref VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    paid_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    INDEX idx_payments_booking_created (booking_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Complete an older/incomplete payments table without deleting its history.
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS method VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS amount DECIMAL(12,2) NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS transaction_ref VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS created_at DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS paid_at DATETIME(6) NULL;

UPDATE payments SET status = 'SUCCESS' WHERE status = 'PAID';
UPDATE payments SET method = 'QR' WHERE method IS NULL OR method = '';
UPDATE payments p
LEFT JOIN booking b ON b.id = p.booking_id
SET p.amount = COALESCE(p.amount, b.total_amount, 0.00)
WHERE p.amount IS NULL;
UPDATE payments SET status = 'PENDING' WHERE status IS NULL OR status = '';
UPDATE payments SET created_at = CURRENT_TIMESTAMP(6) WHERE created_at IS NULL;

ALTER TABLE payments
    MODIFY COLUMN method VARCHAR(30) NOT NULL,
    MODIFY COLUMN amount DECIMAL(12,2) NOT NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

-- Tickets: the application uses tickets.code, not tickets.ticket_code.
SET @rename_ticket_code_sql = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tickets'
                  AND COLUMN_NAME = 'ticket_code')
        AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tickets'
                          AND COLUMN_NAME = 'code'),
        'ALTER TABLE tickets CHANGE COLUMN ticket_code code VARCHAR(40) NOT NULL',
        'SELECT 1'
    )
);
PREPARE rename_ticket_code_stmt FROM @rename_ticket_code_sql;
EXECUTE rename_ticket_code_stmt;
DEALLOCATE PREPARE rename_ticket_code_stmt;

-- Handle databases where an earlier partial migration created both columns.
SET @copy_ticket_code_sql = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tickets'
                  AND COLUMN_NAME = 'ticket_code')
        AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tickets'
                      AND COLUMN_NAME = 'code'),
        'UPDATE tickets SET code = COALESCE(code, ticket_code)',
        'SELECT 1'
    )
);
PREPARE copy_ticket_code_stmt FROM @copy_ticket_code_sql;
EXECUTE copy_ticket_code_stmt;
DEALLOCATE PREPARE copy_ticket_code_stmt;

SET @drop_ticket_code_sql = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tickets'
                  AND COLUMN_NAME = 'ticket_code')
        AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tickets'
                      AND COLUMN_NAME = 'code'),
        'ALTER TABLE tickets DROP COLUMN ticket_code',
        'SELECT 1'
    )
);
PREPARE drop_ticket_code_stmt FROM @drop_ticket_code_sql;
EXECUTE drop_ticket_code_stmt;
DEALLOCATE PREPARE drop_ticket_code_stmt;

CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    booking_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    showtime_start DATETIME(6) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tickets_code (code),
    CONSTRAINT fk_tickets_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_tickets_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id),
    CONSTRAINT fk_tickets_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT fk_tickets_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    INDEX idx_tickets_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verify the final columns expected by the JPA entities.
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('booking', 'payments', 'tickets')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
