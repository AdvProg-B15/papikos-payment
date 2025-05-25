package id.ac.ui.cs.advprog.papikos.payment.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest mockWebRequest;

    private final String MOCK_REQUEST_PATH = "uri=/test/path"; // WebRequest.getDescription(false) often returns this format

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        mockWebRequest = mock(WebRequest.class);
        when(mockWebRequest.getDescription(false)).thenReturn(MOCK_REQUEST_PATH);
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Test resource not found");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleResourceNotFoundException(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.status());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), errorResponse.error());
        assertEquals("Test resource not found", errorResponse.message());
        assertEquals("/test/path", errorResponse.path()); // "uri=" prefix removed
    }

    @Test
    void testHandleInvalidOperationException() {
        InvalidOperationException ex = new InvalidOperationException("Test invalid operation");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleBadRequestBusinessExceptions(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), errorResponse.error());
        assertEquals("Test invalid operation", errorResponse.message());
        assertEquals("/test/path", errorResponse.path());
    }

    @Test
    void testHandleInsufficientBalanceException() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Test insufficient balance");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleBadRequestBusinessExceptions(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), errorResponse.error());
        assertEquals("Test insufficient balance", errorResponse.message());
        assertEquals("/test/path", errorResponse.path());
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        // Mock MethodArgumentNotValidException and its dependencies
        BindingResult mockBindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("objectName", "fieldName", "must not be null");
        when(mockBindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        // MethodParameter can be null for this test's purpose if not deeply inspected by the handler
        MethodParameter mockMethodParameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, mockBindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleValidationExceptions(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), errorResponse.error());
        assertTrue(errorResponse.message().contains("Validation failed: fieldName: must not be null"));
        assertEquals("/test/path", errorResponse.path());
    }

    @Test
    void testHandleConstraintViolationException() {
        // Mock ConstraintViolationException and its dependencies
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
        when(mockViolation.getMessage()).thenReturn("must be positive");
        violations.add(mockViolation);

        ConstraintViolationException ex = new ConstraintViolationException("Constraint violation occurred", violations);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleConstraintViolationException(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), errorResponse.error());
        assertTrue(errorResponse.message().contains("Constraint violation: must be positive"));
        assertEquals("/test/path", errorResponse.path());
    }

    @Test
    void testHandleConstraintViolationExceptionWithNoViolations() {
        // Test with an empty set of violations
        ConstraintViolationException ex = new ConstraintViolationException("No violations", Collections.emptySet());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleConstraintViolationException(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Constraint violation: ", errorResponse.message()); // Message will be "Constraint violation: "
        assertEquals("/test/path", errorResponse.path());
    }


    @Test
    void testHandlePaymentProcessingException() {
        PaymentProcessingException ex = new PaymentProcessingException("Test payment processing error");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handlePaymentProcessingException(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.status());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), errorResponse.error());
        assertEquals("An internal error occurred during payment processing. Please try again later.", errorResponse.message());
        assertEquals("/test/path", errorResponse.path());
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new Exception("Test generic unexpected error");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleGenericException(ex, mockWebRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.status());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), errorResponse.error());
        assertEquals("An unexpected internal error occurred.", errorResponse.message());
        assertEquals("/test/path", errorResponse.path());
    }

    @Test
    void testErrorResponseRecord() {
        // Simple test for the record itself, though it's implicitly tested elsewhere
        LocalDateTime now = LocalDateTime.now();
        GlobalExceptionHandler.ErrorResponse errorResponse = new GlobalExceptionHandler.ErrorResponse(
                now, 400, "Bad Request", "Test message", "/test"
        );
        assertEquals(now, errorResponse.timestamp());
        assertEquals(400, errorResponse.status());
        assertEquals("Bad Request", errorResponse.error());
        assertEquals("Test message", errorResponse.message());
        assertEquals("/test", errorResponse.path());
    }
}