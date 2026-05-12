package com.payment.common.events;

import com.payment.common.enums.PaymentMethod;
import com.payment.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka event published when a payment changes state.
 * Consumed by transaction-service and notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    /**
     * Unique event identifier for deduplication on the consumer side.
     */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * The Kafka topic this event was published to.
     */
    private String eventType;

    private String paymentId;
    private String idempotencyKey;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String payerId;
    private String payeeId;
    private String description;
    private String failureReason;
    private Map<String, String> metadata;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
