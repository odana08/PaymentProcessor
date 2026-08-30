CREATE TABLE payments (
    reference CHAR(36) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    amount_in_cents DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fee_in_cents DECIMAL(19, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (reference),
    CONSTRAINT chk_payments_amount_positive CHECK (amount_in_cents > 0),
    CONSTRAINT chk_payments_fee_non_negative CHECK (fee_in_cents >= 0),
    CONSTRAINT chk_payments_currency CHECK (currency IN ('JOD', 'USD')),
    CONSTRAINT chk_payments_status CHECK (status IN ('CREATED', 'PROCESSED'))
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_currency ON payments (currency);
CREATE INDEX idx_payments_type ON payments (payment_type);

CREATE TABLE payment_notification_channels (
    payment_reference CHAR(36) NOT NULL,
    channel_order INTEGER NOT NULL,
    channel_name VARCHAR(32) NOT NULL,
    PRIMARY KEY (payment_reference, channel_order),
    CONSTRAINT uk_payment_channel UNIQUE (payment_reference, channel_name),
    CONSTRAINT fk_payment_channel_payment
        FOREIGN KEY (payment_reference) REFERENCES payments (reference)
        ON DELETE CASCADE
);
