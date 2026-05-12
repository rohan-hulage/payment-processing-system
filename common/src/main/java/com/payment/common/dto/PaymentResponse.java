package com.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payment.common.enums.PaymentMethod;
import com.payment.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * DTO representing the result of a payment operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private String paymentId;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String payerId;
    private String payeeId;
    private String description;
    private Map<String, String> metadata;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Indicates whether this response was served from an idempotency cache
     * (i.e., a duplicate request was detected).
     */
    private boolean cachedResponse;
}
