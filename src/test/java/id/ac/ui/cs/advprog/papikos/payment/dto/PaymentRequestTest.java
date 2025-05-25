package id.ac.ui.cs.advprog.papikos.payment.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestTest {

    private UUID rentalId;
    private BigDecimal amount;
    private PaymentRequest paymentRequest1;
    private PaymentRequest paymentRequest2; // For equality tests

    @BeforeEach
    void setUp() {
        rentalId = UUID.randomUUID();
        amount = new BigDecimal("250.75");

        paymentRequest1 = new PaymentRequest(rentalId, amount);
    }

    @Test
    void testRecordCreationAndAccessors() {
        assertNotNull(paymentRequest1, "PaymentRequest should be created.");
        assertEquals(rentalId, paymentRequest1.rentalId(), "rentalId accessor should return the correct value.");
        assertEquals(amount, paymentRequest1.amount(), "amount accessor should return the correct value.");
    }

    @Test
    void testEquals_sameValues() {
        paymentRequest2 = new PaymentRequest(rentalId, amount); // Same values
        assertTrue(paymentRequest1.equals(paymentRequest2), "Two PaymentRequest records with the same values should be equal.");
        assertEquals(paymentRequest1, paymentRequest2, "Two PaymentRequest records with the same values should be equal (assertEquals).");
    }

    @Test
    void testEquals_differentRentalId() {
        paymentRequest2 = new PaymentRequest(UUID.randomUUID(), amount);
        assertFalse(paymentRequest1.equals(paymentRequest2), "Records with different rentalId should not be equal.");
    }

    @Test
    void testEquals_differentAmount() {
        paymentRequest2 = new PaymentRequest(rentalId, new BigDecimal("99.00"));
        assertFalse(paymentRequest1.equals(paymentRequest2), "Records with different amount should not be equal.");
    }

    @Test
    void testEquals_nullObject() {
        assertFalse(paymentRequest1.equals(null));
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(paymentRequest1.equals(new Object()));
    }

    @Test
    void testHashCode_consistentWithEquals() {
        paymentRequest2 = new PaymentRequest(rentalId, amount); // Equal to paymentRequest1
        assertEquals(paymentRequest1.hashCode(), paymentRequest2.hashCode(), "Hash codes should be the same for equal records.");
    }

    @Test
    void testHashCode_differentForDifferentRecords() {
        paymentRequest2 = new PaymentRequest(UUID.randomUUID(), amount); // Different rentalId
        assertNotEquals(paymentRequest1.hashCode(), paymentRequest2.hashCode(), "Hash codes should generally be different for unequal records.");
    }

    @Test
    void testToString() {
        String stringRepresentation = paymentRequest1.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.contains("rentalId=" + rentalId));
        assertTrue(stringRepresentation.contains("amount=" + amount));
        assertEquals(String.format("PaymentRequest[rentalId=%s, amount=%s]", rentalId, amount),
                stringRepresentation, "toString format should match the expected record format.");
    }
}