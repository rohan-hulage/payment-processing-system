package com.payment.transaction.service;

import com.payment.common.dto.TransactionDTO;
import com.payment.common.enums.PaymentStatus;
import com.payment.common.events.PaymentEvent;
import com.payment.common.exception.PaymentException;
import com.payment.transaction.model.Transaction;
import com.payment.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Creates a transaction record from a Kafka payment event.
     * Skips processing if a transaction with the same event ID already exists
     * (consumer-side idempotency).
     *
     * @param event the payment event from Kafka
     */
    @Transactional
    public void createFromEvent(PaymentEvent event) {
        if (transactionRepository.existsByEventId(event.getEventId())) {
            log.info("Skipping duplicate event [eventId={}]", event.getEventId());
            return;
        }

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .eventId(event.getEventId())
                .paymentId(event.getPaymentId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .paymentMethod(event.getPaymentMethod())
                .status(event.getStatus())
                .payerId(event.getPayerId())
                .payeeId(event.getPayeeId())
                .description(event.getDescription())
                .failureReason(event.getFailureReason())
                .build();

        transactionRepository.save(transaction);
        log.info("Transaction created [id={}, paymentId={}, status={}]",
                transaction.getId(), transaction.getPaymentId(), transaction.getStatus());
    }

    // -------------------------------------------------------------------------
    // Query operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TransactionDTO getById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .map(this::toDTO)
                .orElseThrow(() -> new PaymentException(
                    "Transaction not found: " + transactionId,
                    "TRANSACTION_NOT_FOUND",
                    HttpStatus.NOT_FOUND
                ));
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getByUserId(String userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable) {
        return transactionRepository.findByUserIdAndStatus(userId, status, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getByPaymentId(String paymentId) {
        return transactionRepository.findByPaymentId(paymentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getByPaymentIdPaged(String paymentId, Pageable pageable) {
        return transactionRepository.findByPaymentId(paymentId, pageable).map(this::toDTO);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private TransactionDTO toDTO(Transaction t) {
        return TransactionDTO.builder()
                .transactionId(t.getId())
                .paymentId(t.getPaymentId())
                .userId(t.getUserId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .payerId(t.getPayerId())
                .payeeId(t.getPayeeId())
                .description(t.getDescription())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
