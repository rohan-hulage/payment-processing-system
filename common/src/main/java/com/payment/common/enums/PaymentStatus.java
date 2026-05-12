package com.payment.common.enums;

/**
 * Represents the lifecycle states of a payment.
 */
public enum PaymentStatus {
    /** Payment has been received but not yet processed. */
    PENDING,
    /** Payment is actively being processed by the payment processor. */
    PROCESSING,
    /** Payment has been successfully completed. */
    COMPLETED,
    /** Payment processing failed. */
    FAILED,
    /** Payment was refunded after completion. */
    REFUNDED
}
