package id.ac.ui.cs.advprog.papikos.payment.controller;

import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.response.ApiResponse;
import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.oauth2.jwt.Jwt; // No longer needed here if you use Authentication
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // Use your friend's method
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Authentication principal is required but missing.");
            // Consider throwing a more specific Spring Security exception if appropriate,
            // or ensure your global exception handler catches IllegalStateException well.
            throw new IllegalStateException("Authentication principal is required but missing.");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            log.error("Error parsing UUID from principal name: {}", authentication.getName(), e);
            throw new IllegalArgumentException("Invalid user identifier format in authentication token.");
        }
    }

    @PostMapping("/topup")
    public ApiResponse<TransactionDto> topUp(
            @RequestBody TopUpRequest request,
            Authentication authentication // Changed from Jwt to Authentication
    ) {
        UUID userId = getUserIdFromAuthentication(authentication); // Use the new method
        //UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // For testing
        log.info("API: Received top-up initiation request for userId: {} with amount {}", userId, request.amount());
        TransactionDto completedTransaction = paymentService.topUp(userId, request);
        log.info("API: Top-Up successful for userId: {} TransactionId {}", userId, completedTransaction.transactionId());
        return ApiResponse.<TransactionDto>builder()
                .status(HttpStatus.OK)
                .message("Payment successful")
                .data(completedTransaction)
                .build();
    }

    @PostMapping("/pay")
    public ApiResponse<TransactionDto> payForRental(
            @RequestBody PaymentRequest request,
           Authentication authentication // Changed from Jwt to Authentication
    ) {
        UUID userId = getUserIdFromAuthentication(authentication); // Use the new method
        log.info("API: Received payment request for rentalId: {} from userId: {}", request.rentalId(), userId);
        TransactionDto transactionDto = paymentService.payForRental(userId, request);
        log.info("API: Payment successful for rentalId: {}. TransactionId: {}", request.rentalId(), transactionDto.transactionId());
        return ApiResponse.<TransactionDto>builder()
                .status(HttpStatus.OK)
                .message("Payment successful")
                .data(transactionDto)
                .build();
    }

    @GetMapping("/balance")
    public ApiResponse<BalanceDto> getMyBalance(
            Authentication authentication // Changed from Jwt to Authentication
    ) {
        UUID userId = getUserIdFromAuthentication(authentication); // Use the new method
        log.info("API: Received balance request for userId: {}", userId);
        BalanceDto balanceDto = paymentService.getUserBalance(userId);
        return ApiResponse.<BalanceDto>builder()
                .status(HttpStatus.OK)
                .message("Payment successful")
                .data(balanceDto)
                .build();

    }

    @GetMapping("/transactions")
    public ApiResponse<Page<TransactionDto>> getMyTransactionHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            Pageable pageable,
            Authentication authentication // Changed from Jwt to Authentication
    ) {
        UUID userId = getUserIdFromAuthentication(authentication); // Use the new method
        log.info("API: Received transaction history request for userId: {} with params - Start: {}, End: {}, Type: {}, Page: {}",
                userId, startDate, endDate, type, pageable);
        Page<TransactionDto> historyPage = paymentService.getTransactionHistory(userId, startDate, endDate, type, pageable);
        return ApiResponse.<Page<TransactionDto>>builder()
                .status(HttpStatus.OK)
                .message("Transaction history retrieved successfully")
                .data(historyPage)
                .build();
    }
}