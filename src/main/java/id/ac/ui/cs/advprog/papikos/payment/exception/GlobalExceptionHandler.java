package id.ac.ui.cs.advprog.papikos.payment.exception;

import id.ac.ui.cs.advprog.papikos.payment.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation; // Use Jakarta validation
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Payment Service.
 * Catches defined exceptions and returns standardized error responses.
 */
@ControllerAdvice // Makes this class intercept exceptions across all @Controllers
@Slf4j
public class GlobalExceptionHandler {

    // Inner class or separate DTO for the error response structure
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}

    // Handler for Resource Not Found errors (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiResponse<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).substring(4) // Extract path typically
        );
        return ApiResponse.<ErrorResponse>builder()
                .status(HttpStatus.NOT_FOUND)
                .message("Resource not found")
                .data(errorResponse)
                .build();
    }

    // Handler for Bad Request errors (400) like invalid operations or insufficient funds
    @ExceptionHandler({InvalidOperationException.class, InsufficientBalanceException.class})
    public ApiResponse<ErrorResponse> handleBadRequestBusinessExceptions(
            RuntimeException ex, WebRequest request) {
        log.warn("Bad request condition: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).substring(4)
        );
        return ApiResponse.<ErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Bad request")
                .data(errorResponse)
                .build();
    }

    // Handler for Validation errors on Request Body DTOs (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        String validationErrorMessage = "Validation failed: " + errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        log.warn("Validation error (DTO): {}", validationErrorMessage);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                validationErrorMessage,
                request.getDescription(false).substring(4)
        );
        return ApiResponse.<ErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Validation error")
                .data(errorResponse)
                .build();
    }

    // Handler for Validation errors on Request Parameters (@RequestParam, @PathVariable with @Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        String violationMessages = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        String constraintErrorMessage = "Constraint violation: " + violationMessages;
        log.warn("Validation error (Constraint): {}", constraintErrorMessage);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                constraintErrorMessage,
                request.getDescription(false).substring(4)
        );
        return ApiResponse.<ErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Constraint violation")
                .data(errorResponse)
                .build();
    }


    // Handler for internal payment processing issues (500)
    @ExceptionHandler(PaymentProcessingException.class)
    public ApiResponse<ErrorResponse> handlePaymentProcessingException(
            PaymentProcessingException ex, WebRequest request) {
        log.error("Internal payment processing error: {}", ex.getMessage(), ex); // Log stack trace for internal errors
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An internal error occurred during payment processing. Please try again later.", // Generic message to client
                request.getDescription(false).substring(4)
        );
        return ApiResponse.<ErrorResponse>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Internal server error")
                .data(errorResponse)
                .build();
    }

    // --- Optional: Generic Fallback Handler ---
    // Catches any other unexpected exceptions (500)
    @ExceptionHandler(Exception.class)
    public ApiResponse<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex); // Log stack trace
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal error occurred.", // Keep it generic
                request.getDescription(false).substring(4)
        );
        return ApiResponse.<ErrorResponse>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Internal server error")
                .data(errorResponse)
                .build();
    }
}
