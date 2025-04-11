package id.ac.ui.cs.advprog.papikos.payment.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {
    @Test
    void testTransactionCreation() {
        Transaction transaction = new Transaction();
        transaction.setUserId("user1");
        transaction.setAmount(100000);
        transaction.setType(PaymentType.TOPUP);

        assertNotNull(transaction.getCreatedAt());
        assertEquals("user1", transaction.getUserId());
    }
}
