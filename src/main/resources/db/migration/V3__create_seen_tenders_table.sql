CREATE TABLE seen_tenders
(
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT        NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    purchase_number VARCHAR(100)  NOT NULL,
    object_info     VARCHAR(500),
    customer_inn    VARCHAR(12),
    max_price       NUMERIC(18, 2),
    currency        VARCHAR(10),
    deadline        TIMESTAMPTZ,
    published_at    TIMESTAMPTZ,
    eis_url         TEXT,
    found_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_seen_tenders UNIQUE (subscription_id, purchase_number)
);

CREATE INDEX idx_seen_tenders_subscription ON seen_tenders (subscription_id);

COMMENT ON TABLE seen_tenders IS 'Tenders already seen per subscription (deduplication)';
