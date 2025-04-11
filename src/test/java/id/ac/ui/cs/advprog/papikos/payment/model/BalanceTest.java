package id.ac.ui.cs.advprog.papikos.payment.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {
    @Test
    void testAddAmount() {
        Balance balance = new Balance();
        balance.setUserId("user1");
        balance.addAmount(100000);
        assertEquals(100000, balance.getAmount());
    }

    @Test
    void testSubtractAmountThrowsWhenInsufficient() {
        Balance balance = new Balance();
        balance.setAmount(50000);
        assertThrows(IllegalArgumentException.class, () -> {
            balance.subtractAmount(100000);
        });
    }
}