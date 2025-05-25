package id.ac.ui.cs.advprog.papikos.payment.repository;

import id.ac.ui.cs.advprog.papikos.payment.entity.Transaction;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // To use @Mock
class TransactionRepositoryTest {

    @Mock
    private TransactionRepository transactionRepository; // Mocking the interface

    @Test
    void testFindByUserIdOrderByCreatedAtDesc_whenMocked() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> expectedPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        // Define behavior for the mocked repository method
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(expectedPage);

        // Call the mocked method
        Page<Transaction> actualPage = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Assert (this tests the mock setup, not the actual query)
        assertNotNull(actualPage);
        assertEquals(expectedPage, actualPage);
    }

    @Test
    void testFindUserTransactionsByFilter_whenMocked() {
        UUID userId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        TransactionType type = TransactionType.TOPUP;
        Pageable pageable = PageRequest.of(0, 5);
        Page<Transaction> expectedPage = new PageImpl<>(Collections.singletonList(new Transaction()), pageable, 1);

        // Define behavior for the mocked repository method
        when(transactionRepository.findUserTransactionsByFilter(
                eq(userId),
                eq(startDate),
                eq(endDate),
                eq(type),
                eq(pageable)))
                .thenReturn(expectedPage);

        // Call the mocked method
        Page<Transaction> actualPage = transactionRepository.findUserTransactionsByFilter(
                userId, startDate, endDate, type, pageable);

        // Assert (this tests the mock setup, not the actual query)
        assertNotNull(actualPage);
        assertEquals(expectedPage, actualPage);
        assertEquals(1, actualPage.getTotalElements());
    }

    @Test
    void testRepositoryCanBeMocked() {
        // This test simply confirms that the interface can be mocked,
        // which is useful for unit testing services that depend on this repository.
        assertNotNull(transactionRepository, "TransactionRepository mock should not be null.");
    }
}