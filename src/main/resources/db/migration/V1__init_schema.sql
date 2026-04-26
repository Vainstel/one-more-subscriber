CREATE TABLE bot_user
(
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT       NOT NULL UNIQUE,
    username      VARCHAR(255),
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255),
    registered_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE service
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)   NOT NULL,
    description      TEXT,
    rules            TEXT,
    monthly_cost     DECIMAL(12, 2) NOT NULL,
    currency         VARCHAR(10)    NOT NULL DEFAULT 'RUB',
    max_members      INTEGER        NOT NULL,
    created_by       BIGINT         NOT NULL REFERENCES bot_user (id),
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    billing_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    password         VARCHAR(255),
    join_description TEXT,
    service_type     VARCHAR(50)    NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE subscription
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES bot_user (id),
    service_id       BIGINT         NOT NULL REFERENCES service (id),
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    joined_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    deactivated_at   TIMESTAMP,
    paid_until       TIMESTAMP,
    balance          DECIMAL(12, 2) NOT NULL DEFAULT 0,
    last_deducted_at TIMESTAMP,
    UNIQUE (user_id, service_id)
);

CREATE TABLE payment
(
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT         NOT NULL REFERENCES subscription (id),
    amount          DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(10)    NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);

CREATE TABLE tip_payment
(
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT         NOT NULL REFERENCES subscription (id),
    amount          DECIMAL(12, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);

CREATE TABLE creator_message
(
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT    NOT NULL REFERENCES subscription (id),
    message_text    TEXT      NOT NULL,
    delivered       BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE error_log
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT REFERENCES bot_user (id),
    action         VARCHAR(255) NOT NULL,
    exception_type VARCHAR(255) NOT NULL,
    message        TEXT,
    stack_trace    TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_log
(
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT      NOT NULL REFERENCES subscription (id),
    sent_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
    type            VARCHAR(50) NOT NULL
);

-- Indexes
CREATE INDEX idx_subscription_service_active ON subscription (service_id, active);
CREATE INDEX idx_payment_sub_deleted_created ON payment (subscription_id, deleted, created_at);
CREATE INDEX idx_tip_sub_deleted_created ON tip_payment (subscription_id, deleted, created_at);
CREATE INDEX idx_notification_sub_type_sent ON notification_log (subscription_id, type, sent_at);
