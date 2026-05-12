package com.payment.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.dto.PaymentResponse;
import com.payment.service.model.IdempotencyRecord;
import com.payment.service.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Manages idempotency keys to prevent duplicate payment processing.
 *
 * <p>Flow:
 * <ol>
 *   <li>Before processing, call {@link #findExistingResponse} to check for a cached result.</li>
 *   <li>If present, return the cached response immediately.</li>
 *   <li>After processing, call {@link #saveRecord} to persist the result.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.idempotency.ttl-hours:24}")
    private int ttlHours;

    /**
     * Looks up a previously stored response for the given idempotency key.
     *
     * @param idempotencyKey the client-supplied idempotency key
     * @return an Optional containing the cached PaymentResponse, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<PaymentResponse> findExistingResponse(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .map(record -> {
                    try {
                        PaymentResponse response = objectMapper.readValue(record.getResponseBody(), PaymentResponse.class);
                        response.setCachedResponse(true);
                        log.info("Idempotency cache hit for key: {}", idempotencyKey);
                        return response;
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize cached response for key {}: {}", idempotencyKey, e.getMessage());
                        return null;
                    }
                });
    }

    /**
     * Persists an idempotency record after a payment has been processed.
     *
     * @param idempotencyKey the client-supplied idempotency key
     * @param paymentId      the resulting payment ID
     * @param response       the PaymentResponse to cache
     * @param httpStatus     the HTTP status code returned to the client
     */
    @Transactional
    public void saveRecord(String idempotencyKey, String paymentId, PaymentResponse response, int httpStatus) {
        if (idempotencyRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.debug("Idempotency record already exists for key: {}", idempotencyKey);
            return;
        }

        try {
            String responseJson = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .paymentId(paymentId)
                    .responseBody(responseJson)
                    .httpStatus(httpStatus)
                    .expiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS))
                    .build();
            idempotencyRepository.save(record);
            log.debug("Saved idempotency record for key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency key {}: {}", idempotencyKey, e.getMessage());
        }
    }

    /**
     * Scheduled cleanup of expired idempotency records.
     * Runs every hour to keep the table size manageable.
     */
    @Scheduled(fixedRateString = "${payment.idempotency.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredRecords() {
        int deleted = idempotencyRepository.deleteExpiredRecords(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
    }
}
