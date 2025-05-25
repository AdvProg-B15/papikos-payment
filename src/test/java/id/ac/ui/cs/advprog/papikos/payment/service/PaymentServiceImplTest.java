package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.client.RentalServiceClient;
import id.ac.ui.cs.advprog.papikos.payment.dto.BalanceDto;
import id.ac.ui.cs.advprog.papikos.payment.entity.UserBalance;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private UserBalanceRepository userBalanceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private RentalServiceClient rentalServiceClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void testGetUserBalance_whenNoBalance_thenCreateNew() {
        UUID userId = UUID.randomUUID();

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(authServiceClient.userExists(userId)).thenReturn(true);

        UserBalance newBalance = new UserBalance(userId, BigDecimal.ZERO);
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(newBalance);

        BalanceDto result = paymentService.getUserBalance(userId);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.balance());
        verify(userBalanceRepository).save(any(UserBalance.class));
    }
}

