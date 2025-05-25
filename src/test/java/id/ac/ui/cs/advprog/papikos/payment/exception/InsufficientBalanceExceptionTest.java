package id.ac.ui.cs.advprog.papikos.payment.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsufficientBalanceExceptionTest {

    private static final String TEST_MESSAGE = "Account does not have enough funds for this transaction.";

    @Test
    void testExceptionCreationWithMessage() {
        // Act
        InsufficientBalanceException exception = new InsufficientBalanceException(TEST_MESSAGE);

        // Assert
        assertNotNull(exception, "Exception should be created.");
        assertEquals(TEST_MESSAGE, exception.getMessage(), "The exception message should match the one provided in the constructor.");
    }

    @Test
    void testExceptionIsRuntimeException() {
        // Act
        InsufficientBalanceException exception = new InsufficientBalanceException(TEST_MESSAGE);

        // Assert
        // This checks if the exception is an instance of RuntimeException
        assertInstanceOf(RuntimeException.class, exception, "InsufficientBalanceException should be a RuntimeException.");
    }

    @Test
    void testExceptionCreationWithNullMessage() {
        // Act
        InsufficientBalanceException exception = new InsufficientBalanceException(null);

        // Assert
        assertNotNull(exception, "Exception should be created even with a null message.");
        assertNull(exception.getMessage(), "The exception message should be null if null was passed to the constructor.");
    }

    @Test
    void testExceptionCreationWithEmptyMessage() {
        // Act
        InsufficientBalanceException exception = new InsufficientBalanceException("");

        // Assert
        assertNotNull(exception, "Exception should be created with an empty message.");
        assertEquals("", exception.getMessage(), "The exception message should be empty if an empty string was passed.");
    }
}