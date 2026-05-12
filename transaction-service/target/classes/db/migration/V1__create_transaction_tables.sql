-- Transaction Service Database Schema
-- V1: Create transactions table

CREATE TABLE IF NOT EXISTS transactions (
    id              VARCHAR(36)    NOT NULL,
    event_id        VARCHAR(36)    NOT NULL,
    payment_id      VARCHAR(36)    NOT NULL,
    user_id         VARCHAR(64)    NOT NULL,
    amount          DECIMAL(19,4)  NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    payment_method  VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    payer_id        VARCHAR(64)    NOT NULL,
    payee_id        VARCHAR(64)    NOT NULL,
    description     VARCHAR(255),
    failure_reason  VARCHAR(512),
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_event_id (event_id),
    INDEX idx_transactions_payment_id  (payment_id),
    INDEX idx_transactions_user_id     (user_id),
    INDEX idx_transactions_status      (status),
    INDEX idx_transactions_created_at  (created_at),
    INDEX idx_transactions_payer_id    (payer_id),
    INDEX idx_transactions_payee_id    (payee_id),
    INDEX idx_transactions_user_status (user_id, status),
    INDEX idx_transactions_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
