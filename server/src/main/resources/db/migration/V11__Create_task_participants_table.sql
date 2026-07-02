CREATE TABLE task_participants (
    task_id TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
