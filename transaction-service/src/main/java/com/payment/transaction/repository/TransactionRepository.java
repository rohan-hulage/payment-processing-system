package com.payment.transaction.repository;

import com.payment.common.enums.PaymentStatus;
import com.payment.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    Page<Transaction> findByUserId(String userId, Pageable pageable);

    Page<Transaction> findByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable);

    List<Transaction> findByPaymentId(String paymentId);

    Page<Transaction> findByPaymentId(String paymentId, Pageable pageable);

    Page<Transaction> findByPayerId(String payerId, Pageable pageable);

    Page<Transaction> findByPayeeId(String payeeId, Pageable pageable);
}
