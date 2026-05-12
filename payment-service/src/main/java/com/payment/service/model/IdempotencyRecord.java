package com.payment.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity that stores idempotency key records.
 * When a payment request arrives, the service checks this table first.
 * If a record exists, the cached response is returned without re-processing.
 *
 * Records are retained for 24 hours (configurable) to cover retry windows.
 */
@Entity
@Table(
    name = "idempotency_records",
    indexes = {
        @Index(name = "idx_idempotency_key",        columnList = "idempotency_key", unique = true),
        @Index(name = "idx_idempotency_expires_at",  columnList = "expires_at"),
        @Index(name = "idx_idempotency_payment_id",  columnList = "payment_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    /**
     * Serialized JSON of the PaymentResponse returned for this key.
     * Stored so the exact same response can be replayed on duplicate requests.
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * After this timestamp the record may be purged by the cleanup scheduler.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
