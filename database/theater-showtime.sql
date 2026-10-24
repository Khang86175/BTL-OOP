-- CineTicket member 3. Run manually on MySQL 8; no seed or changes to catalog/users.
-- Existing tables are not migrated by IF NOT EXISTS: review existing definitions before use.
USE movie_booking;
CREATE TABLE IF NOT EXISTS theaters (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(150) NOT NULL, address VARCHAR(500) NOT NULL, active BIT(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS rooms (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, theater_id BIGINT NOT NULL,
 name VARCHAR(80) NOT NULL, room_type VARCHAR(20) NOT NULL, active BIT(1) NOT NULL,
 CONSTRAINT uk_rooms_theater_name UNIQUE(theater_id,name),
 CONSTRAINT fk_rooms_theater FOREIGN KEY(theater_id) REFERENCES theaters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS seats (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, room_id BIGINT NOT NULL,
 row_label VARCHAR(10) NOT NULL, seat_number INT NOT NULL,
 seat_type VARCHAR(20) NOT NULL, active BIT(1) NOT NULL,
 CONSTRAINT uk_seats_position UNIQUE(room_id,row_label,seat_number),
 CONSTRAINT fk_seats_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT chk_seats_number CHECK(seat_number > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS showtimes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 movie_id BIGINT NOT NULL, room_id BIGINT NOT NULL,
 start_time DATETIME(6) NOT NULL, end_time DATETIME(6) NOT NULL,
 base_price DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL,
 CONSTRAINT fk_showtimes_movie FOREIGN KEY(movie_id) REFERENCES movies(id),
 CONSTRAINT fk_showtimes_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT chk_showtimes_values CHECK(base_price >= 0 AND end_time > start_time),
 INDEX idx_showtimes_room_time(room_id,start_time,end_time),
 INDEX idx_showtimes_movie_time(movie_id,start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
