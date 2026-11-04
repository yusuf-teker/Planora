-- HealthTrack Application Tables (V25)
-- Isolated tables for health features linked to users and tasks

-- 1. Food Logs
CREATE TABLE IF NOT EXISTS health_food_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date VARCHAR(10) NOT NULL, -- YYYY-MM-DD
    raw_text TEXT NOT NULL,
    summary TEXT,
    total_calories INT NOT NULL DEFAULT 0,
    items_json TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL, -- Epoch milliseconds for Last-Write-Wins
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_health_food_logs_user_date ON health_food_logs(user_id, date);
CREATE INDEX IF NOT EXISTS idx_health_food_logs_updated_at ON health_food_logs(user_id, updated_at);

-- 2. Nutrition Goals
CREATE TABLE IF NOT EXISTS health_nutrition_goals (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    calories INT NOT NULL DEFAULT 2000,
    protein_g DOUBLE PRECISION NOT NULL DEFAULT 80.0,
    carbs_g DOUBLE PRECISION NOT NULL DEFAULT 250.0,
    fat_g DOUBLE PRECISION NOT NULL DEFAULT 65.0,
    fiber_g DOUBLE PRECISION NOT NULL DEFAULT 30.0,
    vitamin_c_mg DOUBLE PRECISION NOT NULL DEFAULT 90.0,
    vitamin_d_ug DOUBLE PRECISION NOT NULL DEFAULT 15.0,
    vitamin_b12_ug DOUBLE PRECISION NOT NULL DEFAULT 2.4,
    iron_mg DOUBLE PRECISION NOT NULL DEFAULT 14.0,
    calcium_mg DOUBLE PRECISION NOT NULL DEFAULT 1000.0,
    magnesium_mg DOUBLE PRECISION NOT NULL DEFAULT 350.0,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_health_nutrition_goals_user ON health_nutrition_goals(user_id);

-- 3. Workouts
CREATE TABLE IF NOT EXISTS health_workouts (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date VARCHAR(10) NOT NULL, -- YYYY-MM-DD
    raw_text TEXT NOT NULL,
    calories_burned INT NOT NULL DEFAULT 0,
    duration_minutes INT NOT NULL DEFAULT 0,
    target_calories INT NOT NULL DEFAULT 300,
    target_duration_minutes INT NOT NULL DEFAULT 45,
    ai_analysis TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_health_workouts_user_date ON health_workouts(user_id, date);
CREATE INDEX IF NOT EXISTS idx_health_workouts_updated_at ON health_workouts(user_id, updated_at);

-- 4. Weight Tracking
CREATE TABLE IF NOT EXISTS health_weights (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date VARCHAR(10) NOT NULL, -- YYYY-MM-DD
    weight_kg DOUBLE PRECISION NOT NULL,
    target_weight_kg DOUBLE PRECISION,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_health_weights_user_date ON health_weights(user_id, date);
CREATE INDEX IF NOT EXISTS idx_health_weights_updated_at ON health_weights(user_id, updated_at);

-- 5. Appointments & Prescriptions
CREATE TABLE IF NOT EXISTS health_appointments (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date_time BIGINT NOT NULL, -- Epoch milliseconds
    doctor_name VARCHAR(255) NOT NULL,
    hospital_name VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    notes TEXT,
    reminder_minutes INT DEFAULT 60,
    planora_task_id VARCHAR(100) REFERENCES tasks(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_health_appointments_user_date ON health_appointments(user_id, date_time);
CREATE INDEX IF NOT EXISTS idx_health_appointments_updated_at ON health_appointments(user_id, updated_at);

-- 6. Appointment Attachments Metadata (Files stored locally on device for now)
CREATE TABLE IF NOT EXISTS health_appointment_attachments (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36) NOT NULL REFERENCES health_appointments(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- PDF, IMAGE
    local_file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_health_attachments_appointment ON health_appointment_attachments(appointment_id);
