-- Migration V29: Add body composition columns (body fat %, muscle mass kg, muscle %) to health_weights
ALTER TABLE health_weights ADD COLUMN IF NOT EXISTS body_fat_percent DOUBLE PRECISION;
ALTER TABLE health_weights ADD COLUMN IF NOT EXISTS muscle_mass_kg DOUBLE PRECISION;
ALTER TABLE health_weights ADD COLUMN IF NOT EXISTS muscle_percent DOUBLE PRECISION;
