-- Migration V22: Add premium user columns and ai_usage_logs table for token quotas

ALTER TABLE users ADD COLUMN is_premium BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN premium_until TIMESTAMP;

CREATE TABLE ai_usage_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL DEFAULT 'AI_CHAT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_usage_user_date ON ai_usage_logs (user_id, created_at);
