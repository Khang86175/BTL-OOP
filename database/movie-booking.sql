-- Schema hợp nhất CineTicket: users + movie catalog.
-- Dành cho MySQL 8 và database movie_booking.
-- An toàn khi chạy lại: không DROP bảng và không chứa dữ liệu tài khoản cá nhân.
-- Nếu database đã có bảng users từ da1.sql, các cột cũ (enable/create_at) sẽ được giữ nguyên,
-- còn ứng dụng dùng enabled/created_at như entity User.

CREATE DATABASE IF NOT EXISTS movie_booking
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE movie_booking;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(255) DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
    age_rating ENUM('K', 'P', 'T13', 'T16', 'T18') NOT NULL,
    synopsis VARCHAR(10000) NOT NULL,
    poster_url VARCHAR(2048) DEFAULT NULL,
    trailer_url VARCHAR(2048) DEFAULT NULL,
    visible BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_movie_duration CHECK (duration_minutes BETWEEN 1 AND 600)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS movie_category (
    movie_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, category_id),
    CONSTRAINT fk_movie_category_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT fk_movie_category_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
