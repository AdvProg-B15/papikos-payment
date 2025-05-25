package id.ac.ui.cs.advprog.papikos.payment.dto;

import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionStatus;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDtoTest {

    private UUID transactionId;
    private UUID userId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private TransactionStatus status;
    private UUID relatedRentalId;
    private UUID payerUserId;
    private UUID payeeUserId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private TransactionDto transactionDto1;
    private TransactionDto transactionDto2; // For equality tests

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        transactionType = TransactionType.PAYMENT;
        amount = new BigDecimal("199.99");
        status = TransactionStatus.COMPLETED;
        relatedRentalId = UUID.randomUUID();
        payerUserId = UUID.randomUUID();
        payeeUserId = UUID.randomUUID();
        notes = "Payment for monthly rental";
        createdAt = LocalDateTime.now().minusDays(1);
        updatedAt = LocalDateTime.now();

        transactionDto1 = new TransactionDto(
                transactionId, userId, transactionType, amount, status,
                relatedRentalId, payerUserId, payeeUserId, notes,
                createdAt, updatedAt
        );
    }

    @Test
    void testRecordCreationAndAccessors() {
        assertNotNull(transactionDto1, "TransactionDto should be created.");
        assertEquals(transactionId, transactionDto1.transactionId());
        assertEquals(userId, transactionDto1.userId());
        assertEquals(transactionType, transactionDto1.transactionType());
        assertEquals(amount, transactionDto1.amount());
        assertEquals(status, transactionDto1.status());
        assertEquals(relatedRentalId, transactionDto1.relatedRentalId());
        assertEquals(payerUserId, transactionDto1.payerUserId());
        assertEquals(payeeUserId, transactionDto1.payeeUserId());
        assertEquals(notes, transactionDto1.notes());
        assertEquals(createdAt, transactionDto1.createdAt());
        assertEquals(updatedAt, transactionDto1.updatedAt());
    }

    @Test
    void testEquals_sameValues() {
        transactionDto2 = new TransactionDto(
                transactionId, userId, transactionType, amount, status,
                relatedRentalId, payerUserId, payeeUserId, notes,
                createdAt, updatedAt
        ); // Same values as transactionDto1
        assertTrue(transactionDto1.equals(transactionDto2), "Records with the same component values should be equal.");
        assertEquals(transactionDto1, transactionDto2, "Records with the same component values should be equal (assertEquals).");
    }

    @Test
    void testEquals_differentTransactionId() {
        transactionDto2 = new TransactionDto(
                UUID.randomUUID(), userId, transactionType, amount, status,
                relatedRentalId, payerUserId, payeeUserId, notes,
                createdAt, updatedAt
        );
        assertFalse(transactionDto1.equals(transactionDto2), "Records with different transactionId should not be equal.");
    }

    @Test
    void testEquals_oneComponentDifferent() {
        // Example: different notes
        transactionDto2 = new TransactionDto(
                transactionId, userId, transactionType, amount, status,
                relatedRentalId, payerUserId, payeeUserId, "Different notes", // Changed notes
                createdAt, updatedAt
        );
        assertFalse(transactionDto1.equals(transactionDto2), "Records with different notes should not be equal.");
    }


    @Test
    void testEquals_nullObject() {
        assertFalse(transactionDto1.equals(null));
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(transactionDto1.equals(new Object()));
    }

    @Test
    void testHashCode_consistentWithEquals() {
        transactionDto2 = new TransactionDto(
                transactionId, userId, transactionType, amount, status,
                relatedRentalId, payerUserId, payeeUserId, notes,
                createdAt, updatedAt
        ); // Equal to transactionDto1
        assertEquals(transactionDto1.hashCode(), transactionDto2.hashCode(), "Hash codes should be the same for equal records.");
    }

    @Test
    void testHashCode_differentForDifferentRecords() {
        transactionDto2 = new TransactionDto(
                UUID.randomUUID(), userId, transactionType, amount, status, // Different transactionId
                relatedRentalId, payerUserId, payeeUserId, notes,
                createdAt, updatedAt
        );
        assertNotEquals(transactionDto1.hashCode(), transactionDto2.hashCode(), "Hash codes should generally be different for unequal records.");
    }

    @Test
    void testToString() {
        String stringRepresentation = transactionDto1.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.contains("transactionId=" + transactionId));
        assertTrue(stringRepresentation.contains("userId=" + userId));
        assertTrue(stringRepresentation.contains("transactionType=" + transactionType));
        assertTrue(stringRepresentation.contains("amount=" + amount));
        assertTrue(stringRepresentation.contains("status=" + status));
        assertTrue(stringRepresentation.contains("relatedRentalId=" + relatedRentalId));
        assertTrue(stringRepresentation.contains("payerUserId=" + payerUserId));
        assertTrue(stringRepresentation.contains("payeeUserId=" + payeeUserId));
        assertTrue(stringRepresentation.contains("notes=" + notes));
        assertTrue(stringRepresentation.contains("createdAt=" + createdAt));
        assertTrue(stringRepresentation.contains("updatedAt=" + updatedAt));

        String expectedFormat = String.format("TransactionDto[transactionId=%s, userId=%s, transactionType=%s, amount=%s, status=%s, relatedRentalId=%s, payerUserId=%s, payeeUserId=%s, notes=%s, createdAt=%s, updatedAt=%s]",
                transactionId, userId, transactionType, amount, status, relatedRentalId, payerUserId, payeeUserId, notes, createdAt, updatedAt);
        assertEquals(expectedFormat, stringRepresentation, "toString format should match the expected record format.");
    }

    @Test
    void testRecordWithNullOptionalFields() {
        // Fields like relatedRentalId, payerUserId, payeeUserId, notes can be null
        TransactionDto dtoWithNulls = new TransactionDto(
                transactionId, userId, transactionType, amount, status,
                null, null, null, null, // Nullable UUIDs and String
                createdAt, updatedAt
        );
        assertNull(dtoWithNulls.relatedRentalId());
        assertNull(dtoWithNulls.payerUserId());
        assertNull(dtoWithNulls.payeeUserId());
        assertNull(dtoWithNulls.notes());

        // Test equality
        TransactionDto anotherDtoWithNulls = new TransactionDto(
                transactionId, userId, transactionType, amount, status,
                null, null, null, null,
                createdAt, updatedAt
        );
        assertEquals(dtoWithNulls, anotherDtoWithNulls);
        assertEquals(dtoWithNulls.hashCode(), anotherDtoWithNulls.hashCode());

        // Test toString with nulls
        String s = dtoWithNulls.toString();
        assertTrue(s.contains("relatedRentalId=null"));
        assertTrue(s.contains("payerUserId=null"));
        assertTrue(s.contains("payeeUserId=null"));
        assertTrue(s.contains("notes=null"));
    }
}