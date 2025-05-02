package id.ac.ui.cs.advprog.papikos.payment.repository;

import id.ac.ui.cs.advprog.papikos.payment.entity.Transaction;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.entity.UserBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID; // Import UUID

public interface UserBalanceRepository extends JpaRepository<UserBalance, UUID> { // Changed Long to UUID

    // Find by userId (which is the PK and a UUID)
    Optional<UserBalance> findByUserId(UUID userId); // Changed Long to UUID

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ub FROM UserBalance ub WHERE ub.userId = :userId") // Explicit query optional but clear
    Optional<UserBalance> findByUserIdWithLock(@Param("userId") UUID userId); // Changed Long to UUID
}