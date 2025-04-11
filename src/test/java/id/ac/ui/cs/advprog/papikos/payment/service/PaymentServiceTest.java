package id.ac.ui.cs.advprog.papikos.payment.service;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import id.ac.ui.cs.advprog.papikos.payment.commands.PaymentCommand;
import id.ac.ui.cs.advprog.papikos.payment.repository.BalanceRepository;

class PaymentServiceTest {
    @Test
    void testTopUp() {
        BalanceRepository balanceRepo = mock(BalanceRepository.class);
        PaymentService paymentService = new PaymentService(balanceRepo, null);

        paymentService.topUp("user1", 100000);
        verify(balanceRepo, atLeastOnce()).findByUserId("user1");
    }
}