package id.ac.ui.cs.advprog.papikos.payment.repository;

import id.ac.ui.cs.advprog.papikos.payment.entity.Transaction;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID; // Import UUID

// Repository now deals with UUID as the primary key type
public interface TransactionRepository extends JpaRepository<Transaction, UUID> { // Changed Long to UUID

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable); // Changed Long to UUID

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR t.createdAt < :endDate) " +
            "AND (:transactionType IS NULL OR t.transactionType = :transactionType) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findUserTransactionsByFilter(
            @Param("userId") UUID userId, // Changed Long to UUID
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable);
}