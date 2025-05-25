package id.ac.ui.cs.advprog.papikos.payment.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BalanceDtoTest {

    private UUID userId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
    private BalanceDto balanceDto1;
    private BalanceDto balanceDto2; // For equality tests

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        balance = new BigDecimal("1234.56");
        updatedAt = LocalDateTime.now();

        balanceDto1 = new BalanceDto(userId, balance, updatedAt);
        // balanceDto2 will be created in specific tests as needed
    }

    @Test
    void testRecordCreationAndAccessors() {
        // Assertions are based on the values set in setUp for balanceDto1
        assertNotNull(balanceDto1, "BalanceDto should be created.");
        assertEquals(userId, balanceDto1.userId(), "userId accessor should return the correct value.");
        assertEquals(balance, balanceDto1.balance(), "balance accessor should return the correct value.");
        assertEquals(updatedAt, balanceDto1.updatedAt(), "updatedAt accessor should return the correct value.");
    }

    @Test
    void testEquals_sameValues() {
        balanceDto2 = new BalanceDto(userId, balance, updatedAt); // Same values as balanceDto1
        assertTrue(balanceDto1.equals(balanceDto2), "Two BalanceDto records with the same component values should be equal.");
        assertEquals(balanceDto1, balanceDto2, "Two BalanceDto records with the same component values should be equal (using assertEquals).");
    }

    @Test
    void testEquals_differentUserId() {
        UUID differentUserId = UUID.randomUUID();
        balanceDto2 = new BalanceDto(differentUserId, balance, updatedAt);
        assertFalse(balanceDto1.equals(balanceDto2), "Records with different userId should not be equal.");
    }

    @Test
    void testEquals_differentBalance() {
        BigDecimal differentBalance = new BigDecimal("99.99");
        balanceDto2 = new BalanceDto(userId, differentBalance, updatedAt);
        assertFalse(balanceDto1.equals(balanceDto2), "Records with different balance should not be equal.");
    }

    @Test
    void testEquals_differentUpdatedAt() {
        LocalDateTime differentUpdatedAt = LocalDateTime.now().minusHours(1);
        balanceDto2 = new BalanceDto(userId, balance, differentUpdatedAt);
        assertFalse(balanceDto1.equals(balanceDto2), "Records with different updatedAt should not be equal.");
    }

    @Test
    void testEquals_nullObject() {
        assertFalse(balanceDto1.equals(null), "A record should not be equal to null.");
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(balanceDto1.equals(new Object()), "A record should not be equal to an object of a different class.");
    }

    @Test
    void testHashCode_consistentWithEquals() {
        balanceDto2 = new BalanceDto(userId, balance, updatedAt); // Equal to balanceDto1
        assertEquals(balanceDto1.hashCode(), balanceDto2.hashCode(), "Hash codes should be the same for equal records.");
    }

    @Test
    void testHashCode_differentForDifferentRecords() {
        balanceDto2 = new BalanceDto(UUID.randomUUID(), balance, updatedAt); // Different userId
        assertNotEquals(balanceDto1.hashCode(), balanceDto2.hashCode(), "Hash codes should generally be different for unequal records (unless collision).");
    }

    @Test
    void testToString() {
        String stringRepresentation = balanceDto1.toString();
        assertNotNull(stringRepresentation, "toString should return a non-null string.");
        assertTrue(stringRepresentation.contains("userId=" + userId), "toString should contain userId.");
        assertTrue(stringRepresentation.contains("balance=" + balance), "toString should contain balance.");
        assertTrue(stringRepresentation.contains("updatedAt=" + updatedAt), "toString should contain updatedAt.");
        // Example expected format, though exact format can vary slightly with record toString
        assertEquals(String.format("BalanceDto[userId=%s, balance=%s, updatedAt=%s]", userId, balance, updatedAt),
                stringRepresentation, "toString format should match the expected record format.");
    }

    @Test
    void testRecordWithNullComponents() {
        // Records can have null components if their types allow it.
        // UUID and BigDecimal can be null. LocalDateTime can be null.
        UUID nullUserId = null;
        BigDecimal nullBalance = null;
        LocalDateTime nullUpdatedAt = null;

        BalanceDto dtoWithNulls = new BalanceDto(nullUserId, nullBalance, nullUpdatedAt);

        assertNull(dtoWithNulls.userId());
        assertNull(dtoWithNulls.balance());
        assertNull(dtoWithNulls.updatedAt());

        // Test equality with another instance with nulls
        BalanceDto anotherDtoWithNulls = new BalanceDto(null, null, null);
        assertEquals(dtoWithNulls, anotherDtoWithNulls);
        assertEquals(dtoWithNulls.hashCode(), anotherDtoWithNulls.hashCode());

        // Test toString with nulls
        String s = dtoWithNulls.toString();
        assertTrue(s.contains("userId=null"));
        assertTrue(s.contains("balance=null"));
        assertTrue(s.contains("updatedAt=null"));
    }
}