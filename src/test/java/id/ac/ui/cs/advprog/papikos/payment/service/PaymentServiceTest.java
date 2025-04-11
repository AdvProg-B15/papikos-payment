package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.model.User;
import id.ac.ui.cs.advprog.papikos.payment.repository.BalanceRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @BeforeEach
    void setUp() {
        // Data dummy sudah otomatis terbuat dari TestDataConfig
    }

    @Test
    void testTopUp() {
        paymentService.topUp("user1", 100000);
        var balance = balanceRepository.findByUserId("user1").orElseThrow();
        assertEquals(100000, balance.getAmount());
    }
}