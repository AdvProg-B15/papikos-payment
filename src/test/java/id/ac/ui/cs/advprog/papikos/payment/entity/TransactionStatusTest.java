package id.ac.ui.cs.advprog.papikos.payment.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStatusTest {

    @Test
    void testEnumValuesExist() {
        // Check if all expected enum constants exist and can be accessed
        assertNotNull(TransactionStatus.PENDING);
        assertNotNull(TransactionStatus.COMPLETED);
        assertNotNull(TransactionStatus.FAILED);
        assertNotNull(TransactionStatus.CANCELLED);
    }

    @Test
    void testValuesMethod() {
        // Test the values() method which returns an array of all enum constants
        TransactionStatus[] statuses = TransactionStatus.values();
        assertEquals(4, statuses.length, "There should be 4 transaction statuses.");
        assertArrayEquals(
                new TransactionStatus[]{
                        TransactionStatus.PENDING,
                        TransactionStatus.COMPLETED,
                        TransactionStatus.FAILED,
                        TransactionStatus.CANCELLED
                },
                statuses,
                "The values() method should return all statuses in declaration order."
        );
    }

    @Test
    void testValueOfMethod_validNames() {
        // Test valueOf() with valid names
        assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
        assertEquals(TransactionStatus.COMPLETED, TransactionStatus.valueOf("COMPLETED"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
        assertEquals(TransactionStatus.CANCELLED, TransactionStatus.valueOf("CANCELLED"));
    }

    @Test
    void testValueOfMethod_invalidName() {
        // Test valueOf() with an invalid name - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            TransactionStatus.valueOf("INVALID_STATUS");
        }, "valueOf() should throw IllegalArgumentException for an invalid enum constant name.");
    }

    @Test
    void testValueOfMethod_nullName() {
        // Test valueOf() with null - should throw NullPointerException (as per Enum.valueOf spec)
        assertThrows(NullPointerException.class, () -> {
            TransactionStatus.valueOf(null);
        }, "valueOf() should throw NullPointerException when a null name is provided.");
    }

    @Test
    void testNameMethod() {
        // Test the name() method for each constant
        assertEquals("PENDING", TransactionStatus.PENDING.name());
        assertEquals("COMPLETED", TransactionStatus.COMPLETED.name());
        assertEquals("FAILED", TransactionStatus.FAILED.name());
        assertEquals("CANCELLED", TransactionStatus.CANCELLED.name());
    }

    @Test
    void testOrdinalMethod() {
        // Test the ordinal() method (order of declaration, starting from 0)
        assertEquals(0, TransactionStatus.PENDING.ordinal());
        assertEquals(1, TransactionStatus.COMPLETED.ordinal());
        assertEquals(2, TransactionStatus.FAILED.ordinal());
        assertEquals(3, TransactionStatus.CANCELLED.ordinal());
    }
}