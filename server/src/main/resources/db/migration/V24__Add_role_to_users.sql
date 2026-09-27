-- Migration V24: Add role column to users table with index
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
