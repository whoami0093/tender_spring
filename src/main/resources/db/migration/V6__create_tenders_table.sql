CREATE TABLE tenders
(
    id              BIGSERIAL        PRIMARY KEY,
    purchase_number VARCHAR(100)     NOT NULL,
    title           TEXT             NOT NULL,
    region          VARCHAR(255),
    customer        VARCHAR(500),
    customer_inn    VARCHAR(12),
    amount          NUMERIC(18, 2),
    currency        VARCHAR(10)      NOT NULL DEFAULT 'RUB',
    status          VARCHAR(50)      NOT NULL DEFAULT 'SENT',
    deadline        TIMESTAMPTZ,
    published_at    TIMESTAMPTZ,
    eis_url         TEXT,
    taken_in_work   BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenders_purchase_number UNIQUE (purchase_number)
);

CREATE INDEX idx_tenders_region        ON tenders (region);
CREATE INDEX idx_tenders_status        ON tenders (status);
CREATE INDEX idx_tenders_taken_in_work ON tenders (taken_in_work);
CREATE INDEX idx_tenders_deadline      ON tenders (deadline);
CREATE INDEX idx_tenders_created_at    ON tenders (created_at DESC);
CREATE INDEX idx_tenders_customer_lower ON tenders (lower(customer));

COMMENT ON TABLE tenders IS 'Parsed tenders sent via email';
