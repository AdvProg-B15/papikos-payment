package id.ac.ui.cs.advprog.papikos.payment.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessingExceptionTest {

    private static final String TEST_MESSAGE = "An error occurred during payment processing.";
    private static final String कॉज_MESSAGE = "Underlying database error."; // "cause" in Hindi to ensure it's a distinct string

    @Test
    void testExceptionCreationWithMessageOnly() {
        // Act
        PaymentProcessingException exception = new PaymentProcessingException(TEST_MESSAGE);

        // Assert
        assertNotNull(exception, "Exception should be created with message only.");
        assertEquals(TEST_MESSAGE, exception.getMessage(), "The message should match the one provided.");
        assertNull(exception.getCause(), "The cause should be null when only message is provided.");
    }

    @Test
    void testExceptionCreationWithMessageAndCause() {
        // Arrange
        Throwable cause = new RuntimeException(कॉज_MESSAGE);

        // Act
        PaymentProcessingException exception = new PaymentProcessingException(TEST_MESSAGE, cause);

        // Assert
        assertNotNull(exception, "Exception should be created with message and cause.");
        assertEquals(TEST_MESSAGE, exception.getMessage(), "The message should match the one provided.");
        assertNotNull(exception.getCause(), "The cause should not be null.");
        assertSame(cause, exception.getCause(), "The cause should be the same object that was passed in.");
        assertEquals(कॉज_MESSAGE, exception.getCause().getMessage(), "The cause's message should be retrievable.");
    }

    @Test
    void testExceptionIsRuntimeException() {
        // Act
        PaymentProcessingException exceptionWithMessageOnly = new PaymentProcessingException(TEST_MESSAGE);
        PaymentProcessingException exceptionWithMessageAndCause = new PaymentProcessingException(TEST_MESSAGE, new Throwable());

        // Assert
        assertInstanceOf(RuntimeException.class, exceptionWithMessageOnly, "Exception (message only) should be a RuntimeException.");
        assertInstanceOf(RuntimeException.class, exceptionWithMessageAndCause, "Exception (message and cause) should be a RuntimeException.");
    }

    @Test
    void testExceptionCreationWithNullMessageAndNoCause() {
        // Act
        PaymentProcessingException exception = new PaymentProcessingException(null);

        // Assert
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionCreationWithNullMessageAndCause() {
        // Arrange
        Throwable cause = new RuntimeException(कॉज_MESSAGE);

        // Act
        PaymentProcessingException exception = new PaymentProcessingException(null, cause);

        // Assert
        assertNotNull(exception);
        assertNull(exception.getMessage()); // RuntimeException constructor with (String, Throwable) allows null message
        assertSame(cause, exception.getCause());
    }

    @Test
    void testExceptionCreationWithMessageAndNullCause() {
        // Act
        PaymentProcessingException exception = new PaymentProcessingException(TEST_MESSAGE, null);

        // Assert
        assertNotNull(exception);
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getCause()); // RuntimeException constructor with (String, Throwable) allows null cause
    }
}