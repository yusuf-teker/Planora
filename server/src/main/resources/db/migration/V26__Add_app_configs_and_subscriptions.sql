-- Migration V26: Add app_configs (per-app force update configuration) and subscriptions (billing & receipt verification) tables

CREATE TABLE IF NOT EXISTS app_configs (
    app_id VARCHAR(50) PRIMARY KEY,
    min_supported_version VARCHAR(30) NOT NULL DEFAULT '1.0.0',
    latest_version VARCHAR(30) NOT NULL DEFAULT '1.0.0',
    force_update_title_tr VARCHAR(200) NOT NULL DEFAULT 'Zorunlu Güncelleme',
    force_update_title_en VARCHAR(200) NOT NULL DEFAULT 'Update Required',
    force_update_message_tr TEXT NOT NULL,
    force_update_message_en TEXT NOT NULL,
    store_url_android VARCHAR(500) NOT NULL DEFAULT '',
    store_url_ios VARCHAR(500) NOT NULL DEFAULT '',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_configs (
    app_id, min_supported_version, latest_version,
    force_update_title_tr, force_update_title_en,
    force_update_message_tr, force_update_message_en,
    store_url_android, store_url_ios
) VALUES
(
    'planora', '1.0.0', '1.0.0',
    'Zorunlu Güncelleme', 'Update Required',
    'Planora''yı kullanmaya devam edebilmek için lütfen uygulamayı son sürüme güncelleyin.',
    'Please update Planora to the latest version to continue using the application.',
    '', ''
),
(
    'healthtrack', '1.0.0', '1.0.0',
    'Zorunlu Güncelleme', 'Update Required',
    'HealthTrack''i kullanmaya devam edebilmek için lütfen uygulamayı son sürüme güncelleyin.',
    'Please update HealthTrack to the latest version to continue using the application.',
    '', ''
)
ON CONFLICT (app_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS subscriptions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    app_id VARCHAR(50) NOT NULL DEFAULT 'ecosystem',
    plan_id VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    purchase_token TEXT,
    transaction_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    raw_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_transaction_id ON subscriptions (transaction_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions (status);

