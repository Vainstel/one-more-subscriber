CREATE TABLE balance_deduction
(
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT         NOT NULL REFERENCES subscription (id),
    amount          DECIMAL(12, 2) NOT NULL,
    days            INT            NOT NULL,
    member_count    INT            NOT NULL DEFAULT 1,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_balance_deduction_sub ON balance_deduction (subscription_id);
