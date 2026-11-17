-- Migration V28: Add fitness_goal column to health_nutrition_goals so user's selected fitness goal syncs reliably across devices
ALTER TABLE health_nutrition_goals ADD COLUMN IF NOT EXISTS fitness_goal VARCHAR(50);
