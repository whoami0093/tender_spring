CREATE TABLE subscriptions
(
    id                    BIGSERIAL PRIMARY KEY,
    source                VARCHAR(50)   NOT NULL,
    label                 VARCHAR(255),
    emails                TEXT          NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    filter_regions        VARCHAR(255),
    filter_object_info    VARCHAR(500),
    filter_customer_inn   VARCHAR(12),
    filter_max_price_from NUMERIC(18, 2),
    filter_max_price_to   NUMERIC(18, 2),
    last_checked_at       TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_status ON subscriptions (status);

COMMENT ON TABLE subscriptions IS 'Tender monitoring subscriptions';
COMMENT ON COLUMN subscriptions.source IS 'GOSPLAN_44 | GOSPLAN_223';
COMMENT ON COLUMN subscriptions.emails IS 'JSON array of recipient emails';
COMMENT ON COLUMN subscriptions.status IS 'ACTIVE | PAUSED';
COMMENT ON COLUMN subscriptions.filter_regions IS 'Comma-separated region codes, e.g. 77,78';
