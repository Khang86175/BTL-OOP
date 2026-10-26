-- Run once on an existing MySQL movie_booking database.
-- The application keeps booking.status unchanged for backward compatibility.
USE movie_booking;
ALTER TABLE booking MODIFY COLUMN status VARCHAR(30) NOT NULL;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NULL;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS hold_expires_at DATETIME(6) NULL;
UPDATE booking SET payment_status = 'SUCCESS' WHERE payment_status IS NULL OR payment_status = 'PAID';

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

UPDATE payments SET status = 'SUCCESS' WHERE status = 'PAID';

-- Older versions used ticket_code. Rename it before creating/using the current
-- JPA column name (code). The conditional keeps this script safe for fresh DBs
-- and databases that already have the current column.
SET @rename_ticket_code_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tickets'
              AND COLUMN_NAME = 'ticket_code'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tickets'
              AND COLUMN_NAME = 'code'
        ),
        'ALTER TABLE tickets CHANGE COLUMN ticket_code code VARCHAR(40) NOT NULL',
        'SELECT 1'
    )
);
PREPARE rename_ticket_code_stmt FROM @rename_ticket_code_sql;
EXECUTE rename_ticket_code_stmt;
DEALLOCATE PREPARE rename_ticket_code_stmt;

-- Some databases contain both columns after an earlier partial migration.
-- Keep the current JPA column and remove the obsolete required column.
SET @copy_legacy_ticket_code_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tickets'
              AND COLUMN_NAME = 'ticket_code'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tickets'
              AND COLUMN_NAME = 'code'
        ),
        'UPDATE tickets SET code = COALESCE(code, ticket_code)',
        'SELECT 1'
    )
);
PREPARE copy_legacy_ticket_code_stmt FROM @copy_legacy_ticket_code_sql;
EXECUTE copy_legacy_ticket_code_stmt;
DEALLOCATE PREPARE copy_legacy_ticket_code_stmt;

SET @drop_legacy_ticket_code_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tickets'
              AND COLUMN_NAME = 'ticket_code'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tickets'
              AND COLUMN_NAME = 'code'
        ),
        'ALTER TABLE tickets DROP COLUMN ticket_code',
        'SELECT 1'
    )
);
PREPARE drop_legacy_ticket_code_stmt FROM @drop_legacy_ticket_code_sql;
EXECUTE drop_legacy_ticket_code_stmt;
DEALLOCATE PREPARE drop_legacy_ticket_code_stmt;

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
