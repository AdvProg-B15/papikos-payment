package id.ac.ui.cs.advprog.papikos.payment.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTypeTest {

    @Test
    void testEnumValuesExist() {
        // Check if all expected enum constants exist and can be accessed
        assertNotNull(TransactionType.TOPUP);
        assertNotNull(TransactionType.PAYMENT);
        assertNotNull(TransactionType.WITHDRAWAL);
        assertNotNull(TransactionType.REFUND);
    }

    @Test
    void testValuesMethod() {
        // Test the values() method which returns an array of all enum constants
        TransactionType[] types = TransactionType.values();
        assertEquals(4, types.length, "There should be 4 transaction types.");
        assertArrayEquals(
                new TransactionType[]{
                        TransactionType.TOPUP,
                        TransactionType.PAYMENT,
                        TransactionType.WITHDRAWAL,
                        TransactionType.REFUND
                },
                types,
                "The values() method should return all types in declaration order."
        );
    }

    @Test
    void testValueOfMethod_validNames() {
        // Test valueOf() with valid names
        assertEquals(TransactionType.TOPUP, TransactionType.valueOf("TOPUP"));
        assertEquals(TransactionType.PAYMENT, TransactionType.valueOf("PAYMENT"));
        assertEquals(TransactionType.WITHDRAWAL, TransactionType.valueOf("WITHDRAWAL"));
        assertEquals(TransactionType.REFUND, TransactionType.valueOf("REFUND"));
    }

    @Test
    void testValueOfMethod_invalidName() {
        // Test valueOf() with an invalid name - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            TransactionType.valueOf("INVALID_TYPE");
        }, "valueOf() should throw IllegalArgumentException for an invalid enum constant name.");
    }

    @Test
    void testValueOfMethod_nullName() {
        // Test valueOf() with null - should throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            TransactionType.valueOf(null);
        }, "valueOf() should throw NullPointerException when a null name is provided.");
    }

    @Test
    void testNameMethod() {
        // Test the name() method for each constant
        assertEquals("TOPUP", TransactionType.TOPUP.name());
        assertEquals("PAYMENT", TransactionType.PAYMENT.name());
        assertEquals("WITHDRAWAL", TransactionType.WITHDRAWAL.name());
        assertEquals("REFUND", TransactionType.REFUND.name());
    }

    @Test
    void testOrdinalMethod() {
        // Test the ordinal() method (order of declaration, starting from 0)
        assertEquals(0, TransactionType.TOPUP.ordinal());
        assertEquals(1, TransactionType.PAYMENT.ordinal());
        assertEquals(2, TransactionType.WITHDRAWAL.ordinal());
        assertEquals(3, TransactionType.REFUND.ordinal());
    }
}