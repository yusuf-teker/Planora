-- ============================================================================
-- V23: Add High-Impact Performance Indexes (B-Tree, Composite & Partial)
-- ============================================================================

-- 1. Tasks Table:
-- Foreign key index on creator_id (prevents full-table scans and speeds up CASCADE operations)
CREATE INDEX IF NOT EXISTS idx_tasks_creator_id ON tasks(creator_id);

-- Composite Index: (creator_id, start_time)
-- Speeds up calendar/timeline queries filtering by creator and sorting or filtering by time range.
CREATE INDEX IF NOT EXISTS idx_tasks_creator_start_time ON tasks(creator_id, start_time);

-- 2. Task Participants Table:
-- The PK is (task_id, user_id). Because user_id is the 2nd column, queries like
-- WHERE user_id = ? cannot use the PK index. Adding an index on user_id allows O(log N) lookup.
CREATE INDEX IF NOT EXISTS idx_task_participants_user_id ON task_participants(user_id);

-- 3. Task Shared Rooms Table:
-- The PK is (task_id, room_id). Room lookups (WHERE room_id = ?) cannot use the PK index.
CREATE INDEX IF NOT EXISTS idx_task_shared_rooms_room_id ON task_shared_rooms(room_id);

-- 4. Plan Room Members Table:
-- Partial Index: Almost all business logic checks for accepted memberships (WHERE user_id = ? AND status = 'ACCEPTED').
-- A partial index is 5-10x smaller, caches easily in RAM, and makes authorization checks instant.
CREATE INDEX IF NOT EXISTS idx_plan_room_members_user_accepted ON plan_room_members(user_id) WHERE status = 'ACCEPTED';
CREATE INDEX IF NOT EXISTS idx_plan_room_members_user_id ON plan_room_members(user_id);

-- 5. Follow Requests Table:
-- Partial Index: Pending follow requests incoming to target_id (WHERE target_id = ? AND status = 'PENDING').
CREATE INDEX IF NOT EXISTS idx_follow_requests_target_pending ON follow_requests(target_id) WHERE status = 'PENDING';

-- 6. FCM Tokens Table:
-- FCM push notification dispatch queries tokens by user_id on every notification.
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON fcm_tokens(user_id);

-- 7. Password Reset & Email Verification Tokens (Composite Indexes):
-- Code validation queries both the identifier and the 6-digit code together:
-- WHERE user_id = ? AND token = ?
-- WHERE email = ? AND token = ?
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_token ON password_reset_tokens(user_id, token);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_email_token ON email_verification_tokens(email, token);
