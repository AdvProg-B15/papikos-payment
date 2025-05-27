package id.ac.ui.cs.advprog.papikos.payment.service;

// RentalServiceClient might still be needed if rental details are not in the token
import id.ac.ui.cs.advprog.papikos.payment.client.RentalServiceClient;
import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.*;
import id.ac.ui.cs.advprog.papikos.payment.exception.*;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
// Optional is still needed
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final UserBalanceRepository userBalanceRepository;
    private final TransactionRepository transactionRepository;
    // private final AuthServiceClient authServiceClient; // No longer needed
    private final RentalServiceClient rentalServiceClient; // Keep if used

    @Override
    @Transactional // Ensure this is read-write for the save operation
    public BalanceDto getUserBalance(UUID userId) {
        log.info("Fetching balance for userId: {}", userId);
        // Since the user is guaranteed to exist by the validated token,
        // if their balance record is not found, we create it.
        UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.warn("Balance record not found for userId: {}. Creating initial zero balance as user is authenticated.", userId);
                    UserBalance newBalance = new UserBalance(userId, BigDecimal.ZERO);
                    return userBalanceRepository.save(newBalance);
                });
        return mapToBalanceDto(userBalance);
    }

    @Override
    @Transactional
    public TransactionDto topUp(UUID userId, TopUpRequest request) {
        log.info("Initiating top-up for userId: {} with amount: {}", userId, request.amount());

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid top-up amount received: {}", request.amount());
            throw new InvalidOperationException("Top-up amount must be positive.");
        }

        // Find the user's balance record. If it doesn't exist, create it,
        // as the user is guaranteed to exist (authenticated via token).
        // Then lock the record for the update.
        UserBalance userBalance = userBalanceRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    log.warn("Balance record not found for userId: {} during top-up. Creating initial zero balance as user is authenticated.", userId);
                    UserBalance newBalance = new UserBalance(userId, BigDecimal.ZERO);
                    // Save the new balance first
                    userBalanceRepository.save(newBalance);
                    // Then re-fetch with a lock to ensure consistency for the subsequent update.
                    // This ensures that even if created in this transaction, the lock is properly acquired.
                    return userBalanceRepository.findByUserIdWithLock(userId)
                            .orElseThrow(() -> {
                                // This should ideally not happen if save was successful
                                log.error("CRITICAL: Failed to find or lock newly created balance for userId {} during top-up.", userId);
                                return new PaymentProcessingException("Failed to establish and lock balance for user ID: " + userId);
                            });
                });

        log.debug("Successfully obtained and locked balance for userId: {}", userBalance.getUserId());

        BigDecimal oldBalance = userBalance.getBalance();
        userBalance.setBalance(oldBalance.add(request.amount()));
        userBalanceRepository.save(userBalance);
        log.info("Updated balance for userId: {}. Old: {}, New: {}", userBalance.getUserId(), oldBalance, userBalance.getBalance());

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(request.amount());
        transaction.setTransactionType(TransactionType.TOPUP);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setNotes("Internal top-up completed automatically.");

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Saved COMPLETED internal top-up transactionId: {}", savedTransaction.getTransactionId());

        return mapToTransactionDto(savedTransaction);
    }

    @Override
    @Transactional
    public TransactionDto payForRental(UUID tenantUserId, PaymentRequest request) {
        log.info("Processing payment for rentalId: {} by tenantId: {} for amount: {}",
                request.rentalId(), tenantUserId, request.amount());

        // User existence for tenantUserId is guaranteed by the token.
        // Existence of ownerUserId will be implicitly checked when we try to get their balance.

        RentalDetailsDto rental;
        try {
            log.debug("Fetching rental details for rentalId: {}", request.rentalId());
            rental = rentalServiceClient.getRentalDetailsForPayment(request.rentalId());
            if (rental == null) {
                throw new PaymentProcessingException("Received null rental details for rentalId: " + request.rentalId());
            }
            log.info("Fetched rental details for rentalId: {}. Owner={}, Tenant={}, Status={}, Price={}",
                    rental.getRentalId(), rental.getOwnerUserId(), rental.getTenantUserId(), rental.getStatus(), rental.getMonthlyRentPrice());
        } catch (ResourceNotFoundException e) {
            log.warn("Payment failed: Rental not found via RentalService for rentalId: {}", request.rentalId());
            throw e;
        } catch (Exception e) {
            log.error("Payment failed: Error fetching rental details for rentalId {}: {}", request.rentalId(), e.getMessage(), e);
            throw new PaymentProcessingException("Failed to retrieve rental details due to an internal error.", e);
        }

        if (!Objects.equals(rental.getTenantUserId(), tenantUserId)) {
            log.error("Payment authorization failed: Requestor userId {} does not match rental tenant userId {}.", tenantUserId, rental.getTenantUserId());
            throw new InvalidOperationException("User is not the tenant for this rental.");
        }
        if (!"APPROVED".equalsIgnoreCase(rental.getStatus()) && !"ACTIVE".equalsIgnoreCase(rental.getStatus())) {
            log.warn("Payment failed: Rental {} is not in APPROVED or ACTIVE state (status: {}).", request.rentalId(), rental.getStatus());
            throw new InvalidOperationException("Rental is not approved or active for payment.");
        }
        if (rental.getMonthlyRentPrice() == null || request.amount() == null || rental.getMonthlyRentPrice().compareTo(request.amount()) != 0) {
            log.warn("Payment amount mismatch for rental {}. Expected: {}, Received: {}", request.rentalId(), rental.getMonthlyRentPrice(), request.amount());
            throw new InvalidOperationException("Payment amount does not match the expected rental price.");
        }

        UUID ownerUserId = rental.getOwnerUserId();
        BigDecimal paymentAmount = request.amount();

        Transaction tenantPaymentTransaction;
        try {
            // performInternalTransfer will handle creating balance for owner if needed.
            tenantPaymentTransaction = performInternalTransfer(tenantUserId, ownerUserId, paymentAmount, request.rentalId());
            log.info("Internal transfer completed for rental {}. Tenant Tx ID: {}", request.rentalId(), tenantPaymentTransaction.getTransactionId());
        } catch (InsufficientBalanceException | ResourceNotFoundException e) {
            log.warn("Payment failed during internal transfer: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Payment failed: Unexpected error during internal transfer for rental {}: {}", request.rentalId(), e.getMessage(), e);
            throw new PaymentProcessingException("Payment failed due to an internal error during balance transfer.", e);
        }

        return mapToTransactionDto(tenantPaymentTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionHistory(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type, Pageable pageable) {
        log.info("Fetching transaction history for userId: {} with filters - Start: {}, End: {}, Type: {}, Page: {}",
                userId, startDate, endDate, type, pageable);
        // User existence guaranteed by token, so no extra check needed.
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Page<Transaction> transactionPage;
        boolean hasFilters = startDateTime != null || endDateTime != null || type != null;
        if (hasFilters) {
            log.debug("Using filtered transaction query for userId: {}", userId);
            transactionPage = transactionRepository.findUserTransactionsByFilter(
                    userId, startDateTime, endDateTime, type, pageable);
        } else {
            log.debug("Using default transaction query (no filters) for userId: {}", userId);
            transactionPage = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        log.info("Found {} transactions on page {} for userId: {}", transactionPage.getNumberOfElements(), pageable.getPageNumber(), userId);
        return transactionPage.map(this::mapToTransactionDto);
    }

    protected Transaction performInternalTransfer(UUID payerId, UUID payeeId, BigDecimal amount, UUID rentalId) {
        log.debug("Performing internal transfer: Payer={}, Payee={}, Amount={}, RentalId={}", payerId, payeeId, amount, rentalId);

        // Get or create payer's balance (user existence guaranteed by token if payerId is from token)
        UserBalance payerBalance = userBalanceRepository.findByUserIdWithLock(payerId)
                .orElseGet(() -> {
                    log.warn("Payer balance record not found for ID: {}. Creating initial zero balance as user is authenticated.", payerId);
                    UserBalance newBalance = new UserBalance(payerId, BigDecimal.ZERO);
                    userBalanceRepository.save(newBalance);
                    return userBalanceRepository.findByUserIdWithLock(payerId)
                            .orElseThrow(() -> new PaymentProcessingException("Failed to establish and lock payer balance for ID: " + payerId));
                });

        // Get or create payee's balance.
        // We assume payeeId is also a valid user in the system,
        // though not necessarily the one authenticated for *this* request.
        // If a payee might not exist yet in auth service, a check might be needed here,
        // or rely on the fact that a rental implies a valid owner.
        // For now, let's assume payee (owner) should have a balance created if not present.
        UserBalance payeeBalance = userBalanceRepository.findByUserIdWithLock(payeeId)
                .orElseGet(() -> {
                    log.warn("Payee balance record not found for ID: {}. Creating initial zero balance.", payeeId);
                    // Potentially, if payeeId is from an external system (like rental service),
                    // you might want to verify its existence in Auth service before creating a balance.
                    // For simplicity now, we create it.
                    // if (!authServiceClient.userExists(payeeId)) { // If you wanted to check payee from auth service
                    //    throw new ResourceNotFoundException("Payee user not found with ID: " + payeeId);
                    // }
                    UserBalance newBalance = new UserBalance(payeeId, BigDecimal.ZERO);
                    userBalanceRepository.save(newBalance);
                    return userBalanceRepository.findByUserIdWithLock(payeeId)
                            .orElseThrow(() -> new PaymentProcessingException("Failed to establish and lock payee balance for ID: " + payeeId));
                });

        log.debug("Balances locked successfully. Payer: {}, Payee: {}", payerBalance.getUserId(), payeeBalance.getUserId());

        if (payerBalance.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for payer {}. Required: {}, Available: {}", payerId, amount, payerBalance.getBalance());
            throw new InsufficientBalanceException("Insufficient balance for payment. Required: " + amount + ", Available: " + payerBalance.getBalance());
        }

        BigDecimal oldPayerBalance = payerBalance.getBalance();
        BigDecimal oldPayeeBalance = payeeBalance.getBalance();
        payerBalance.setBalance(oldPayerBalance.subtract(amount));
        payeeBalance.setBalance(oldPayeeBalance.add(amount));

        userBalanceRepository.save(payerBalance);
        userBalanceRepository.save(payeeBalance);
        log.info("Updated balances - PayerId: {} (Old: {}, New: {}), PayeeId: {} (Old: {}, New: {})",
                payerId, oldPayerBalance, payerBalance.getBalance(), payeeId, oldPayeeBalance, payeeBalance.getBalance());

        Transaction payerTx = createPaymentTransactionRecord(payerId, amount, TransactionStatus.COMPLETED, rentalId, payerId, payeeId, "Payment sent for rental " + rentalId);
        Transaction payeeTx = createPaymentTransactionRecord(payeeId, amount, TransactionStatus.COMPLETED, rentalId, payerId, payeeId, "Payment received for rental " + rentalId);

        Transaction savedPayerTx = transactionRepository.save(payerTx);
        Transaction savedPayeeTx = transactionRepository.save(payeeTx);
        log.info("Saved payment transactions. Payer Tx ID: {}, Payee Tx ID: {}", savedPayerTx.getTransactionId(), savedPayeeTx.getTransactionId());

        return savedPayerTx;
    }

    private Transaction createPaymentTransactionRecord(UUID userId, BigDecimal amount, TransactionStatus status, UUID rentalId, UUID payerId, UUID payeeId, String notes) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setAmount(amount);
        tx.setStatus(status);
        tx.setRelatedRentalId(rentalId);
        tx.setPayerUserId(payerId);
        tx.setPayeeUserId(payeeId);
        tx.setNotes(notes);
        return tx;
    }

    private BalanceDto mapToBalanceDto(UserBalance entity) {
        if (entity == null) return null;
        return new BalanceDto(entity.getUserId(), entity.getBalance(), entity.getUpdatedAt());
    }

    private TransactionDto mapToTransactionDto(Transaction entity) {
        if (entity == null) return null;
        return new TransactionDto(
                entity.getTransactionId(),
                entity.getUserId(),
                entity.getTransactionType(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getRelatedRentalId(),
                entity.getPayerUserId(),
                entity.getPayeeUserId(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}