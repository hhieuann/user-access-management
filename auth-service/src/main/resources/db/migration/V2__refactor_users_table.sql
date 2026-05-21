-- =========================================================
-- V2: Refactor users table - SAFE MIGRATION (preserves user data)
-- =========================================================
-- Truoc khi chay tren PROD: BACKUP database day du.
-- Migration nay khong drop bang users.
-- Cac buoc:
--   1) Them cot 'role' (VARCHAR) vao bang users
--   2) Backfill cot 'role' tu bang user_roles + roles (lay role dau tien moi user)
--   3) Drop bang trung gian user_roles, drop bang roles (data da migrate)
--   4) Drop cot 'enabled' khong con dung
--   5) Chuyen kieu users.id tu UUID sang BIGINT (preserve ban ghi)
--   6) Tao index
-- =========================================================

-- Buoc 1: Them cot role neu chua co
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);

-- Buoc 2: Backfill role tu bang M2M cu (chi chay neu user_roles va roles ton tai)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'user_roles'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'roles'
    ) THEN
        UPDATE users u
        SET role = sub.role_name
        FROM (
            SELECT DISTINCT ON (ur.user_id) ur.user_id, r.name AS role_name
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            ORDER BY ur.user_id, r.id
        ) sub
        WHERE sub.user_id::text = u.id::text
          AND u.role IS NULL;
    END IF;
END $$;

-- Default cho user khong co role (vd: tao tu code moi sau khi backfill)
UPDATE users SET role = 'ROLE_USER' WHERE role IS NULL;

-- Set NOT NULL + DEFAULT
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'ROLE_USER';

-- Buoc 3: Drop M2M tables (data da duoc backfill vao users.role)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;

-- Buoc 4: Drop cot 'enabled' khong con dung (entity moi khong co)
ALTER TABLE users DROP COLUMN IF EXISTS enabled;

-- Buoc 5: Chuyen kieu users.id tu UUID sang BIGINT (preserve ban ghi)
-- Lam an toan: them cot moi BIGSERIAL, drop PK cu, drop cot cu, rename, set PK moi.
DO $$
DECLARE
    id_type TEXT;
BEGIN
    SELECT data_type INTO id_type
    FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'id';

    IF id_type = 'uuid' THEN
        -- Them cot id_new BIGSERIAL
        ALTER TABLE users ADD COLUMN id_new BIGSERIAL;
        -- Drop primary key cu tren cot id (UUID)
        ALTER TABLE users DROP CONSTRAINT IF EXISTS users_pkey;
        -- Drop cot UUID cu
        ALTER TABLE users DROP COLUMN id;
        -- Rename id_new -> id
        ALTER TABLE users RENAME COLUMN id_new TO id;
        -- Set primary key moi
        ALTER TABLE users ADD PRIMARY KEY (id);
    END IF;
END $$;

-- Buoc 6: Index toc do tim kiem theo username
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
