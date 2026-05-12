package com.payment.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.dto.PaymentRequest;
import com.payment.common.dto.PaymentResponse;
import com.payment.common.enums.PaymentStatus;
import com.payment.common.events.PaymentEvent;
import com.payment.common.exception.PaymentException;
import com.payment.service.kafka.PaymentEventProducer;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core payment processing service.
 *
 * <p>Idempotency flow:
 * <ol>
 *   <li>Check idempotency cache — return cached response if found.</li>
 *   <li>Validate the request.</li>
 *   <li>Persist the payment in PENDING state.</li>
 *   <li>Publish payment.initiated event to Kafka.</li>
 *   <li>Simulate processing (in production this would call an external processor).</li>
 *   <li>Update status to COMPLETED or FAILED.</li>
 *   <li>Publish the outcome event.</li>
 *   <li>Save the idempotency record.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentValidationService validationService;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Initiate payment
    // -------------------------------------------------------------------------

    /**
     * Initiates a new payment with idempotency protection.
     *
     * @param request        the payment request
     * @param idempotencyKey the client-supplied idempotency key (from X-Idempotency-Key header)
     * @param userId         the authenticated user's ID (from JWT)
     * @return the payment response
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey, String userId) {
        // Step 1: Check idempotency cache
        Optional<PaymentResponse> cached = idempotencyService.findExistingResponse(idempotencyKey);
        if (cached.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return cached.get();
        }

        // Step 2: Validate
        validationService.validate(request, idempotencyKey);

        // Step 3: Persist in PENDING state
        String paymentId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .id(paymentId)
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .payerId(request.getPayerId())
                .payeeId(request.getPayeeId())
                .description(request.getDescription())
                .metadata(serializeMetadata(request.getMetadata()))
                .build();

        paymentRepository.save(payment);
        log.info("Payment created [id={}, idempotencyKey={}]", paymentId, idempotencyKey);

        // Step 4: Publish payment.initiated
        PaymentEvent initiatedEvent = buildEvent(payment);
        eventProducer.publishPaymentInitiated(initiatedEvent);

        // Step 5: Process payment (transition to PROCESSING)
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Step 6: Simulate external processor call
        PaymentResponse response = processPayment(payment);

        // Step 7: Save idempotency record
        idempotencyService.saveRecord(idempotencyKey, paymentId, response, HttpStatus.CREATED.value());

        return response;
    }

    /**
     * Simulates calling an external payment processor.
     * In production, replace this with an actual gateway integration.
     */
    private PaymentResponse processPayment(Payment payment) {
        try {
            // Simulate processing delay and success/failure logic
            // In production: call Stripe, Braintree, etc.
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            PaymentEvent completedEvent = buildEvent(payment);
            eventProducer.publishPaymentCompleted(completedEvent);

            log.info("Payment completed [id={}]", payment.getId());
            return toResponse(payment);

        } catch (Exception e) {
            log.error("Payment processing failed [id={}]: {}", payment.getId(), e.getMessage(), e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Processing error: " + e.getMessage());
            paymentRepository.save(payment);

            PaymentEvent failedEvent = buildEvent(payment);
            eventProducer.publishPaymentFailed(failedEvent);

            return toResponse(payment);
        }
    }

    // -------------------------------------------------------------------------
    // Query operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(
                    "Payment not found: " + paymentId,
                    "PAYMENT_NOT_FOUND",
                    HttpStatus.NOT_FOUND
                ));

        // Ensure the requesting user owns this payment
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(
                "Access denied to payment: " + paymentId,
                "ACCESS_DENIED",
                HttpStatus.FORBIDDEN
            );
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUser(String userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUserAndStatus(
            String userId, PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    // Cancel payment
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentResponse cancelPayment(String paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(
                    "Payment not found: " + paymentId,
                    "PAYMENT_NOT_FOUND",
                    HttpStatus.NOT_FOUND
                ));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(
                "Access denied to payment: " + paymentId,
                "ACCESS_DENIED",
                HttpStatus.FORBIDDEN
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(
                "Only PENDING payments can be cancelled. Current status: " + payment.getStatus(),
                "INVALID_STATUS_TRANSITION",
                HttpStatus.CONFLICT
            );
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Cancelled by user");
        paymentRepository.save(payment);

        PaymentEvent failedEvent = buildEvent(payment);
        eventProducer.publishPaymentFailed(failedEvent);

        log.info("Payment cancelled [id={}, userId={}]", paymentId, userId);
        return toResponse(payment);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PaymentEvent buildEvent(Payment payment) {
        return PaymentEvent.builder()
                .paymentId(payment.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .description(payment.getDescription())
                .failureReason(payment.getFailureReason())
                .metadata(deserializeMetadata(payment.getMetadata()))
                .build();
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .description(payment.getDescription())
                .failureReason(payment.getFailureReason())
                .metadata(deserializeMetadata(payment.getMetadata()))
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .cachedResponse(false)
                .build();
    }

    private String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> deserializeMetadata(String metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize metadata: {}", e.getMessage());
            return null;
        }
    }
}
