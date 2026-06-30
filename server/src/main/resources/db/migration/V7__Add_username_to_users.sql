-- Add username column with a default value to avoid breaking existing rows
ALTER TABLE users ADD COLUMN username VARCHAR(255) DEFAULT 'user_' || (random() * 1000000)::INT;

-- Remove default constraint after populating existing rows
ALTER TABLE users ALTER COLUMN username DROP DEFAULT;

-- Add UNIQUE constraint to username
ALTER TABLE users ADD CONSTRAINT users_username_unique UNIQUE (username);
