-- V1: Tao bang users cho user-service
CREATE TABLE IF NOT EXISTS users (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email     VARCHAR(100) UNIQUE,
    role      VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER'
);

-- Index tang toc tim kiem
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);