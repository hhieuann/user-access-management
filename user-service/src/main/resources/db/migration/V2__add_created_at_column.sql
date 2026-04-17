-- V2: Them cot created_at de track thoi gian tao user
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Cap nhat gia tri cho cac row cu
UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;