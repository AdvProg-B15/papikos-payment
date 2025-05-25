package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.client.RentalServiceClient;
import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.*;
import id.ac.ui.cs.advprog.papikos.payment.exception.*;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private UserBalanceRepository userBalanceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RentalServiceClient rentalServiceClient;

    @InjectMocks
    @Spy
    private PaymentServiceImpl paymentService;

    private UUID userId;
    private UUID rentalId;
    private UUID ownerId;
    private UserBalance userBalance; // General purpose user balance for some tests
    private Transaction sampleTransaction; // General purpose transaction for some tests

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rentalId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        userBalance = new UserBalance(userId, new BigDecimal("1000.00"));
        userBalance.setUpdatedAt(LocalDateTime.now());

        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId(UUID.randomUUID());
        sampleTransaction.setUserId(userId);
        sampleTransaction.setAmount(new BigDecimal("100.00"));
        sampleTransaction.setTransactionType(TransactionType.TOPUP);
        sampleTransaction.setStatus(TransactionStatus.COMPLETED);
        sampleTransaction.setNotes("Sample transaction notes");
        sampleTransaction.setCreatedAt(LocalDateTime.now().minusHours(1));
        sampleTransaction.setUpdatedAt(LocalDateTime.now());
    }

    // --- getUserBalance Tests ---
    @Test
    void getUserBalance_whenBalanceExists_returnsBalanceDto() {
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        BalanceDto result = paymentService.getUserBalance(userId);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(0, userBalance.getBalance().compareTo(result.balance()));
        assertEquals(userBalance.getUpdatedAt(), result.updatedAt());
        verify(userBalanceRepository).findByUserId(userId);
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void getUserBalance_whenBalanceNotExists_createsAndReturnsZeroBalanceDto() {
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> {
            UserBalance ub = invocation.getArgument(0);
            ub.setUserId(userId); // Ensure userId is set if new UserBalance() was used
            ub.setBalance(BigDecimal.ZERO);
            ub.setUpdatedAt(LocalDateTime.now()); // Simulate JPA @UpdateTimestamp
            return ub;
        });

        BalanceDto result = paymentService.getUserBalance(userId);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.balance()));
        assertNotNull(result.updatedAt());
        verify(userBalanceRepository).findByUserId(userId);
        verify(userBalanceRepository).save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().equals(BigDecimal.ZERO)));
    }

    // --- topUp Tests ---
    private Transaction createFullMockTransaction(UUID txId, UUID uId, BigDecimal amt, TransactionType type, TransactionStatus stat) {
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setUserId(uId);
        tx.setAmount(amt);
        tx.setTransactionType(type);
        tx.setStatus(stat);
        tx.setNotes("Internal top-up completed automatically.");
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        return tx;
    }
    @Test
    void topUp_whenValidRequestAndBalanceExists_succeeds() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        UserBalance existingBalance = new UserBalance(userId, new BigDecimal("500.00"));
        existingBalance.setUpdatedAt(LocalDateTime.now());

        Transaction savedTx = createFullMockTransaction(UUID.randomUUID(), userId, request.amount(), TransactionType.TOPUP, TransactionStatus.COMPLETED);

        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(existingBalance));
        // Mocking save for the updated balance
        when(userBalanceRepository.save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().compareTo(new BigDecimal("600.00")) == 0 )))
                .thenAnswer(invocation -> {
                    UserBalance saved = invocation.getArgument(0);
                    saved.setUpdatedAt(LocalDateTime.now()); // Simulate timestamp update
                    return saved;
                });
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionDto result = paymentService.topUp(userId, request);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(0, request.amount().compareTo(result.amount()));
        assertEquals(TransactionType.TOPUP, result.transactionType());
        assertEquals(TransactionStatus.COMPLETED, result.status());
        assertEquals(savedTx.getTransactionId(), result.transactionId());
        verify(userBalanceRepository).save(existingBalance); // Verifies the object with updated balance is saved
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void topUp_whenNewBalanceLockFails_throwsPaymentProcessingException() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        UserBalance initialNewBalance = new UserBalance(userId, BigDecimal.ZERO);

        when(userBalanceRepository.findByUserIdWithLock(userId))
                .thenReturn(Optional.empty()) // First call
                .thenReturn(Optional.empty()); // Second call (simulating lock fetch failure)
        when(userBalanceRepository.save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().equals(BigDecimal.ZERO))))
                .thenReturn(initialNewBalance);

        assertThrows(PaymentProcessingException.class, () -> paymentService.topUp(userId, request));
        verify(userBalanceRepository).save(any(UserBalance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }


    @Test
    void topUp_whenAmountIsNull_throwsInvalidOperationException() {
        TopUpRequest request = new TopUpRequest(null);
        assertThrows(InvalidOperationException.class, () -> paymentService.topUp(userId, request));
    }

    @Test
    void topUp_whenAmountIsZero_throwsInvalidOperationException() {
        TopUpRequest request = new TopUpRequest(BigDecimal.ZERO);
        assertThrows(InvalidOperationException.class, () -> paymentService.topUp(userId, request));
    }

    @Test
    void topUp_whenAmountIsNegative_throwsInvalidOperationException() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("-10.00"));
        assertThrows(InvalidOperationException.class, () -> paymentService.topUp(userId, request));
    }

    // --- payForRental Tests ---
    private RentalDetailsDto createMockRentalDetails(UUID rentalId, UUID tenantId, UUID ownerId, String status, BigDecimal price) {
        RentalDetailsDto details = new RentalDetailsDto();
        details.setRentalId(rentalId);
        details.setTenantUserId(tenantId);
        details.setOwnerUserId(ownerId);
        details.setStatus(status);
        details.setMonthlyRentPrice(price);
        return details;
    }

    private Transaction createFullPaymentTransaction(UUID txId, UUID uId, BigDecimal amt, TransactionType type, TransactionStatus stat, UUID rentalId, UUID payer, UUID payee, String notes) {
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setUserId(uId);
        tx.setAmount(amt);
        tx.setTransactionType(type);
        tx.setStatus(stat);
        tx.setRelatedRentalId(rentalId);
        tx.setPayerUserId(payer);
        tx.setPayeeUserId(payee);
        tx.setNotes(notes);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        return tx;
    }


    @Test
    void payForRental_successfulPayment() {
        BigDecimal rentPrice = new BigDecimal("300.00");
        PaymentRequest request = new PaymentRequest(rentalId, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "APPROVED", rentPrice);
        UserBalance tenantBalance = new UserBalance(userId, new BigDecimal("500.00"));
        UserBalance ownerBalance = new UserBalance(ownerId, new BigDecimal("100.00"));

        Transaction payerTxMock = createFullPaymentTransaction(UUID.randomUUID(), userId, rentPrice, TransactionType.PAYMENT, TransactionStatus.COMPLETED, rentalId, userId, ownerId, "Payment sent for rental " + rentalId);

        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);
        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(tenantBalance));
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.of(ownerBalance));

        // Capture UserBalance saves
        ArgumentCaptor<UserBalance> ubCaptor = ArgumentCaptor.forClass(UserBalance.class);
        when(userBalanceRepository.save(ubCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Capture Transaction saves
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        // Return payer's transaction when it's saved, then payee's or any other
        when(transactionRepository.save(txCaptor.capture()))
                .thenAnswer(inv -> {
                    Transaction tx = inv.getArgument(0);
                    if (tx.getUserId().equals(userId)) return payerTxMock; // Return the fully mocked payer tx for mapping
                    Transaction otherTx = new Transaction(); // Mock for payee tx or others
                    otherTx.setTransactionId(UUID.randomUUID());
                    return otherTx;
                });

        TransactionDto result = paymentService.payForRental(userId, request);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.status());
        assertEquals(payerTxMock.getTransactionId(), result.transactionId()); // Ensure mapped from payerTx

        List<UserBalance> savedBalances = ubCaptor.getAllValues();
        assertEquals(2, savedBalances.size());
        UserBalance savedTenantBalance = savedBalances.stream().filter(b -> b.getUserId().equals(userId)).findFirst().orElse(null);
        UserBalance savedOwnerBalance = savedBalances.stream().filter(b -> b.getUserId().equals(ownerId)).findFirst().orElse(null);

        assertNotNull(savedTenantBalance);
        assertNotNull(savedOwnerBalance);
        assertEquals(0, new BigDecimal("200.00").compareTo(savedTenantBalance.getBalance())); // 500 - 300
        assertEquals(0, new BigDecimal("400.00").compareTo(savedOwnerBalance.getBalance())); // 100 + 300

        List<Transaction> savedTransactions = txCaptor.getAllValues();
        assertEquals(2, savedTransactions.size()); // Payer and Payee transactions
    }

    @Test
    void payForRental_whenPayerBalanceNotFound_createsAndProceedsButInsufficient() {
        BigDecimal rentPrice = new BigDecimal("300.00");
        PaymentRequest request = new PaymentRequest(rentalId, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "ACTIVE", rentPrice);
        UserBalance ownerBalance = new UserBalance(ownerId, new BigDecimal("100.00"));
        UserBalance createdPayerBalance = new UserBalance(userId, BigDecimal.ZERO); // Payer starts at zero

        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);
        // Payer balance interactions
        when(userBalanceRepository.findByUserIdWithLock(userId))
                .thenReturn(Optional.empty()) // Not found first for payer
                .thenReturn(Optional.of(createdPayerBalance)); // Payer found after creation (with 0 balance)
        // Owner balance
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.of(ownerBalance));

        // Mock save for new payer balance
        when(userBalanceRepository.save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().equals(BigDecimal.ZERO))))
                .thenReturn(createdPayerBalance);

        assertThrows(InsufficientBalanceException.class, () -> paymentService.payForRental(userId, request));

        verify(userBalanceRepository).save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().equals(BigDecimal.ZERO)));
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class)); // Only one save (payer creation)
        verify(transactionRepository, never()).save(any(Transaction.class));
    }


    @Test
    void payForRental_rentalServiceReturnsNull_throwsPaymentProcessingException() {
        PaymentRequest request = new PaymentRequest(rentalId, new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(null);

        assertThrows(PaymentProcessingException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentalServiceThrowsResourceNotFound_rethrowsException() {
        PaymentRequest request = new PaymentRequest(rentalId, new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenThrow(new ResourceNotFoundException("Rental not found"));

        assertThrows(ResourceNotFoundException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentalServiceThrowsOtherException_throwsPaymentProcessingException() {
        PaymentRequest request = new PaymentRequest(rentalId, new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenThrow(new RuntimeException("Network error"));

        assertThrows(PaymentProcessingException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_userNotTenant_throwsInvalidOperationException() {
        BigDecimal rentPrice = new BigDecimal("100.00");
        PaymentRequest request = new PaymentRequest(rentalId, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, UUID.randomUUID(), ownerId, "APPROVED", rentPrice);
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);

        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentalNotApprovedOrActive_throwsInvalidOperationException() {
        BigDecimal rentPrice = new BigDecimal("100.00");
        PaymentRequest request = new PaymentRequest(rentalId, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "PENDING", rentPrice);
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);

        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_amountMismatch_throwsInvalidOperationException() {
        PaymentRequest request = new PaymentRequest(rentalId, new BigDecimal("150.00"));
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "APPROVED", new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);

        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_amountNullInRequest_throwsInvalidOperationException() {
        PaymentRequest request = new PaymentRequest(rentalId, null);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "APPROVED", new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);

        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentPriceNullInDetails_throwsInvalidOperationException() {
        PaymentRequest request = new PaymentRequest(rentalId, new BigDecimal("100.00"));
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "APPROVED", null);
        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);

        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_insufficientBalance_throwsInsufficientBalanceException() {
        BigDecimal rentPrice = new BigDecimal("3000.00");
        PaymentRequest request = new PaymentRequest(rentalId, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalId, userId, ownerId, "APPROVED", rentPrice);
        UserBalance tenantBalance = new UserBalance(userId, new BigDecimal("500.00"));
        UserBalance ownerBalance = new UserBalance(ownerId, new BigDecimal("100.00"));

        when(rentalServiceClient.getRentalDetailsForPayment(rentalId)).thenReturn(rentalDetails);
        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(tenantBalance));
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.of(ownerBalance));

        assertThrows(InsufficientBalanceException.class, () -> paymentService.payForRental(userId, request));
        assertEquals(0, new BigDecimal("500.00").compareTo(tenantBalance.getBalance()));
        assertEquals(0, new BigDecimal("100.00").compareTo(ownerBalance.getBalance()));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // --- getTransactionHistory Tests ---
    @Test
    void getTransactionHistory_noFilters_callsCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Transaction> transactions = Collections.singletonList(sampleTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(transactionPage);

        Page<TransactionDto> result = paymentService.getTransactionHistory(userId, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleTransaction.getTransactionId(), result.getContent().get(0).transactionId());
        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
        verify(transactionRepository, never()).findUserTransactionsByFilter(any(), any(), any(), any(), any());
    }

    @Test
    void getTransactionHistory_withFilters_callsCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        TransactionType type = TransactionType.TOPUP;
        List<Transaction> transactions = Collections.singletonList(sampleTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        LocalDateTime expectedStartDateTime = startDate.atStartOfDay();
        LocalDateTime expectedEndDateTime = endDate.plusDays(1).atStartOfDay();

        when(transactionRepository.findUserTransactionsByFilter(userId, expectedStartDateTime, expectedEndDateTime, type, pageable))
                .thenReturn(transactionPage);

        Page<TransactionDto> result = paymentService.getTransactionHistory(userId, startDate, endDate, type, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        verify(transactionRepository).findUserTransactionsByFilter(userId, expectedStartDateTime, expectedEndDateTime, type, pageable);
    }
}