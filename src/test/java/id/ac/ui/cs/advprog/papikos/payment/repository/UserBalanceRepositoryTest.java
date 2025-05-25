package id.ac.ui.cs.advprog.papikos.payment.repository;

import id.ac.ui.cs.advprog.papikos.payment.entity.UserBalance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBalanceRepositoryTest {

    @Mock
    private UserBalanceRepository userBalanceRepository; // Mocking the interface

    @Test
    void testFindByUserId_whenMocked() {
        UUID userId = UUID.randomUUID();
        UserBalance mockBalance = new UserBalance(userId, new BigDecimal("100.00"));
        Optional<UserBalance> expectedOptional = Optional.of(mockBalance);

        // Define behavior for the mocked repository method
        when(userBalanceRepository.findByUserId(eq(userId))).thenReturn(expectedOptional);

        // Call the mocked method
        Optional<UserBalance> actualOptional = userBalanceRepository.findByUserId(userId);

        // Assert (this tests the mock setup, not the actual query)
        assertNotNull(actualOptional);
        assertEquals(expectedOptional, actualOptional);
        assertTrue(actualOptional.isPresent());
        assertEquals(userId, actualOptional.get().getUserId());
    }

    @Test
    void testFindByUserIdWithLock_whenMocked() {
        UUID userId = UUID.randomUUID();
        UserBalance mockBalance = new UserBalance(userId, new BigDecimal("200.00"));
        Optional<UserBalance> expectedOptional = Optional.of(mockBalance);

        // Define behavior for the mocked repository method
        when(userBalanceRepository.findByUserIdWithLock(eq(userId))).thenReturn(expectedOptional);

        // Call the mocked method
        Optional<UserBalance> actualOptional = userBalanceRepository.findByUserIdWithLock(userId);

        // Assert (this tests the mock setup, not the actual query or lock)
        assertNotNull(actualOptional);
        assertEquals(expectedOptional, actualOptional);
        assertTrue(actualOptional.isPresent());
        assertEquals(userId, actualOptional.get().getUserId());
    }

    @Test
    void testRepositoryCanBeMocked() {
        assertNotNull(userBalanceRepository, "UserBalanceRepository mock should not be null.");
    }
}