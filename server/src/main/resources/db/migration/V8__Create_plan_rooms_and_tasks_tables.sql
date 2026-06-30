CREATE TABLE plan_rooms (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    creator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL
);

CREATE TABLE plan_room_members (
    room_id VARCHAR(36) NOT NULL REFERENCES plan_rooms(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at BIGINT,
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    creator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_rule VARCHAR(255),
    is_flexible BOOLEAN NOT NULL DEFAULT FALSE,
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    is_postponable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE task_shared_rooms (
    task_id VARCHAR(36) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    room_id VARCHAR(36) NOT NULL REFERENCES plan_rooms(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, room_id)
);
