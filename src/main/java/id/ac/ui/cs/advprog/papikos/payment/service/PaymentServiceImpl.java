package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.client.AuthServiceClient;
import id.ac.ui.cs.advprog.papikos.payment.client.RentalServiceClient;
import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.*;
import id.ac.ui.cs.advprog.papikos.payment.exception.*;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor; // Lombok for constructor injection
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

@Service
@RequiredArgsConstructor // Generates constructor for final fields
@Slf4j // Lombok logging
public class PaymentServiceImpl implements PaymentService {

    private final UserBalanceRepository userBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final AuthServiceClient authServiceClient;
    private final RentalServiceClient rentalServiceClient;
    // private final PaymentGatewayClient paymentGatewayClient; // If needed

    @Override
    @Transactional(readOnly = true) // Good practice for read operations
    public BalanceDto getUserBalance(Long userId) {
        log.info("Fetching balance for userId: {}", userId);
        Optional<UserBalance> balanceOpt = userBalanceRepository.findByUserId(userId);

        if (balanceOpt.isPresent()) {
            return mapToBalanceDto(balanceOpt.get());
        } else {
            log.warn("Balance record not found for userId: {}. Checking user existence.", userId);
            // Check if user exists before creating a balance record
            if (!authServiceClient.userExists(userId)) {
                log.error("User with ID {} not found.", userId);
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }
            // User exists, but no balance record - create one
            log.info("User {} exists, creating initial zero balance.", userId);
            UserBalance newBalance = new UserBalance(userId, BigDecimal.ZERO);
            UserBalance savedBalance = userBalanceRepository.save(newBalance);
            return mapToBalanceDto(savedBalance);
        }
    }

    @Override
    @Transactional
    public TopUpInitiationResponse initiateTopUp(Long userId, TopUpRequest request) {
        log.info("Initiating top-up for userId: {} with amount: {}", userId, request.getAmount());
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Top-up amount must be positive.");
        }

        // Create pending transaction record
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.TOPUP);
        transaction.setStatus(TransactionStatus.PENDING);
        // Set other fields like notes if necessary

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Saved pending top-up transactionId: {}", savedTransaction.getTransactionId());

        // --- Integration with Payment Gateway would happen here ---
        // String gatewayUrl = paymentGatewayClient.initiate(savedTransaction.getAmount(), savedTransaction.getTransactionId());
        String mockGatewayUrl = "http://mock-gateway.url/pay?tx=" + savedTransaction.getTransactionId(); // Placeholder

        return new TopUpInitiationResponse(savedTransaction.getTransactionId(), mockGatewayUrl);
    }


    @Override
    @Transactional // Ensure atomicity
    public void confirmTopUp(Long transactionId) {
        log.info("Confirming top-up for transactionId: {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up transaction not found with ID: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Top-up transaction {} is not in PENDING state (current: {}). Skipping confirmation.", transactionId, transaction.getStatus());
            // Decide whether to throw or just log and return
            throw new InvalidOperationException("Transaction is not in PENDING state.");
        }
        if (transaction.getTransactionType() != TransactionType.TOPUP) {
            log.error("Transaction {} is not a TOPUP transaction.", transactionId);
            throw new InvalidOperationException("Transaction is not of type TOPUP.");
        }

        // Use lock to prevent race conditions on balance update
        UserBalance userBalance = userBalanceRepository.findByUserIdWithLock(transaction.getUserId())
                .orElseThrow(() -> {
                    // This indicates a potential data inconsistency issue
                    log.error("User balance record not found for userId {} during top-up confirmation of txId {}.", transaction.getUserId(), transactionId);
                    return new PaymentProcessingException("User balance record not found for user ID: " + transaction.getUserId());
                });

        // Update balance
        userBalance.setBalance(userBalance.getBalance().add(transaction.getAmount()));
        userBalanceRepository.save(userBalance);
        log.info("Updated balance for userId: {} to {}", userBalance.getUserId(), userBalance.getBalance());

        // Update transaction status
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setNotes("Top-up completed successfully.");
        transactionRepository.save(transaction);
        log.info("Marked transaction {} as COMPLETED.", transactionId);

        // TODO: Optionally send a notification to the user via Notification Service internal call
    }


    @Override
    @Transactional // This operation involves multiple DB updates, needs to be atomic
    public TransactionDto payForRental(Long tenantUserId, PaymentRequest request) {
        log.info("Attempting payment for rentalId: {} by tenantId: {} for amount: {}",
                request.getRentalId(), tenantUserId, request.getAmount());

        // 1. Get Rental Details from Rental Service
        RentalDetailsDto rental;
        try {
            rental = rentalServiceClient.getRentalDetailsForPayment(request.getRentalId());
            log.info("Fetched rental details: Owner={}, Tenant={}, Status={}, Price={}",
                    rental.getOwnerUserId(), rental.getTenantUserId(), rental.getStatus(), rental.getMonthlyRentPrice());
        } catch (ResourceNotFoundException e) {
            log.warn("Rental not found: {}", request.getRentalId());
            throw e; // Re-throw specific exception
        } catch (Exception e) { // Catch broader exceptions from client call
            log.error("Error fetching rental details for rentalId {}: {}", request.getRentalId(), e.getMessage());
            throw new PaymentProcessingException("Failed to retrieve rental details.", e);
        }


        // 2. Validate Rental and Request
        if (!Objects.equals(rental.getTenantUserId(), tenantUserId)) {
            log.error("Tenant ID mismatch. Requestor: {}, Rental Tenant: {}", tenantUserId, rental.getTenantUserId());
            throw new InvalidOperationException("User is not the tenant for this rental.");
        }
        if (!"APPROVED".equalsIgnoreCase(rental.getStatus()) && !"ACTIVE".equalsIgnoreCase(rental.getStatus())) { // Allow payment if ACTIVE or just APPROVED
            log.warn("Rental {} is not in an approved or active state (status: {}).", request.getRentalId(), rental.getStatus());
            throw new InvalidOperationException("Rental is not approved or active for payment.");
        }
        if (rental.getMonthlyRentPrice() == null || request.getAmount() == null || rental.getMonthlyRentPrice().compareTo(request.getAmount()) != 0) {
            log.warn("Payment amount mismatch for rental {}. Expected: {}, Received: {}", request.getRentalId(), rental.getMonthlyRentPrice(), request.getAmount());
            throw new InvalidOperationException("Payment amount does not match the rental price.");
        }

        Long ownerUserId = rental.getOwnerUserId();
        BigDecimal paymentAmount = request.getAmount();

        // 3. Perform Internal Transfer (Locks balances)
        Transaction tenantTransaction;
        try {
            tenantTransaction = performInternalTransfer(tenantUserId, ownerUserId, paymentAmount, request.getRentalId());
            log.info("Internal transfer completed for rental {}. Tenant Tx ID: {}", request.getRentalId(), tenantTransaction.getTransactionId());
        } catch (InsufficientBalanceException | ResourceNotFoundException e) {
            log.warn("Internal transfer failed: {}", e.getMessage());
            throw e; // Re-throw specific exceptions
        } catch (Exception e) {
            log.error("Unexpected error during internal transfer for rental {}: {}", request.getRentalId(), e.getMessage(), e);
            throw new PaymentProcessingException("Payment failed due to an internal error during transfer.", e);
        }

        // TODO: Optionally call Rental Service to update rental status (e.g., to ACTIVE if just APPROVED)
        // rentalServiceClient.markRentalAsActive(request.getRentalId());

        // TODO: Optionally send notifications to Tenant and Owner

        // 4. Return DTO of the tenant's payment transaction
        return mapToTransactionDto(tenantTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionHistory(Long userId, LocalDate startDate, LocalDate endDate, TransactionType type, Pageable pageable) {
        log.info("Fetching transaction history for userId: {} with filters - Start: {}, End: {}, Type: {}, Page: {}",
                userId, startDate, endDate, type, pageable);

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        // End date is typically exclusive, so add 1 day if specified
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Page<Transaction> transactionPage;
        if (startDateTime != null || endDateTime != null || type != null) {
            transactionPage = transactionRepository.findUserTransactionsByFilter(
                    userId, startDateTime, endDateTime, type, pageable);
            log.debug("Using filtered transaction query.");
        } else {
            transactionPage = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            log.debug("Using default transaction query (no filters).");
        }

        return transactionPage.map(this::mapToTransactionDto); // Map Page<Entity> to Page<DTO>
    }

    // --- Helper Methods ---

    // This method handles the core balance transfer logic and is marked private/protected
    // It should be called within a transactional context (@Transactional on the public method)
    //@Transactional // Inherits from calling method, but ensures atomicity if called internally elsewhere
    protected Transaction performInternalTransfer(Long payerId, Long payeeId, BigDecimal amount, Long rentalId) {
        log.debug("Performing internal transfer: Payer={}, Payee={}, Amount={}, RentalId={}", payerId, payeeId, amount, rentalId);

        // Lock and Fetch Balances
        UserBalance payerBalance = userBalanceRepository.findByUserIdWithLock(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer balance record not found for ID: " + payerId));
        UserBalance payeeBalance = userBalanceRepository.findByUserIdWithLock(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee balance record not found for ID: " + payeeId));

        // Check Payer Balance
        if (payerBalance.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for payer {}. Required: {}, Available: {}", payerId, amount, payerBalance.getBalance());
            throw new InsufficientBalanceException("Insufficient balance for payment.");
        }

        // Perform Transfer
        payerBalance.setBalance(payerBalance.getBalance().subtract(amount));
        payeeBalance.setBalance(payeeBalance.getBalance().add(amount));

        userBalanceRepository.save(payerBalance);
        userBalanceRepository.save(payeeBalance);
        log.debug("Updated balances - Payer: {}, Payee: {}", payerBalance.getBalance(), payeeBalance.getBalance());

        // Create Transactions for both parties
        Transaction payerTx = createPaymentTransaction(payerId, amount, TransactionStatus.COMPLETED, rentalId, payerId, payeeId, "Payment for rental " + rentalId);
        Transaction payeeTx = createPaymentTransaction(payeeId, amount, TransactionStatus.COMPLETED, rentalId, payerId, payeeId, "Received payment for rental " + rentalId); // Payee sees it as income

        transactionRepository.save(payerTx);
        transactionRepository.save(payeeTx);
        log.debug("Saved payment transactions. Payer Tx ID: {}, Payee Tx ID: {}", payerTx.getTransactionId(), payeeTx.getTransactionId());

        return payerTx; // Return the payer's transaction for the API response
    }

    private Transaction createPaymentTransaction(Long userId, BigDecimal amount, TransactionStatus status, Long rentalId, Long payerId, Long payeeId, String notes) {
        Transaction tx = new Transaction();
        tx.setUserId(userId); // The user whose history this belongs to
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
        return new BalanceDto(entity.getUserId(), entity.getBalance(), entity.getUpdatedAt());
    }

    private TransactionDto mapToTransactionDto(Transaction entity) {
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