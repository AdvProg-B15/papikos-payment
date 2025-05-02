package id.ac.ui.cs.advprog.papikos.payment.repository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Example Custom Query for filtering (using JPQL)
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR t.createdAt < :endDate) " + // End date often exclusive
            "AND (:transactionType IS NULL OR t.transactionType = :transactionType) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findUserTransactionsByFilter(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable);
}