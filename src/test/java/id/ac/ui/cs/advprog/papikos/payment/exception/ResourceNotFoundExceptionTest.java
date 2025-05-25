package id.ac.ui.cs.advprog.papikos.payment.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    private static final String TEST_MESSAGE = "The requested resource could not be found.";

    @Test
    void testExceptionCreationWithMessage() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(TEST_MESSAGE);

        // Assert
        assertNotNull(exception, "Exception should be created.");
        assertEquals(TEST_MESSAGE, exception.getMessage(), "The exception message should match the one provided in the constructor.");
    }

    @Test
    void testExceptionIsRuntimeException() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(TEST_MESSAGE);

        // Assert
        // This checks if the exception is an instance of RuntimeException
        assertInstanceOf(RuntimeException.class, exception, "ResourceNotFoundException should be a RuntimeException.");
    }

    @Test
    void testExceptionCreationWithNullMessage() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(null);

        // Assert
        assertNotNull(exception, "Exception should be created even with a null message.");
        assertNull(exception.getMessage(), "The exception message should be null if null was passed to the constructor.");
    }

    @Test
    void testExceptionCreationWithEmptyMessage() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException("");

        // Assert
        assertNotNull(exception, "Exception should be created with an empty message.");
        assertEquals("", exception.getMessage(), "The exception message should be empty if an empty string was passed.");
    }
}