package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.client.AuthServiceClient;
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
import java.util.Optional;
import java.util.UUID; // Use UUID for IDs

@Service
@RequiredArgsConstructor // Auto-creates constructor for final fields (Dependency Injection)
@Slf4j // Logging framework facade
public class PaymentServiceImpl implements PaymentService {

    // Dependencies injected via constructor by @RequiredArgsConstructor
    private final UserBalanceRepository userBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final AuthServiceClient authServiceClient;     // Client to talk to Auth Service
    private final RentalServiceClient rentalServiceClient; // Client to talk to Rental Service
    // private final PaymentGatewayClient paymentGatewayClient; // Would be needed for real top-ups

    @Override
    @Transactional(readOnly = true) // Indicate this method only reads data
    public BalanceDto getUserBalance(UUID userId) {
        log.info("Fetching balance for userId: {}", userId);
        Optional<UserBalance> balanceOpt = userBalanceRepository.findByUserId(userId);

        if (balanceOpt.isPresent()) {
            // Found existing balance record
            return mapToBalanceDto(balanceOpt.get());
        } else {
            // No balance record found, check if the user actually exists
            log.warn("Balance record not found for userId: {}. Checking user existence.", userId);
            if (!authServiceClient.userExists(userId)) {
                log.error("User with ID {} not found via Auth service.", userId);
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }
            // User exists, create their initial balance record
            log.info("User {} exists, creating initial zero balance.", userId);
            UserBalance newBalance = new UserBalance(userId, BigDecimal.ZERO);
            UserBalance savedBalance = userBalanceRepository.save(newBalance);
            return mapToBalanceDto(savedBalance);
        }
    }

    @Override
    @Transactional // This method modifies data
    public TopUpInitiationResponse initiateTopUp(UUID userId, TopUpRequest request) {
        log.info("Initiating top-up for userId: {} with amount: {}", userId, request.amount());

        // Basic validation for the amount
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid top-up amount received: {}", request.amount());
            throw new InvalidOperationException("Top-up amount must be positive.");
        }

        // Create a transaction record to track this top-up attempt
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(request.amount());
        transaction.setTransactionType(TransactionType.TOPUP);
        transaction.setStatus(TransactionStatus.PENDING); // Starts as pending
        transaction.setNotes("Top-up initiated.");

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Saved pending top-up transactionId: {}", savedTransaction.getTransactionId());

        // --- Placeholder for Payment Gateway Interaction ---
        // In a real app, you would call the payment gateway here using the
        // savedTransaction.getTransactionId() and request.getAmount().
        // The gateway would return a URL or instructions.
        // Example: String gatewayUrl = paymentGatewayClient.initiatePayment(savedTransaction.getId(), ...);
        String mockGatewayUrl = "http://mock-payment-gateway.test/pay?txId=" + savedTransaction.getTransactionId();
        log.warn("Using mock payment gateway URL: {}", mockGatewayUrl);
        // -----------------------------------------------------

        return new TopUpInitiationResponse(savedTransaction.getTransactionId(), mockGatewayUrl);
    }


    @Override
    @Transactional // This method modifies data and needs atomicity
    public void confirmTopUp(UUID transactionId) {
        log.info("Confirming top-up for transactionId: {}", transactionId);

        // 1. Find the transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error("Top-up confirmation failed: Transaction {} not found.", transactionId);
                    return new ResourceNotFoundException("Top-up transaction not found with ID: " + transactionId);
                });

        // 2. Validate the transaction state
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Top-up confirmation skipped: Transaction {} is not in PENDING state (current: {}).", transactionId, transaction.getStatus());
            throw new InvalidOperationException("Transaction is not in PENDING state.");
        }
        if (transaction.getTransactionType() != TransactionType.TOPUP) {
            log.error("Top-up confirmation failed: Transaction {} is not a TOPUP type (type: {}).", transactionId, transaction.getTransactionType());
            throw new InvalidOperationException("Transaction is not of type TOPUP.");
        }

        // 3. Find and Lock the user's balance record to prevent race conditions
        log.debug("Attempting to lock balance for userId: {}", transaction.getUserId());
        UserBalance userBalance = userBalanceRepository.findByUserIdWithLock(transaction.getUserId())
                .orElseThrow(() -> {
                    // This should ideally not happen if the user existed when initiating top-up
                    log.error("CRITICAL: User balance record not found for userId {} during top-up confirmation of txId {}.", transaction.getUserId(), transactionId);
                    return new PaymentProcessingException("User balance record not found for user ID: " + transaction.getUserId());
                });
        log.debug("Successfully locked balance for userId: {}", userBalance.getUserId());

        // 4. Update the balance
        BigDecimal oldBalance = userBalance.getBalance();
        userBalance.setBalance(oldBalance.add(transaction.getAmount()));
        userBalanceRepository.save(userBalance); // Save the updated balance
        log.info("Updated balance for userId: {}. Old: {}, New: {}", userBalance.getUserId(), oldBalance, userBalance.getBalance());

        // 5. Update the transaction status
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setNotes("Top-up completed successfully via confirmation.");
        transactionRepository.save(transaction); // Save the updated transaction
        log.info("Marked transaction {} as COMPLETED.", transactionId);

    }


    @Override
    @Transactional // Complex operation involving multiple steps and services
    public TransactionDto payForRental(UUID tenantUserId, PaymentRequest request) {
        log.info("Processing payment for rentalId: {} by tenantId: {} for amount: {}",
                request.rentalId(), tenantUserId, request.amount());

        // 1. Get Rental Details (call external Rental Service)
        RentalDetailsDto rental;
        try {
            log.debug("Fetching rental details for rentalId: {}", request.rentalId());
            rental = rentalServiceClient.getRentalDetailsForPayment(request.rentalId());
            if (rental == null) { // Defensive check
                throw new PaymentProcessingException("Received null rental details for rentalId: " + request.rentalId());
            }
            log.info("Fetched rental details for rentalId: {}. Owner={}, Tenant={}, Status={}, Price={}",
                    rental.getRentalId(), rental.getOwnerUserId(), rental.getTenantUserId(), rental.getStatus(), rental.getMonthlyRentPrice());
        } catch (ResourceNotFoundException e) {
            log.warn("Payment failed: Rental not found via RentalService for rentalId: {}", request.rentalId());
            throw e; // Re-throw to be handled by controller advice
        } catch (Exception e) { // Catch other potential errors from the client call
            log.error("Payment failed: Error fetching rental details for rentalId {}: {}", request.rentalId(), e.getMessage(), e);
            throw new PaymentProcessingException("Failed to retrieve rental details due to an internal error.", e);
        }


        // 2. Validate the Request against Rental Details
        // Check if the requestor is the actual tenant
        if (!Objects.equals(rental.getTenantUserId(), tenantUserId)) {
            log.error("Payment authorization failed: Requestor userId {} does not match rental tenant userId {}.", tenantUserId, rental.getTenantUserId());
            throw new InvalidOperationException("User is not the tenant for this rental.");
        }
        // Check if the rental is in a state where payment is allowed
        if (!"APPROVED".equalsIgnoreCase(rental.getStatus()) && !"ACTIVE".equalsIgnoreCase(rental.getStatus())) {
            log.warn("Payment failed: Rental {} is not in APPROVED or ACTIVE state (status: {}).", request.rentalId(), rental.getStatus());
            throw new InvalidOperationException("Rental is not approved or active for payment.");
        }
        // Check if the payment amount matches the expected rent price
        if (rental.getMonthlyRentPrice() == null || request.amount() == null || rental.getMonthlyRentPrice().compareTo(request.amount()) != 0) {
            log.warn("Payment amount mismatch for rental {}. Expected: {}, Received: {}", request.rentalId(), rental.getMonthlyRentPrice(), request.amount());
            throw new InvalidOperationException("Payment amount does not match the expected rental price.");
        }

        UUID ownerUserId = rental.getOwnerUserId();
        BigDecimal paymentAmount = request.amount();

        // 3. Perform the Core Balance Transfer Logic
        // This helper method locks balances and updates records atomically
        Transaction tenantPaymentTransaction;
        try {
            tenantPaymentTransaction = performInternalTransfer(tenantUserId, ownerUserId, paymentAmount, request.rentalId());
            log.info("Internal transfer completed for rental {}. Tenant Tx ID: {}", request.rentalId(), tenantPaymentTransaction.getTransactionId());
        } catch (InsufficientBalanceException | ResourceNotFoundException e) {
            log.warn("Payment failed during internal transfer: {}", e.getMessage());
            throw e; // Re-throw specific known exceptions
        } catch (Exception e) { // Catch unexpected issues during transfer
            log.error("Payment failed: Unexpected error during internal transfer for rental {}: {}", request.rentalId(), e.getMessage(), e);
            throw new PaymentProcessingException("Payment failed due to an internal error during balance transfer.", e);
        }

        return mapToTransactionDto(tenantPaymentTransaction);
    }

    @Override
    @Transactional(readOnly = true) // Read-only operation
    public Page<TransactionDto> getTransactionHistory(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type, Pageable pageable) {
        log.info("Fetching transaction history for userId: {} with filters - Start: {}, End: {}, Type: {}, Page: {}",
                userId, startDate, endDate, type, pageable);

        // Convert LocalDate to LocalDateTime for database comparison
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        // For end date, consider the whole day up to midnight of the next day for inclusiveness
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Page<Transaction> transactionPage;

        // Decide which repository method to call based on filters
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

        // Map the Page<Transaction> to Page<TransactionDto>
        return transactionPage.map(this::mapToTransactionDto);
    }

    // --- Helper Methods ---

    // Protected scope, intended for internal use within the service
    protected Transaction performInternalTransfer(UUID payerId, UUID payeeId, BigDecimal amount, UUID rentalId) {
        log.debug("Performing internal transfer: Payer={}, Payee={}, Amount={}, RentalId={}", payerId, payeeId, amount, rentalId);

        // 1. Lock and Fetch Balances to prevent race conditions
        log.debug("Locking balance for payerId: {}", payerId);
        UserBalance payerBalance = userBalanceRepository.findByUserIdWithLock(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer balance record not found for ID: " + payerId));

        log.debug("Locking balance for payeeId: {}", payeeId);
        UserBalance payeeBalance = userBalanceRepository.findByUserIdWithLock(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee balance record not found for ID: " + payeeId));
        log.debug("Balances locked successfully.");

        // 2. Check Payer's Balance
        if (payerBalance.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for payer {}. Required: {}, Available: {}", payerId, amount, payerBalance.getBalance());
            throw new InsufficientBalanceException("Insufficient balance for payment. Required: " + amount + ", Available: " + payerBalance.getBalance());
        }

        // 3. Perform the balance update
        BigDecimal oldPayerBalance = payerBalance.getBalance();
        BigDecimal oldPayeeBalance = payeeBalance.getBalance();
        payerBalance.setBalance(oldPayerBalance.subtract(amount));
        payeeBalance.setBalance(oldPayeeBalance.add(amount));

        // 4. Save the updated balances
        userBalanceRepository.save(payerBalance);
        userBalanceRepository.save(payeeBalance);
        log.info("Updated balances - PayerId: {} (Old: {}, New: {}), PayeeId: {} (Old: {}, New: {})",
                payerId, oldPayerBalance, payerBalance.getBalance(), payeeId, oldPayeeBalance, payeeBalance.getBalance());

        // 5. Create Transaction records for both parties
        Transaction payerTx = createPaymentTransactionRecord(payerId, amount, TransactionStatus.COMPLETED, rentalId, payerId, payeeId, "Payment sent for rental " + rentalId);
        Transaction payeeTx = createPaymentTransactionRecord(payeeId, amount, TransactionStatus.COMPLETED, rentalId, payerId, payeeId, "Payment received for rental " + rentalId); // Note: userId is payeeId here

        // 6. Save the transactions
        Transaction savedPayerTx = transactionRepository.save(payerTx);
        Transaction savedPayeeTx = transactionRepository.save(payeeTx);
        log.info("Saved payment transactions. Payer Tx ID: {}, Payee Tx ID: {}", savedPayerTx.getTransactionId(), savedPayeeTx.getTransactionId());

        // 7. Return the payer's transaction (useful for the API response)
        return savedPayerTx;
    }

    /**
     * Helper method to create a Transaction entity object.
     */
    private Transaction createPaymentTransactionRecord(UUID userId, BigDecimal amount, TransactionStatus status, UUID rentalId, UUID payerId, UUID payeeId, String notes) {
        Transaction tx = new Transaction();
        tx.setUserId(userId); // The user whose history this transaction belongs to
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setAmount(amount); // Amount is always positive in the record
        tx.setStatus(status);
        tx.setRelatedRentalId(rentalId);
        tx.setPayerUserId(payerId);
        tx.setPayeeUserId(payeeId);
        tx.setNotes(notes);
        // Timestamps (createdAt, updatedAt) will be set automatically by JPA
        return tx;
    }


    /**
     * Maps a UserBalance entity to a BalanceDto.
     */
    private BalanceDto mapToBalanceDto(UserBalance entity) {
        if (entity == null) return null;
        return new BalanceDto(entity.getUserId(), entity.getBalance(), entity.getUpdatedAt());
    }

    /**
     * Maps a Transaction entity to a TransactionDto.
     */
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