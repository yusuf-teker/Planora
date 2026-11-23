CREATE TABLE IF NOT EXISTS health_huawei_tokens (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at BIGINT NOT NULL,
    scope TEXT,
    api_base_url VARCHAR(255) NOT NULL DEFAULT 'https://health-api.cloud.huawei.eu',
    last_synced_at BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_health_huawei_tokens_user_id ON health_huawei_tokens(user_id);
