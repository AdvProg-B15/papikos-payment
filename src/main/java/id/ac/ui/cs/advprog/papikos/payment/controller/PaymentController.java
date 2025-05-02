package id.ac.ui.cs.advprog.papikos.payment.controller;

import id.ac.ui.cs.advprog.papikos.payment.dto.BalanceDto;
import id.ac.ui.cs.advprog.papikos.payment.dto.PaymentRequest;
import id.ac.ui.cs.advprog.papikos.payment.dto.TopUpInitiationResponse;
import id.ac.ui.cs.advprog.papikos.payment.dto.TopUpRequest;
import id.ac.ui.cs.advprog.papikos.payment.dto.TransactionDto;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
import jakarta.validation.Valid; // Use Jakarta validation
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt; // Example: Using JWT for authentication principal
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID; // Use UUID


@RestController
@RequestMapping("/api/v1/payments") // Base path for all payment endpoints
@RequiredArgsConstructor // Auto-injects final fields via constructor
@Validated // Enables validation for parameters like TransactionType enum
@Slf4j // Logging
public class PaymentController {

    private final PaymentService paymentService; // The service layer dependency

    private UUID getUserIdFromPrincipal(Jwt principal) {
        if (principal == null) {
            // This situation should ideally be prevented by Spring Security filters
            log.error("Authentication principal is missing in request.");
            throw new IllegalStateException("Authentication principal is missing.");
        }
        String subject = principal.getSubject(); // 'sub' claim often holds the user ID
        if (subject == null) {
            log.error("JWT 'sub' claim is missing.");
            throw new IllegalStateException("User identifier (subject) missing in authentication token.");
        }
        try {
            // Attempt to parse the subject claim as a UUID
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            log.error("Could not parse UUID from JWT subject: {}", subject, e);
            throw new IllegalStateException("Invalid user ID format (UUID expected) in authentication token.");
        }
    }

    /**
     * Endpoint to initiate a balance top-up for the authenticated user.
     * POST /api/v1/payments/topup/initiate
     */
    @PostMapping("/topup/initiate")
    public ResponseEntity<TopUpInitiationResponse> initiateTopUp(
            @Valid @RequestBody TopUpRequest request, // Validate the request body DTO
            @AuthenticationPrincipal Jwt principal) { // Get authenticated user info
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("API: Received top-up initiation request for userId: {} with amount {}", userId, request.getAmount());
        TopUpInitiationResponse response = paymentService.initiateTopUp(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to handle webhook callbacks from an external payment gateway
     * confirming a top-up transaction.
     * POST /api/v1/payments/topup/webhook
     * NOTE: This endpoint is typically NOT protected by user authentication,
     * but MUST be secured by other means (e.g., verifying webhook signature).
     */
    @PostMapping("/topup/webhook")
    public ResponseEntity<Void> handleTopUpWebhook(@RequestBody String payload) {
        // --- SECURITY WARNING ---
        // Real implementation MUST validate the incoming request, usually by
        // checking a signature provided by the payment gateway using a shared secret.
        // This example does NOT include signature validation and is INSECURE.
        log.warn("API: Received webhook payload. --- SIGNATURE VALIDATION IS REQUIRED FOR PRODUCTION ---");
        log.debug("Webhook payload content: {}", payload);

        // --- Placeholder for Payload Parsing and Confirmation ---
        // You would parse the payload (e.g., JSON) to extract the transaction ID
        // and the status reported by the gateway.
        try {
            // Example: Basic check and extraction (Replace with proper JSON parsing and logic)
            if (payload != null && payload.contains("SUCCESS")) { // VERY basic check
                // Extract transaction ID (EXTREMELY brittle, use a JSON library!)
                int txIdIndex = payload.indexOf("transaction_id"); // Or whatever the gateway calls it
                if (txIdIndex != -1) {
                    // Extract the UUID string... (complex string manipulation needed)
                    // String txIdString = ... extract UUID string ...
                    // UUID transactionId = UUID.fromString(txIdString);
                    // paymentService.confirmTopUp(transactionId);
                    log.info("API: Webhook indicates success, assuming confirmation processed (IMPLEMENT PROPER PARSING!)");
                    return ResponseEntity.ok().build(); // Acknowledge receipt
                } else {
                    log.warn("API: Webhook payload indicates success but transaction ID not found in payload.");
                    return ResponseEntity.badRequest().build(); // Indicate payload issue
                }
            } else {
                log.warn("API: Webhook payload did not indicate success status.");
                // Still return OK usually, so gateway doesn't keep retrying for non-success statuses
                return ResponseEntity.ok().build();
            }
        } catch (Exception e) {
            log.error("API: Error processing webhook payload: {}", e.getMessage(), e);
            // Return 500 to signal processing error, potentially causing gateway retry
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint for the authenticated tenant to pay for a rental using their balance.
     * POST /api/v1/payments/pay
     */
    @PostMapping("/pay")
    public ResponseEntity<TransactionDto> payForRental(
            @Valid @RequestBody PaymentRequest request, // Validate request DTO
            @AuthenticationPrincipal Jwt principal) { // Get authenticated user
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("API: Received payment request for rentalId: {} from userId: {}", request.getRentalId(), userId);
        TransactionDto transactionDto = paymentService.payForRental(userId, request);
        log.info("API: Payment successful for rentalId: {}. TransactionId: {}", request.getRentalId(), transactionDto.transactionId());
        return ResponseEntity.ok(transactionDto);
    }

    /**
     * Endpoint for the authenticated user to retrieve their current balance.
     * GET /api/v1/payments/balance
     */
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
