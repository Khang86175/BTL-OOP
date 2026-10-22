-- Schema module Người 2, dùng cho MySQL 8 trong database movie_booking.
-- Chạy thủ công nếu muốn tạo bảng trước. Hibernate ddl-auto=update cũng tự tạo các bảng này.
-- Script không xóa/sửa bảng users và không tự chạy khi khởi động.
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS movies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    director VARCHAR(150) NOT NULL,
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    age_rating VARCHAR(10) NOT NULL,
    synopsis VARCHAR(10000) NOT NULL,
    poster_url VARCHAR(2048) DEFAULT NULL,
    trailer_url VARCHAR(2048) DEFAULT NULL,
    visible BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_movie_duration CHECK (duration_minutes BETWEEN 1 AND 600),
    CONSTRAINT chk_movie_age_rating CHECK (age_rating IN ('P', 'K', 'T13', 'T16', 'T18'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS movie_category (
    movie_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, category_id),
    CONSTRAINT fk_movie_category_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT fk_movie_category_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
