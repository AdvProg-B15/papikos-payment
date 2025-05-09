package id.ac.ui.cs.advprog.papikos.payment.controller;

import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.Transaction;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID; // Use UUID


@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    private UUID getUserIdFromPrincipal(Jwt principal) {
        return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
//        if (principal == null) {
//            // This situation should ideally be prevented by Spring Security filters
//            log.error("Authentication principal is missing in request.");
//            throw new IllegalStateException("Authentication principal is missing.");
//        }
//        String subject = principal.getSubject(); // 'sub' claim often holds the user ID
//        if (subject == null) {
//            log.error("JWT 'sub' claim is missing.");
//            throw new IllegalStateException("User identifier (subject) missing in authentication token.");
//        }
//        try {
//            // Attempt to parse the subject claim as a UUID
//            return UUID.fromString(subject);
//        } catch (IllegalArgumentException e) {
//            log.error("Could not parse UUID from JWT subject: {}", subject, e);
//            throw new IllegalStateException("Invalid user ID format (UUID expected) in authentication token.");
//        }
    }

    @PostMapping("/topup/initiate")
    public ResponseEntity<TransactionDto> TopUp(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal Jwt principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("API: Received top-up initiation request for userId: {} with amount {}", userId, request.amount());
        TransactionDto completedTransaction = paymentService.TopUp(userId, request);
        log.info("API: Top-Up successful for userId: {} TransactionId {}", userId, completedTransaction.transactionId());
        return ResponseEntity.ok(completedTransaction);
    }


    @PostMapping("/pay")
    public ResponseEntity<TransactionDto> payForRental(
            @Valid @RequestBody PaymentRequest request, // Validate request DTO
            @AuthenticationPrincipal Jwt principal) { // Get authenticated user
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("API: Received payment request for rentalId: {} from userId: {}", request.rentalId(), userId);
        TransactionDto transactionDto = paymentService.payForRental(userId, request);
        log.info("API: Payment successful for rentalId: {}. TransactionId: {}", request.rentalId(), transactionDto.transactionId());
        return ResponseEntity.ok(transactionDto);
    }


    @GetMapping("/balance")
    public ResponseEntity<BalanceDto> getMyBalance(@AuthenticationPrincipal Jwt principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("API: Received balance request for userId: {}", userId);
        BalanceDto balanceDto = paymentService.getUserBalance(userId);
        return ResponseEntity.ok(balanceDto);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionDto>> getMyTransactionHistory(
            // Optional request parameters for filtering
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type, // Spring converts string to enum
            // Spring Data automatically injects pagination info from request params (e.g., ?page=0&size=10)
            Pageable pageable,
            @AuthenticationPrincipal Jwt principal) { // Get authenticated user
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("API: Received transaction history request for userId: {} with params - Start: {}, End: {}, Type: {}, Page: {}",
                userId, startDate, endDate, type, pageable);
        Page<TransactionDto> historyPage = paymentService.getTransactionHistory(userId, startDate, endDate, type, pageable);
        return ResponseEntity.ok(historyPage);
    }
}
