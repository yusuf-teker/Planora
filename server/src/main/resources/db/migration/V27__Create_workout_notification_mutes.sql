CREATE TABLE IF NOT EXISTS health_workout_notification_mutes (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    muted_user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_muted_user UNIQUE (user_id, muted_user_id)
);

CREATE INDEX IF NOT EXISTS idx_workout_mute_user ON health_workout_notification_mutes(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_mute_muted ON health_workout_notification_mutes(muted_user_id);
