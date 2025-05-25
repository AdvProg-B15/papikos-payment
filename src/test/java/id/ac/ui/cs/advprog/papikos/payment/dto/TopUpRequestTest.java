package id.ac.ui.cs.advprog.papikos.payment.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TopUpRequestTest {

    private BigDecimal amount;
    private TopUpRequest topUpRequest1;
    private TopUpRequest topUpRequest2; // For equality tests

    @BeforeEach
    void setUp() {
        amount = new BigDecimal("50.00");
        topUpRequest1 = new TopUpRequest(amount);
    }

    @Test
    void testRecordCreationAndAccessor() {
        assertNotNull(topUpRequest1, "TopUpRequest should be created.");
        assertEquals(amount, topUpRequest1.amount(), "amount accessor should return the correct value.");
    }

    @Test
    void testEquals_sameValue() {
        topUpRequest2 = new TopUpRequest(amount); // Same value
        assertTrue(topUpRequest1.equals(topUpRequest2), "Two TopUpRequest records with the same amount should be equal.");
        assertEquals(topUpRequest1, topUpRequest2, "Two TopUpRequest records with the same amount should be equal (assertEquals).");
    }

    @Test
    void testEquals_differentAmount() {
        topUpRequest2 = new TopUpRequest(new BigDecimal("99.99"));
        assertFalse(topUpRequest1.equals(topUpRequest2), "Records with different amounts should not be equal.");
    }

    @Test
    void testEquals_nullObject() {
        assertFalse(topUpRequest1.equals(null));
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(topUpRequest1.equals(new Object()));
    }

    @Test
    void testHashCode_consistentWithEquals() {
        topUpRequest2 = new TopUpRequest(amount); // Equal to topUpRequest1
        assertEquals(topUpRequest1.hashCode(), topUpRequest2.hashCode(), "Hash codes should be the same for equal records.");
    }

    @Test
    void testHashCode_differentForDifferentRecords() {
        topUpRequest2 = new TopUpRequest(new BigDecimal("123.45")); // Different amount
        assertNotEquals(topUpRequest1.hashCode(), topUpRequest2.hashCode(), "Hash codes should generally be different for unequal records.");
    }

    @Test
    void testToString() {
        String stringRepresentation = topUpRequest1.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.contains("amount=" + amount));
        assertEquals(String.format("TopUpRequest[amount=%s]", amount),
                stringRepresentation, "toString format should match the expected record format.");
    }
}