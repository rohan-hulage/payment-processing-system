package com.payment.common.dto;

import com.payment.common.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for initiating a payment request.
 * The idempotency key is passed via the X-Idempotency-Key HTTP header,
 * not in the request body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999999.99")
    @Digits(integer = 6, fraction = 2, message = "Amount must have at most 6 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase letters only")
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Payer ID is required")
    @Size(max = 64, message = "Payer ID must not exceed 64 characters")
    private String payerId;

    @NotBlank(message = "Payee ID is required")
    @Size(max = 64, message = "Payee ID must not exceed 64 characters")
    private String payeeId;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /**
     * Optional metadata for additional payment context (e.g., order ID, merchant data).
     */
    private Map<String, String> metadata;
}
