CREATE TABLE audit_log
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES bot_user (id),
    username   VARCHAR(255),
    action     TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
