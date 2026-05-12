package com.payment.transaction.model;

import com.payment.common.enums.PaymentMethod;
import com.payment.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a transaction record.
 * Transactions are created by consuming Kafka payment events.
 * Each payment state change produces a separate transaction record.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_payment_id",  columnList = "payment_id"),
        @Index(name = "idx_transactions_user_id",     columnList = "user_id"),
        @Index(name = "idx_transactions_status",      columnList = "status"),
        @Index(name = "idx_transactions_created_at",  columnList = "created_at"),
        @Index(name = "idx_transactions_payer_id",    columnList = "payer_id"),
        @Index(name = "idx_transactions_payee_id",    columnList = "payee_id"),
        @Index(name = "idx_transactions_event_id",    columnList = "event_id", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /**
     * The Kafka event ID — used for consumer-side deduplication.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "payment_id", nullable = false, length = 36)
    private String paymentId;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

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
