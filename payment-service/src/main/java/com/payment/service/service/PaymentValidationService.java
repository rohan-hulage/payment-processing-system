package com.payment.service.service;

import com.payment.common.dto.PaymentRequest;
import com.payment.common.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

/**
 * Business-level validation for payment requests, beyond Bean Validation constraints.
 */
@Service
@Slf4j
public class PaymentValidationService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "SGD"
    );

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");

    /**
     * Validates a payment request for business rule compliance.
     *
     * @param request        the payment request to validate
     * @param idempotencyKey the idempotency key from the request header
     * @throws PaymentException if any validation rule is violated
     */
    public void validate(PaymentRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        validateAmount(request.getAmount());
        validateCurrency(request.getCurrency());
        validatePayerPayeeDifferent(request.getPayerId(), request.getPayeeId());
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new PaymentException(
                "X-Idempotency-Key header is required",
                "MISSING_IDEMPOTENCY_KEY",
                HttpStatus.BAD_REQUEST
            );
        }
        if (idempotencyKey.length() > 128) {
            throw new PaymentException(
                "X-Idempotency-Key must not exceed 128 characters",
                "INVALID_IDEMPOTENCY_KEY",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new PaymentException("Amount is required", "MISSING_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new PaymentException(
                "Amount must be at least " + MIN_AMOUNT,
                "AMOUNT_TOO_SMALL",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new PaymentException(
                "Amount must not exceed " + MAX_AMOUNT,
                "AMOUNT_TOO_LARGE",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    private void validateCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw new PaymentException("Currency is required", "MISSING_CURRENCY", HttpStatus.BAD_REQUEST);
        }
        if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            throw new PaymentException(
                "Unsupported currency: " + currency + ". Supported: " + SUPPORTED_CURRENCIES,
                "UNSUPPORTED_CURRENCY",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        // Verify it's a valid ISO 4217 code
        try {
            Currency.getInstance(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentException(
                "Invalid ISO 4217 currency code: " + currency,
                "INVALID_CURRENCY",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validatePayerPayeeDifferent(String payerId, String payeeId) {
        if (StringUtils.hasText(payerId) && payerId.equals(payeeId)) {
            throw new PaymentException(
                "Payer and payee must be different",
                "SAME_PAYER_PAYEE",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }
}
