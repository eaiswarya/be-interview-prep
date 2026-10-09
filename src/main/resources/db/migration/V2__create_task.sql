CREATE TABLE task (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date    DATE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_task_status ON task (status);
