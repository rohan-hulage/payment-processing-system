package com.payment.service.model;

import com.payment.common.enums.PaymentMethod;
import com.payment.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * JPA entity representing a payment record.
 * Uses optimistic locking via @Version to prevent concurrent update conflicts
 * in high-volume transaction scenarios.
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_user_id",        columnList = "user_id"),
        @Index(name = "idx_payments_status",          columnList = "status"),
        @Index(name = "idx_payments_created_at",      columnList = "created_at"),
        @Index(name = "idx_payments_payer_id",        columnList = "payer_id"),
        @Index(name = "idx_payments_payee_id",        columnList = "payee_id"),
        @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payer_id", nullable = false, length = 64)
    private String payerId;

    @Column(name = "payee_id", nullable = false, length = 64)
    private String payeeId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    /**
     * Serialized JSON metadata for extensible payment context.
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Optimistic locking version field.
     * Prevents lost-update anomalies when multiple threads process the same payment.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
