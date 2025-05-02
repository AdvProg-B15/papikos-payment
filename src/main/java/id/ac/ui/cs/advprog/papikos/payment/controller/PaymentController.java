package id.ac.ui.cs.advprog.papikos.payment.controller;

import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
// Import Principal or custom annotation for getting user ID
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt; // Example if using JWT principal
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // Use Jakarta validation
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payments") // Consistent base path
@RequiredArgsConstructor
@Validated // Enable validation of request params like TransactionType
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // Helper to get User ID from Principal (adjust based on your auth setup)
    private Long getUserIdFromPrincipal(Jwt principal) {
        if (principal == null) {
            // This should ideally be caught by security filters
            throw new IllegalStateException("Authentication principal is missing.");
        }
        // Assuming user ID is stored in the 'sub' claim. Adjust as needed.
        try {
            return Long.parseLong(principal.getSubject());
        } catch (NumberFormatException e) {
            log.error("Could not parse user ID from JWT subject: {}", principal.getSubject());
            throw new IllegalStateException("Invalid user ID format in authentication token.");
        }
    }

    @PostMapping("/topup/initiate")
    public ResponseEntity<TopUpInitiationResponse> initiateTopUp(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal Jwt principal) { // Inject principal
        Long userId = getUserIdFromPrincipal(principal);
        log.info("Received top-up initiation request for userId: {}", userId);
        TopUpInitiationResponse response = paymentService.initiateTopUp(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/topup/webhook")
    public ResponseEntity<Void> handleTopUpWebhook(@RequestBody String payload) { // Keep payload generic for now
        // WARNING: Production webhooks NEED proper validation (signatures, etc.)
        log.warn("Received raw webhook payload. IMPLEMENT SIGNATURE VALIDATION!");
        log.debug("Webhook payload: {}", payload);

        // Basic parsing example (replace with robust JSON parsing and validation)
        try {
            // Use ObjectMapper or similar to parse the payload securely
            // Extract the relevant transaction ID confirmed by the gateway
            // For this example, let's assume a simple parsing logic (NOT PRODUCTION READY)
            if (payload.contains("\"status\": \"SUCCESS\"") && payload.contains("\"transaction_id\":")) {
                // Extremely basic extraction - use a JSON library!
                String txIdStr = payload.substring(payload.indexOf("\"transaction_id\":") + 18);
                txIdStr = txIdStr.substring(0, txIdStr.indexOf(",")); // Assuming comma follows
                long transactionId = Long.parseLong(txIdStr.trim());

                log.info("Processing successful webhook confirmation for transactionId: {}", transactionId);
                paymentService.confirmTopUp(transactionId);
                return ResponseEntity.ok().build();
            } else {
                log.warn("Webhook payload did not indicate success or transaction ID was missing.");
                return ResponseEntity.badRequest().build(); // Or OK if we don't want gateway to retry
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            // Return 500 to indicate processing failure, gateway might retry
            return ResponseEntity.internalServerError().build();
        }
        // If validation fails (signature mismatch), return 400 Bad Request usually.
    }

    @PostMapping("/pay")
    public ResponseEntity<TransactionDto> payForRental(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal Jwt principal) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("Received payment request for rentalId: {} from userId: {}", request.getRentalId(), userId);
        TransactionDto transactionDto = paymentService.payForRental(userId, request);
        return ResponseEntity.ok(transactionDto);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceDto> getMyBalance(@AuthenticationPrincipal Jwt principal) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("Received balance request for userId: {}", userId);
        BalanceDto balanceDto = paymentService.getUserBalance(userId);
        return ResponseEntity.ok(balanceDto);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionDto>> getMyTransactionHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type, // Spring automatically converts String to Enum
            Pageable pageable, // Spring automatically provides pagination info (page, size, sort)
            @AuthenticationPrincipal Jwt principal) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("Received transaction history request for userId: {}", userId);
        Page<TransactionDto> historyPage = paymentService.getTransactionHistory(userId, startDate, endDate, type, pageable);
        return ResponseEntity.ok(historyPage);
    }

    // @ExceptionHandler(ResourceNotFoundException.class)
    // public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
    //     ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getDescription(false));
    //     return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    // }

    // @ExceptionHandler({InvalidOperationException.class, InsufficientBalanceException.class})
    // public ResponseEntity<ErrorResponse> handleBadRequestExceptions(RuntimeException ex, WebRequest request) {
    //     ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getDescription(false));
    //     return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    // }

    // @ExceptionHandler(PaymentProcessingException.class)
    // public ResponseEntity<ErrorResponse> handlePaymentProcessingError(PaymentProcessingException ex, WebRequest request) {
    //     ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage(), request.getDescription(false));
    //     return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    // }

    // Add handler for MethodArgumentNotValidException (for @Valid DTOs) -> 400
    // Add handler for ConstraintViolationException (for @Validated @RequestParam) -> 400
}
