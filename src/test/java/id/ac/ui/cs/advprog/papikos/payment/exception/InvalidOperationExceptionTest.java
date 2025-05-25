package id.ac.ui.cs.advprog.papikos.payment.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidOperationExceptionTest {

    private static final String TEST_MESSAGE = "The requested operation is not allowed in the current state.";

    @Test
    void testExceptionCreationWithMessage() {
        // Act
        InvalidOperationException exception = new InvalidOperationException(TEST_MESSAGE);

        // Assert
        assertNotNull(exception, "Exception should be created.");
        assertEquals(TEST_MESSAGE, exception.getMessage(), "The exception message should match the one provided in the constructor.");
    }

    @Test
    void testExceptionIsRuntimeException() {
        // Act
        InvalidOperationException exception = new InvalidOperationException(TEST_MESSAGE);

        // Assert
        // This checks if the exception is an instance of RuntimeException
        assertInstanceOf(RuntimeException.class, exception, "InvalidOperationException should be a RuntimeException.");
    }

    @Test
    void testExceptionCreationWithNullMessage() {
        // Act
        InvalidOperationException exception = new InvalidOperationException(null);

        // Assert
        assertNotNull(exception, "Exception should be created even with a null message.");
        assertNull(exception.getMessage(), "The exception message should be null if null was passed to the constructor.");
    }

    @Test
    void testExceptionCreationWithEmptyMessage() {
        // Act
        InvalidOperationException exception = new InvalidOperationException("");

        // Assert
        assertNotNull(exception, "Exception should be created with an empty message.");
        assertEquals("", exception.getMessage(), "The exception message should be empty if an empty string was passed.");
    }
}