package com.payment.transaction.controller;

import com.payment.common.dto.ApiResponse;
import com.payment.common.dto.TransactionDTO;
import com.payment.common.enums.PaymentStatus;
import com.payment.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * GET /api/v1/transactions/{transactionId}
     * Retrieves a single transaction by ID.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getById(
            @PathVariable String transactionId) {
        TransactionDTO dto = transactionService.getById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * GET /api/v1/transactions
     * Lists transactions for the authenticated user with optional status filter and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> listByUser(
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

        Page<TransactionDTO> transactions = status != null
                ? transactionService.getByUserIdAndStatus(principal.getName(), status, pageable)
                : transactionService.getByUserId(principal.getName(), pageable);

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * GET /api/v1/transactions/payment/{paymentId}
     * Lists all transactions associated with a specific payment ID.
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> listByPayment(
            @PathVariable String paymentId) {
        List<TransactionDTO> transactions = transactionService.getByPaymentId(paymentId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * GET /api/v1/transactions/payment/{paymentId}/paged
     * Paginated version of the payment transaction list.
     */
    @GetMapping("/payment/{paymentId}/paged")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> listByPaymentPaged(
            @PathVariable String paymentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        Page<TransactionDTO> transactions = transactionService.getByPaymentIdPaged(paymentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
