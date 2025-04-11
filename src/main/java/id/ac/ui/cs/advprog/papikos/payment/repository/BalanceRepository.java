package id.ac.ui.cs.advprog.papikos.payment.repository;

import id.ac.ui.cs.advprog.papikos.payment.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, String> {
    Optional<Balance> findByUserId(String userId);
}
