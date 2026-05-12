package com.payment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a duplicate transaction is detected via idempotency key lookup.
 * The caller should return the cached response rather than processing again.
 */
public class DuplicateTransactionException extends PaymentException {

    private static final String ERROR_CODE = "DUPLICATE_TRANSACTION";

    public DuplicateTransactionException(String idempotencyKey) {
        super(
            "Duplicate transaction detected for idempotency key: " + idempotencyKey,
            ERROR_CODE,
            HttpStatus.CONFLICT
        );
    }

    public DuplicateTransactionException(String idempotencyKey, String message) {
        super(message, ERROR_CODE, HttpStatus.CONFLICT);
    }
}
