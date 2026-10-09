CREATE TABLE short_url (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(8)  NOT NULL UNIQUE,
    original_url TEXT        NOT NULL,
    expires_at   TIMESTAMPTZ,
    visit_count  BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);
