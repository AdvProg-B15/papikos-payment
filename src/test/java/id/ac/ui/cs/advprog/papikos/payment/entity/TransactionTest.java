package id.ac.ui.cs.advprog.papikos.payment.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private Transaction transaction1;
    private Transaction transaction2;

    private UUID transactionId1;
    private UUID transactionId2;
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

    @BeforeEach
    void setUp() {
        transaction1 = new Transaction();
        transaction2 = new Transaction();

        // Initialize common values
        transactionId1 = UUID.randomUUID();
        transactionId2 = UUID.randomUUID();
        userId = UUID.randomUUID();
        transactionType = TransactionType.TOPUP;
        amount = new BigDecimal("100.50");
        status = TransactionStatus.COMPLETED;
        relatedRentalId = UUID.randomUUID();
        payerUserId = UUID.randomUUID();
        payeeUserId = UUID.randomUUID();
        notes = "Test transaction notes";
        createdAt = LocalDateTime.now().minusDays(1);
        updatedAt = LocalDateTime.now();

        // Setup transaction1 with some values
        transaction1.setTransactionId(transactionId1);
        transaction1.setUserId(userId);
        transaction1.setTransactionType(transactionType);
        transaction1.setAmount(amount);
        transaction1.setStatus(status);
        transaction1.setRelatedRentalId(relatedRentalId);
        transaction1.setPayerUserId(payerUserId);
        transaction1.setPayeeUserId(payeeUserId);
        transaction1.setNotes(notes);
        transaction1.setCreatedAt(createdAt); // Manually set for testing; Hibernate would do this
        transaction1.setUpdatedAt(updatedAt); // Manually set for testing; Hibernate would do this
    }

    @Test
    void testNoArgsConstructor() {
        Transaction newTransaction = new Transaction();
        assertNotNull(newTransaction, "Transaction should be creatable with no-args constructor.");
        assertNull(newTransaction.getTransactionId(), "transactionId should be null initially.");
        assertEquals(TransactionStatus.PENDING, newTransaction.getStatus(), "Default status should be PENDING.");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(transactionId1, transaction1.getTransactionId());
        assertEquals(userId, transaction1.getUserId());
        assertEquals(transactionType, transaction1.getTransactionType());
        assertEquals(amount, transaction1.getAmount());
        assertEquals(status, transaction1.getStatus());
        assertEquals(relatedRentalId, transaction1.getRelatedRentalId());
        assertEquals(payerUserId, transaction1.getPayerUserId());
        assertEquals(payeeUserId, transaction1.getPayeeUserId());
        assertEquals(notes, transaction1.getNotes());
        assertEquals(createdAt, transaction1.getCreatedAt());
        assertEquals(updatedAt, transaction1.getUpdatedAt());

        // Test setting a different status
        transaction1.setStatus(TransactionStatus.FAILED);
        assertEquals(TransactionStatus.FAILED, transaction1.getStatus());
    }

    @Test
    void testEquals_sameObject() {
        assertTrue(transaction1.equals(transaction1), "An object should be equal to itself.");
    }

    @Test
    void testEquals_nullObject() {
        assertFalse(transaction1.equals(null), "An object should not be equal to null.");
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(transaction1.equals(new Object()), "An object should not be equal to an object of a different class.");
    }

    @Test
    void testEquals_sameId() {
        transaction2.setTransactionId(transactionId1); // Same ID as transaction1
        transaction2.setUserId(UUID.randomUUID()); // Different other fields
        assertTrue(transaction1.equals(transaction2), "Transactions with the same transactionId should be equal.");
    }

    @Test
    void testEquals_differentId() {
        transaction2.setTransactionId(transactionId2); // Different ID
        assertFalse(transaction1.equals(transaction2), "Transactions with different transactionIds should not be equal.");
    }

    @Test
    void testEquals_oneIdNull() {
        transaction1.setTransactionId(null);
        transaction2.setTransactionId(transactionId2);
        assertFalse(transaction1.equals(transaction2), "Equality check should handle one transactionId being null.");
        assertFalse(transaction2.equals(transaction1), "Equality check should be symmetric when one transactionId is null.");
    }

    @Test
    void testEquals_bothIdsNull() {
        // Your equals method: return getTransactionId() != null && Objects.equals(getTransactionId(), that.getTransactionId());
        // If both IDs are null, getTransactionId() != null is false, so they are not equal by your current logic.
        // This is a common strategy for JPA entities: only consider them equal if they have non-null and equal IDs.
        transaction1.setTransactionId(null);
        transaction2.setTransactionId(null);
        assertFalse(transaction1.equals(transaction2), "Transactions with both transactionIds null should not be equal based on current equals logic.");
    }

    @Test
    void testHashCode_consistentWithEquals() {
        // Per contract, if two objects are equal, their hashCodes must be the same.
        // Your hashCode() currently returns getClass().hashCode(), which means all Transaction instances
        // (before being persisted and having an ID, or if ID is not part of hashCode) will have the same hashCode.
        // This is a valid, though not highly performant for hash-based collections, strategy.
        // It satisfies the contract "equal objects must have equal hash codes".
        // The converse "if hash codes are equal, objects might or might not be equal" is also true.

        transaction2.setTransactionId(transactionId1); // Makes transaction2 equal to transaction1
        // If equals relies on ID, and hashCode doesn't, this test might not be meaningful
        // Your hashCode() is: return getClass().hashCode();
        // So, all Transaction instances will have the same hash code.
        assertEquals(transaction1.hashCode(), transaction2.hashCode(), "Hash code should be consistent for Transaction class.");

        Transaction transaction3 = new Transaction();
        transaction3.setTransactionId(UUID.randomUUID()); // Different ID
        // Since hashCode() is getClass().hashCode(), even non-equal objects (by ID) will have the same hash code.
        assertEquals(transaction1.hashCode(), transaction3.hashCode(), "All Transaction instances share the same class-based hash code.");
    }

    @Test
    void testHashCode_forDetachedEntity() {
        Transaction newTransaction = new Transaction();
        // Before an ID is set (e.g., before persisting), its hashCode is based on the class.
        assertEquals(Transaction.class.hashCode(), newTransaction.hashCode(), "hashCode for new entity should be class-based.");
    }


    @Test
    void testToString() {
        String stringRepresentation = transaction1.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.startsWith("Transaction{"), "toString should start with 'Transaction{'.");
        assertTrue(stringRepresentation.contains("transactionId=" + transactionId1), "toString should contain transactionId.");
        assertTrue(stringRepresentation.contains("userId=" + userId), "toString should contain userId.");
        assertTrue(stringRepresentation.contains("transactionType=" + transactionType), "toString should contain transactionType.");
        assertTrue(stringRepresentation.contains("amount=" + amount), "toString should contain amount.");
        assertTrue(stringRepresentation.contains("status=" + status), "toString should contain status.");
        assertTrue(stringRepresentation.contains("relatedRentalId=" + relatedRentalId), "toString should contain relatedRentalId.");
        assertTrue(stringRepresentation.contains("payerUserId=" + payerUserId), "toString should contain payerUserId.");
        assertTrue(stringRepresentation.contains("payeeUserId=" + payeeUserId), "toString should contain payeeUserId.");
        assertTrue(stringRepresentation.contains("createdAt=" + createdAt), "toString should contain createdAt.");
        // Notes and updatedAt are not in your current toString() implementation, so we don't check for them.
        assertFalse(stringRepresentation.contains("notes="), "toString currently does not include notes.");
        assertFalse(stringRepresentation.contains("updatedAt="), "toString currently does not include updatedAt.");
        assertTrue(stringRepresentation.endsWith("}"), "toString should end with '}'.");
    }
}