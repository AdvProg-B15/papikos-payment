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
import static org.mockito.Mockito.*; // Keep this for general any()

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
    private UUID rentalIdUuid; // Keep UUID for internal use and PaymentRequest
    private String rentalIdString; // For mocking the client call
    private UUID ownerId;
    private UserBalance userBalance;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rentalIdUuid = UUID.randomUUID();
        rentalIdString = rentalIdUuid.toString(); // String version for client mock
        ownerId = UUID.randomUUID();
        userBalance = new UserBalance(userId, new BigDecimal("1000.00"));
        userBalance.setUpdatedAt(LocalDateTime.now());
        sampleTransaction = createFullMockTransaction(UUID.randomUUID(), userId, new BigDecimal("100.00"), TransactionType.TOPUP, TransactionStatus.COMPLETED, "Sample notes");
    }

    // ... (helper methods createFullMockTransaction, createFullPaymentTransaction, createMockRentalDetails remain the same) ...
    private Transaction createFullMockTransaction(UUID txId, UUID uId, BigDecimal amt, TransactionType type, TransactionStatus stat, String notes) {
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setUserId(uId);
        tx.setAmount(amt);
        tx.setTransactionType(type);
        tx.setStatus(stat);
        tx.setNotes(notes);
        tx.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        tx.setUpdatedAt(LocalDateTime.now());
        return tx;
    }

    private Transaction createFullPaymentTransaction(UUID txId, UUID uId, BigDecimal amt, TransactionType type, TransactionStatus stat, UUID relRentalId, UUID pUserId, UUID payeeUId, String notes) {
        Transaction tx = createFullMockTransaction(txId, uId, amt, type, stat, notes);
        tx.setRelatedRentalId(relRentalId);
        tx.setPayerUserId(pUserId);
        tx.setPayeeUserId(payeeUId);
        return tx;
    }

    private RentalDetailsDto createMockRentalDetails(UUID rId, UUID tenantUId, UUID ownerUId, String stat, BigDecimal price) {
        RentalDetailsDto details = new RentalDetailsDto();
        details.setRentalId(rId);
        details.setTenantUserId(tenantUId);
        details.setOwnerUserId(ownerUId);
        details.setStatus(stat);
        details.setMonthlyRentPrice(price);
        return details;
    }


    // --- getUserBalance Tests (Unaffected by RentalServiceClient changes) ---
    @Test
    void getUserBalance_whenBalanceExists_returnsBalanceDto() {
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));
        BalanceDto result = paymentService.getUserBalance(userId);
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(0, userBalance.getBalance().compareTo(result.balance()));
        verify(userBalanceRepository).findByUserId(userId);
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void getUserBalance_whenBalanceNotExists_createsAndReturnsZeroBalanceDto() {
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BalanceDto result = paymentService.getUserBalance(userId);
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.balance()));
        verify(userBalanceRepository).save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().equals(BigDecimal.ZERO)));
    }

    // --- topUp Tests (Unaffected by RentalServiceClient changes) ---
    @Test
    void topUp_whenValidRequestAndBalanceExists_succeeds() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        UserBalance existingBalance = new UserBalance(userId, new BigDecimal("500.00"));
        Transaction savedTx = createFullMockTransaction(UUID.randomUUID(), userId, request.amount(), TransactionType.TOPUP, TransactionStatus.COMPLETED, "Internal top-up completed automatically.");
        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(existingBalance));
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(existingBalance);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
        TransactionDto result = paymentService.topUp(userId, request);
        assertNotNull(result);
        assertEquals(savedTx.getTransactionId(), result.transactionId());
        verify(userBalanceRepository).save(argThat(ub -> ub.getBalance().compareTo(new BigDecimal("600.00")) == 0));
    }

    @Test
    void topUp_whenValidRequestAndBalanceNotExists_createsBalanceAndSucceeds() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        UserBalance lockedNewBalance = new UserBalance(userId, BigDecimal.ZERO);
        Transaction savedTx = createFullMockTransaction(UUID.randomUUID(), userId, request.amount(), TransactionType.TOPUP, TransactionStatus.COMPLETED, "Internal top-up completed automatically.");
        when(userBalanceRepository.findByUserIdWithLock(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(lockedNewBalance));
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
        TransactionDto result = paymentService.topUp(userId, request);
        assertNotNull(result);
        assertEquals(savedTx.getTransactionId(), result.transactionId());
        verify(userBalanceRepository, times(2)).save(any(UserBalance.class));
    }

    @Test
    void topUp_whenNewBalanceLockFails_throwsPaymentProcessingException() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        when(userBalanceRepository.findByUserIdWithLock(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(new UserBalance(userId, BigDecimal.ZERO));
        assertThrows(PaymentProcessingException.class, () -> paymentService.topUp(userId, request));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void topUp_whenAmountIsNull_throwsInvalidOperationException() {
        assertThrows(InvalidOperationException.class, () -> paymentService.topUp(userId, new TopUpRequest(null)));
    }

    @Test
    void topUp_whenAmountIsZero_throwsInvalidOperationException() {
        assertThrows(InvalidOperationException.class, () -> paymentService.topUp(userId, new TopUpRequest(BigDecimal.ZERO)));
    }

    @Test
    void topUp_whenAmountIsNegative_throwsInvalidOperationException() {
        assertThrows(InvalidOperationException.class, () -> paymentService.topUp(userId, new TopUpRequest(new BigDecimal("-10.00"))));
    }

    @Test
    void payForRental_whenPayerBalanceNotFoundAndSufficientAfterCreation_succeedsWithZeroRent() {
        BigDecimal rentPrice = BigDecimal.ZERO;
        PaymentRequest request = new PaymentRequest(rentalIdUuid, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, userId, ownerId, "ACTIVE", rentPrice);
        UserBalance ownerBalance = new UserBalance(ownerId, new BigDecimal("100.00"));
        UserBalance createdPayerBalance = new UserBalance(userId, BigDecimal.ZERO);
        Transaction payerTxMock = createFullPaymentTransaction(UUID.randomUUID(), userId, rentPrice, TransactionType.PAYMENT, TransactionStatus.COMPLETED, rentalIdUuid, userId, ownerId, "Payment sent for rental " + rentalIdUuid);

        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>();
        mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper); // Corrected

        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty()).thenReturn(Optional.of(createdPayerBalance));
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.of(ownerBalance));
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(payerTxMock);

        TransactionDto result = paymentService.payForRental(userId, request);
        assertNotNull(result);
        verify(userBalanceRepository, times(3)).save(any(UserBalance.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void payForRental_whenPayerBalanceNotFound_createsAndProceedsButInsufficient() {
        BigDecimal rentPrice = new BigDecimal("300.00");
        PaymentRequest request = new PaymentRequest(rentalIdUuid, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, userId, ownerId, "ACTIVE", rentPrice);
        UserBalance ownerBalance = new UserBalance(ownerId, new BigDecimal("100.00"));
        UserBalance createdPayerBalance = new UserBalance(userId, BigDecimal.ZERO);

        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>();
        mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper); // Corrected

        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty()).thenReturn(Optional.of(createdPayerBalance));
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.of(ownerBalance));
        when(userBalanceRepository.save(argThat(ub -> ub.getUserId().equals(userId) && ub.getBalance().equals(BigDecimal.ZERO)))).thenReturn(createdPayerBalance);

        assertThrows(InsufficientBalanceException.class, () -> paymentService.payForRental(userId, request));
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void payForRental_rentalServiceReturnsWrapperWithNullData_throwsPaymentProcessingException() {
        PaymentRequest request = new PaymentRequest(rentalIdUuid, new BigDecimal("100.00"));
        RentalResponseWrapper<RentalDetailsDto> mockWrapperWithNullData = new RentalResponseWrapper<>();
        mockWrapperWithNullData.setData(null); // Simulate wrapper has null data
        mockWrapperWithNullData.setStatus(200);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapperWithNullData); // Corrected

        assertThrows(PaymentProcessingException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentalServiceThrowsResourceNotFound_rethrowsException() {
        PaymentRequest request = new PaymentRequest(rentalIdUuid, new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenThrow(new ResourceNotFoundException("Rental not found")); // Corrected
        assertThrows(ResourceNotFoundException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentalServiceThrowsOtherException_throwsPaymentProcessingException() {
        PaymentRequest request = new PaymentRequest(rentalIdUuid, new BigDecimal("100.00"));
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenThrow(new RuntimeException("Network error")); // Corrected
        assertThrows(PaymentProcessingException.class, () -> paymentService.payForRental(userId, request));
    }

    // ... (other payForRental validation tests - ensure they use eq(rentalIdString) for the client mock) ...
    @Test
    void payForRental_userNotTenant_throwsInvalidOperationException() {
        PaymentRequest request = new PaymentRequest(rentalIdUuid, new BigDecimal("100.00"));
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, UUID.randomUUID(), ownerId, "APPROVED", new BigDecimal("100.00"));
        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>(); mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper);
        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_rentalNotApprovedOrActive_throwsInvalidOperationException() {
        PaymentRequest request = new PaymentRequest(rentalIdUuid, new BigDecimal("100.00"));
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, userId, ownerId, "PENDING", new BigDecimal("100.00"));
        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>(); mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper);
        assertThrows(InvalidOperationException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_insufficientBalance_throwsInsufficientBalanceException() {
        BigDecimal rentPrice = new BigDecimal("3000.00");
        PaymentRequest request = new PaymentRequest(rentalIdUuid, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, userId, ownerId, "APPROVED", rentPrice);
        UserBalance tenantBalance = new UserBalance(userId, new BigDecimal("500.00"));
        UserBalance ownerBalance = new UserBalance(ownerId, new BigDecimal("100.00"));
        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>(); mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper);
        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(tenantBalance));
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.of(ownerBalance));
        assertThrows(InsufficientBalanceException.class, () -> paymentService.payForRental(userId, request));
    }

    // --- getTransactionHistory Tests (Unaffected) ---
    @Test
    void getTransactionHistory_noFilters_callsCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction mappedTx = createFullMockTransaction(sampleTransaction.getTransactionId(), userId, sampleTransaction.getAmount(), sampleTransaction.getTransactionType(), sampleTransaction.getStatus(), sampleTransaction.getNotes());
        List<Transaction> transactions = Collections.singletonList(mappedTx);
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
        Transaction mappedTx = createFullMockTransaction(sampleTransaction.getTransactionId(), userId, sampleTransaction.getAmount(), type, sampleTransaction.getStatus(), sampleTransaction.getNotes());
        List<Transaction> transactions = Collections.singletonList(mappedTx);
        Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        LocalDateTime expectedStartDateTime = startDate.atStartOfDay();
        LocalDateTime expectedEndDateTime = endDate.plusDays(1).atStartOfDay();

        when(transactionRepository.findUserTransactionsByFilter(userId, expectedStartDateTime, expectedEndDateTime, type, pageable))
                .thenReturn(transactionPage);

        Page<TransactionDto> result = paymentService.getTransactionHistory(userId, startDate, endDate, type, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleTransaction.getTransactionId(), result.getContent().get(0).transactionId());
        verify(transactionRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        verify(transactionRepository).findUserTransactionsByFilter(userId, expectedStartDateTime, expectedEndDateTime, type, pageable);
    }

    // --- Additional Edge Case Tests for performInternalTransfer (indirectly via payForRental) ---
    @Test
    void payForRental_whenPayerBalanceIsNullAfterCreationInPerformInternalTransfer_throwsPaymentProcessingException() {
        BigDecimal rentPrice = new BigDecimal("50.00");
        PaymentRequest request = new PaymentRequest(rentalIdUuid, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, userId, ownerId, "APPROVED", rentPrice);

        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>();mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper); // Corrected

        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty()).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(new UserBalance(userId, BigDecimal.ZERO));
        assertThrows(PaymentProcessingException.class, () -> paymentService.payForRental(userId, request));
    }

    @Test
    void payForRental_whenPayeeBalanceIsNullAfterCreationInPerformInternalTransfer_throwsPaymentProcessingException() {
        BigDecimal rentPrice = new BigDecimal("50.00");
        PaymentRequest request = new PaymentRequest(rentalIdUuid, rentPrice);
        RentalDetailsDto rentalDetails = createMockRentalDetails(rentalIdUuid, userId, ownerId, "APPROVED", rentPrice);
        UserBalance payerBalance = new UserBalance(userId, new BigDecimal("100.00"));

        RentalResponseWrapper<RentalDetailsDto> mockWrapper = new RentalResponseWrapper<>();mockWrapper.setData(rentalDetails);
        when(rentalServiceClient.getRentalDetailsForPayment(eq(rentalIdString))).thenReturn(mockWrapper); // Corrected

        when(userBalanceRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(payerBalance));
        when(userBalanceRepository.findByUserIdWithLock(ownerId)).thenReturn(Optional.empty()).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(new UserBalance(ownerId, BigDecimal.ZERO));
        assertThrows(PaymentProcessingException.class, () -> paymentService.payForRental(userId, request));
    }
}