-- Payment Service Database Schema
-- V1: Create payments and idempotency_records tables

CREATE TABLE IF NOT EXISTS payments (
    id               VARCHAR(36)    NOT NULL,
    idempotency_key  VARCHAR(128)   NOT NULL,
    user_id          VARCHAR(64)    NOT NULL,
    amount           DECIMAL(19,4)  NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    payment_method   VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    payer_id         VARCHAR(64)    NOT NULL,
    payee_id         VARCHAR(64)    NOT NULL,
    description      VARCHAR(255),
    failure_reason   VARCHAR(512),
    metadata         JSON,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6),
    version          BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_idempotency_key (idempotency_key),
    INDEX idx_payments_user_id        (user_id),
    INDEX idx_payments_status         (status),
    INDEX idx_payments_created_at     (created_at),
    INDEX idx_payments_payer_id       (payer_id),
    INDEX idx_payments_payee_id       (payee_id),
    INDEX idx_payments_user_status    (user_id, status),
    INDEX idx_payments_user_created   (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS idempotency_records (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    idempotency_key  VARCHAR(128) NOT NULL,
    payment_id       VARCHAR(36),
    response_body    TEXT,
    http_status      INT          NOT NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    INDEX idx_idempotency_expires_at (expires_at),
    INDEX idx_idempotency_payment_id (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
