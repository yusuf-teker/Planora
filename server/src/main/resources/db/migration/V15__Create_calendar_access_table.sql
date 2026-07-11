CREATE TABLE calendar_access (
    id SERIAL PRIMARY KEY,
    requester_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    granter_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    color VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT unique_calendar_access UNIQUE (requester_id, granter_id)
);
