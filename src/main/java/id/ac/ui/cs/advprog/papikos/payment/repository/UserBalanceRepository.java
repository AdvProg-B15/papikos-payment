package id.ac.ui.cs.advprog.papikos.payment.repository;

public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {
    Optional<UserBalance> findByUserId(Long userId);

    // Use pessimistic lock for concurrent balance updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserBalance> findByUserIdWithLock(Long userId);
}