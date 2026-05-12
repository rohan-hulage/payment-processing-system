package com.payment.service.controller;

import com.payment.common.dto.ApiResponse;
import com.payment.common.dto.PaymentRequest;
import com.payment.common.dto.PaymentResponse;
import com.payment.common.enums.PaymentStatus;
import com.payment.service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments
     * Initiates a new payment. Requires X-Idempotency-Key header.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            Principal principal) {

        log.info("Payment initiation request [user={}, idempotencyKey={}]",
                principal.getName(), idempotencyKey);

        PaymentResponse response = paymentService.initiatePayment(request, idempotencyKey, principal.getName());

        // Return 200 if this was a cached (duplicate) response, 201 for new payments
        HttpStatus status = response.isCachedResponse() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(response,
                        response.isCachedResponse() ? "Duplicate request — returning cached response" : "Payment initiated"));
    }

    /**
     * GET /api/v1/payments/{paymentId}
     * Retrieves a payment by ID. Only the owning user can access it.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable String paymentId,
            Principal principal) {

        PaymentResponse response = paymentService.getPaymentById(paymentId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/payments
     * Lists payments for the authenticated user with optional status filter and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> listPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        Page<PaymentResponse> payments = status != null
                ? paymentService.getPaymentsByUserAndStatus(principal.getName(), status, pageable)
                : paymentService.getPaymentsByUser(principal.getName(), pageable);

        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * DELETE /api/v1/payments/{paymentId}
     * Cancels a PENDING payment. Only the owning user can cancel.
     */
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable String paymentId,
            Principal principal) {

        log.info("Payment cancellation request [paymentId={}, user={}]", paymentId, principal.getName());
        PaymentResponse response = paymentService.cancelPayment(paymentId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Payment cancelled"));
    }
}
