package id.ac.ui.cs.advprog.papikos.payment.repository;

import id.ac.ui.cs.advprog.papikos.payment.model.UserBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BalanceRepositoryTest {
    @Autowired
    private UserBalanceRepository balanceRepository;

    @Test
    void testFindByUserId() {
        UserBalance balance = new UserBalance();
        balance.setUserId("user1");
        balanceRepository.save(balance);

        Optional<UserBalance> found = balanceRepository.findByUserId("user1");
        assertTrue(found.isPresent());
    }
}
