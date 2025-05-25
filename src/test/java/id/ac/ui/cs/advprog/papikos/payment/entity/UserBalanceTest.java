package id.ac.ui.cs.advprog.papikos.payment.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserBalanceTest {

    private UserBalance userBalance1;
    private UserBalance userBalance2;

    private UUID userId1;
    private UUID userId2;
    private BigDecimal initialBalance;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        initialBalance = new BigDecimal("1000.00");
        updatedAt = LocalDateTime.now();

        // Use the custom constructor for initial setup
        userBalance1 = new UserBalance(userId1, initialBalance);
        userBalance1.setUpdatedAt(updatedAt); // Manually set for testing

        userBalance2 = new UserBalance(); // For testing no-args constructor and setters
    }

    @Test
    void testNoArgsConstructor() {
        UserBalance newUserBalance = new UserBalance();
        assertNotNull(newUserBalance, "UserBalance should be creatable with no-args constructor.");
        assertNull(newUserBalance.getUserId(), "userId should be null initially for no-args constructor.");
        assertEquals(BigDecimal.ZERO, newUserBalance.getBalance(), "Default balance should be ZERO for no-args constructor.");
    }

    @Test
    void testCustomConstructor_withValidBalance() {
        BigDecimal positiveBalance = new BigDecimal("500.75");
        UserBalance ub = new UserBalance(userId1, positiveBalance);

        assertNotNull(ub);
        assertEquals(userId1, ub.getUserId());
        assertEquals(positiveBalance, ub.getBalance());
        // updatedAt would be null initially until persisted/updated by JPA
        assertNull(ub.getUpdatedAt());
    }

    @Test
    void testCustomConstructor_withNegativeBalance_shouldSetToZero() {
        BigDecimal negativeBalance = new BigDecimal("-100.00");
        UserBalance ub = new UserBalance(userId1, negativeBalance);

        assertNotNull(ub);
        assertEquals(userId1, ub.getUserId());
        assertEquals(BigDecimal.ZERO, ub.getBalance(), "Negative initial balance should default to ZERO.");
    }

    @Test
    void testCustomConstructor_withNullBalance_shouldSetToZero() {
        UserBalance ub = new UserBalance(userId1, null);

        assertNotNull(ub);
        assertEquals(userId1, ub.getUserId());
        assertEquals(BigDecimal.ZERO, ub.getBalance(), "Null initial balance should default to ZERO.");
    }

    @Test
    void testGettersAndSetters() {
        // Test getters for userBalance1 initialized in setUp
        assertEquals(userId1, userBalance1.getUserId());
        assertEquals(initialBalance, userBalance1.getBalance());
        assertEquals(updatedAt, userBalance1.getUpdatedAt());

        // Test setters on userBalance2
        userBalance2.setUserId(userId2);
        BigDecimal newBalance = new BigDecimal("250.50");
        userBalance2.setBalance(newBalance);
        LocalDateTime newUpdatedAt = LocalDateTime.now().plusHours(1);
        userBalance2.setUpdatedAt(newUpdatedAt);

        assertEquals(userId2, userBalance2.getUserId());
        assertEquals(newBalance, userBalance2.getBalance());
        assertEquals(newUpdatedAt, userBalance2.getUpdatedAt());
    }

    @Test
    void testEquals_sameObject() {
        assertTrue(userBalance1.equals(userBalance1), "An object should be equal to itself.");
    }

    @Test
    void testEquals_nullObject() {
        assertFalse(userBalance1.equals(null), "An object should not be equal to null.");
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(userBalance1.equals(new Object()), "An object should not be equal to an object of a different class.");
    }

    @Test
    void testEquals_sameUserId() {
        userBalance2 = new UserBalance(userId1, new BigDecimal("200.00")); // Same userId, different balance
        assertTrue(userBalance1.equals(userBalance2), "UserBalances with the same userId should be equal.");
    }

    @Test
    void testEquals_differentUserId() {
        userBalance2 = new UserBalance(userId2, initialBalance); // Different userId
        assertFalse(userBalance1.equals(userBalance2), "UserBalances with different userIds should not be equal.");
    }

    @Test
    void testEquals_oneUserIdNull() {
        UserBalance ubWithNullId = new UserBalance(); // userId is null
        ubWithNullId.setBalance(BigDecimal.TEN);

        assertFalse(userBalance1.equals(ubWithNullId), "Equality check should handle one userId being null.");
        assertFalse(ubWithNullId.equals(userBalance1), "Equality check should be symmetric when one userId is null.");
    }

    @Test
    void testEquals_bothUserIdsNull() {
        UserBalance ub1NullId = new UserBalance();
        UserBalance ub2NullId = new UserBalance();
        assertTrue(ub1NullId.equals(ub2NullId), "UserBalances with both userIds null should be equal based on Objects.equals(null, null).");
    }


    @Test
    void testHashCode_consistentWithEquals() {
        userBalance2 = new UserBalance(userId1, new BigDecimal("200.00")); // Makes userBalance2 equal to userBalance1
        assertEquals(userBalance1.hashCode(), userBalance2.hashCode(), "Hash code should be the same for equal objects.");
    }

    @Test
    void testHashCode_differentForDifferentUserId() {
        userBalance2 = new UserBalance(userId2, initialBalance);
        assertNotEquals(userBalance1.hashCode(), userBalance2.hashCode(), "Hash code should be different for objects with different userIds (unless hash collision).");
    }

    @Test
    void testHashCode_forNullUserId() {
        UserBalance ubNullId = new UserBalance(); // userId is null
        assertEquals(Objects.hash((Object) null), ubNullId.hashCode(), "Hash code for null userId should be Objects.hash(null).");
    }


    @Test
    void testToString() {
        String stringRepresentation = userBalance1.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.startsWith("UserBalance{"), "toString should start with 'UserBalance{'.");
        assertTrue(stringRepresentation.contains("userId=" + userId1), "toString should contain userId.");
        assertTrue(stringRepresentation.contains("balance=" + initialBalance), "toString should contain balance.");
        assertTrue(stringRepresentation.contains("updatedAt=" + updatedAt), "toString should contain updatedAt.");
        assertTrue(stringRepresentation.endsWith("}"), "toString should end with '}'.");
    }

    @Test
    void testToString_withNullUpdatedAt() {
        UserBalance ub = new UserBalance(userId1, BigDecimal.TEN); // updatedAt will be null
        String stringRepresentation = ub.toString();
        assertTrue(stringRepresentation.contains("updatedAt=null"), "toString should show null for updatedAt if not set.");
    }
}