-- V2: Refactor users table to match new entity (Long id, role column)
-- Drop bảng cũ (many-to-many không còn dùng)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;

-- Drop bảng users cũ và tạo lại với schema mới
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER'
);

-- Index tang toc tim kiem
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);