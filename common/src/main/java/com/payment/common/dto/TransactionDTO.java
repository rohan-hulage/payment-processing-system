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

/**
 * DTO representing a transaction record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO {

    private String transactionId;
    private String paymentId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String payerId;
    private String payeeId;
    private String description;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
