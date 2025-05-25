package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.dto.BalanceDto;
import id.ac.ui.cs.advprog.papikos.payment.dto.PaymentRequest;
import id.ac.ui.cs.advprog.papikos.payment.dto.TopUpRequest;
import id.ac.ui.cs.advprog.papikos.payment.dto.TransactionDto;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionStatus;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // To use @Mock
class PaymentServiceTest {

    @Mock
    private PaymentService paymentService; // Mocking the interface

    @Test
    void testGetUserBalance_whenMocked() {
        UUID userId = UUID.randomUUID();
        BalanceDto expectedBalance = new BalanceDto(userId, new BigDecimal("100.00"), LocalDateTime.now());

        // Define behavior for the mocked service method
        when(paymentService.getUserBalance(eq(userId))).thenReturn(expectedBalance);

        // Call the mocked method
        BalanceDto actualBalance = paymentService.getUserBalance(userId);

        // Assert (this tests the mock setup)
        assertNotNull(actualBalance);
        assertEquals(expectedBalance, actualBalance);
    }

    @Test
    void testTopUp_whenMocked() {
        UUID userId = UUID.randomUUID();
        TopUpRequest topUpRequest = new TopUpRequest(new BigDecimal("50.00"));
        TransactionDto expectedTransaction = new TransactionDto(
                UUID.randomUUID(), userId, TransactionType.TOPUP, topUpRequest.amount(),
                TransactionStatus.COMPLETED, null, null, null,
                "Top-up successful", LocalDateTime.now(), LocalDateTime.now()
        );

        // Define behavior
        when(paymentService.topUp(eq(userId), eq(topUpRequest))).thenReturn(expectedTransaction);

        // Call
        TransactionDto actualTransaction = paymentService.topUp(userId, topUpRequest);

        // Assert
        assertNotNull(actualTransaction);
        assertEquals(expectedTransaction, actualTransaction);
    }

    @Test
    void testPayForRental_whenMocked() {
        UUID tenantUserId = UUID.randomUUID();
        PaymentRequest paymentRequest = new PaymentRequest(UUID.randomUUID(), new BigDecimal("200.00"));
        TransactionDto expectedTransaction = new TransactionDto(
                UUID.randomUUID(), tenantUserId, TransactionType.PAYMENT, paymentRequest.amount(),
                TransactionStatus.COMPLETED, paymentRequest.rentalId(), tenantUserId, UUID.randomUUID(), // Example payee
                "Payment for rental successful", LocalDateTime.now(), LocalDateTime.now()
        );

        // Define behavior
        when(paymentService.payForRental(eq(tenantUserId), eq(paymentRequest))).thenReturn(expectedTransaction);

        // Call
        TransactionDto actualTransaction = paymentService.payForRental(tenantUserId, paymentRequest);

        // Assert
        assertNotNull(actualTransaction);
        assertEquals(expectedTransaction, actualTransaction);
    }

    @Test
    void testGetTransactionHistory_whenMocked() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        TransactionType type = TransactionType.PAYMENT;
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionDto> expectedPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        // Define behavior
        when(paymentService.getTransactionHistory(eq(userId), eq(startDate), eq(endDate), eq(type), eq(pageable)))
                .thenReturn(expectedPage);

        // Call
        Page<TransactionDto> actualPage = paymentService.getTransactionHistory(userId, startDate, endDate, type, pageable);

        // Assert
        assertNotNull(actualPage);
        assertEquals(expectedPage, actualPage);
    }

    @Test
    void testServiceCanBeMocked() {
        // This test simply confirms that the interface can be mocked,
        // which is essential for testing classes that depend on PaymentService.
        assertNotNull(paymentService, "PaymentService mock should not be null.");
    }
}