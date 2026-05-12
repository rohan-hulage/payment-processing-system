package com.payment.service.repository;

import com.payment.common.enums.PaymentStatus;
import com.payment.service.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByUserId(String userId, Pageable pageable);

    Page<Payment> findByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable);

    Page<Payment> findByPayerId(String payerId, Pageable pageable);

    Page<Payment> findByPayeeId(String payeeId, Pageable pageable);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant cutoff);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :newStatus, p.failureReason = :reason WHERE p.id = :id AND p.status = :currentStatus")
    int updateStatusIfCurrent(
        @Param("id") String id,
        @Param("currentStatus") PaymentStatus currentStatus,
        @Param("newStatus") PaymentStatus newStatus,
        @Param("reason") String reason
    );
}
