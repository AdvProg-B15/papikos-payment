package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.client.AuthServiceClient; // Mock interface
import id.ac.ui.cs.advprog.papikos.payment.client.RentalServiceClient; // Mock interface
import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.Transaction;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionStatus;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.entity.UserBalance;
import id.ac.ui.cs.advprog.papikos.payment.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.papikos.payment.exception.InvalidOperationException;
import id.ac.ui.cs.advprog.papikos.payment.exception.PaymentProcessingException;
import id.ac.ui.cs.advprog.papikos.payment.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserBalanceRepository userBalanceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthServiceClient authServiceClient; // To check if user exists

    @Mock
    private RentalServiceClient rentalServiceClient; // To get rental details

    // We won't mock the gateway client here, assume initiateTopUp handles that detail internally
    // For confirmTopUp, we assume validation happens before calling the service method

    @InjectMocks
    private PaymentServiceImpl paymentService; // Use implementation class

    private final Long TENANT_USER_ID = 1L;
    private final Long OWNER_USER_ID = 2L;
    private final Long RENTAL_ID = 101L;
    private final BigDecimal RENTAL_PRICE = new BigDecimal("500.00");
    private UserBalance tenantBalance;
    private UserBalance ownerBalance;
    private RentalDetailsDto rentalDetailsDto; // DTO from Rental Service

    @BeforeEach
    void setUp() {
        tenantBalance = new UserBalance(TENANT_USER_ID, new BigDecimal("1000.00"));
        tenantBalance.setUpdatedAt(LocalDateTime.now()); // JPA Auditing would normally handle this

        ownerBalance = new UserBalance(OWNER_USER_ID, new BigDecimal("200.00"));
        ownerBalance.setUpdatedAt(LocalDateTime.now());

        rentalDetailsDto = new RentalDetailsDto();
        rentalDetailsDto.setRentalId(RENTAL_ID);
        rentalDetailsDto.setTenantUserId(TENANT_USER_ID);
        rentalDetailsDto.setOwnerUserId(OWNER_USER_ID);
        rentalDetailsDto.setStatus("APPROVED"); // Important status
        rentalDetailsDto.setMonthlyRentPrice(RENTAL_PRICE);
    }

    // --- Get Balance ---
    @Test
    @DisplayName("Get User Balance - Success")
    void getUserBalance_Success() {
        when(userBalanceRepository.findByUserId(TENANT_USER_ID)).thenReturn(Optional.of(tenantBalance));

        BalanceDto result = paymentService.getUserBalance(TENANT_USER_ID);

        assertNotNull(result);
        assertEquals(TENANT_USER_ID, result.getUserId());
        assertEquals(0, tenantBalance.getBalance().compareTo(result.getBalance())); // Use compareTo for BigDecimal
        verify(userBalanceRepository).findByUserId(TENANT_USER_ID);
    }

    @Test
    @DisplayName("Get User Balance - First Time (Create and Return Zero)")
    void getUserBalance_FirstTime_CreatesAndReturnsZero() {
        when(userBalanceRepository.findByUserId(TENANT_USER_ID)).thenReturn(Optional.empty());
        // Mock the save operation to return a new balance object
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> {
            UserBalance newBalance = invocation.getArgument(0);
            // Simulate setting the timestamp on save
            newBalance.setUpdatedAt(LocalDateTime.now());
            return newBalance;
        });
        // Mock auth service to confirm user exists when creating balance
        when(authServiceClient.userExists(TENANT_USER_ID)).thenReturn(true);


        BalanceDto result = paymentService.getUserBalance(TENANT_USER_ID);

        assertNotNull(result);
        assertEquals(TENANT_USER_ID, result.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalance())); // Should be zero
        verify(userBalanceRepository).findByUserId(TENANT_USER_ID);
        verify(userBalanceRepository).save(argThat(ub -> ub.getUserId().equals(TENANT_USER_ID) && ub.getBalance().equals(BigDecimal.ZERO)));
        verify(authServiceClient).userExists(TENANT_USER_ID);
    }

    @Test
    @DisplayName("Get User Balance - User Not Found by Auth Service")
    void getUserBalance_UserNotFound_ThrowsResourceNotFound() {
        when(userBalanceRepository.findByUserId(TENANT_USER_ID)).thenReturn(Optional.empty());
        when(authServiceClient.userExists(TENANT_USER_ID)).thenReturn(false); // Simulate user doesn't exist

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.getUserBalance(TENANT_USER_ID);
        });

        verify(userBalanceRepository).findByUserId(TENANT_USER_ID);
        verify(authServiceClient).userExists(TENANT_USER_ID);
        verify(userBalanceRepository, never()).save(any()); // Should not attempt to save balance if user doesn't exist
    }


    // --- Initiate Top Up ---
    @Test
    @DisplayName("Initiate TopUp - Success")
    void initiateTopUp_Success() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        long expectedTransactionId = 1L; // Assume this ID is generated

        // Capture the transaction saved
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(transactionCaptor.capture())).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setTransactionId(expectedTransactionId); // Simulate ID generation
            tx.setCreatedAt(LocalDateTime.now()); // Simulate timestamp
            return tx;
        });

        // This method should return data needed for the gateway (e.g., redirect URL, payment code)
        // We'll assume it returns a simple response DTO containing the internal transaction ID
        TopUpInitiationResponse response = paymentService.initiateTopUp(TENANT_USER_ID, request);

        assertNotNull(response);
        assertEquals(expectedTransactionId, response.getTransactionId());
        // Assertions on the captured transaction
        Transaction savedTx = transactionCaptor.getValue();
        assertEquals(TENANT_USER_ID, savedTx.getUserId());
        assertEquals(TransactionType.TOPUP, savedTx.getTransactionType());
        assertEquals(TransactionStatus.PENDING, savedTx.getStatus());
        assertEquals(0, request.getAmount().compareTo(savedTx.getAmount()));

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Initiate TopUp - Invalid Amount (Zero)")
    void initiateTopUp_InvalidAmount_Zero() {
        TopUpRequest request = new TopUpRequest(BigDecimal.ZERO);

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.initiateTopUp(TENANT_USER_ID, request);
        });
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Initiate TopUp - Invalid Amount (Negative)")
    void initiateTopUp_InvalidAmount_Negative() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("-10.00"));

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.initiateTopUp(TENANT_USER_ID, request);
        });
        verify(transactionRepository, never()).save(any());
    }


    // --- Confirm Top Up ---
    @Test
    @DisplayName("Confirm TopUp - Success")
    void confirmTopUp_Success() {
        long transactionId = 1L;
        BigDecimal topUpAmount = new BigDecimal("100.00");
        Transaction pendingTx = new Transaction();
        pendingTx.setTransactionId(transactionId);
        pendingTx.setUserId(TENANT_USER_ID);
        pendingTx.setAmount(topUpAmount);
        pendingTx.setStatus(TransactionStatus.PENDING);
        pendingTx.setTransactionType(TransactionType.TOPUP);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTx));
        when(userBalanceRepository.findByUserIdWithLock(TENANT_USER_ID)).thenReturn(Optional.of(tenantBalance)); // Use lock for update
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(tenantBalance); // Assume save returns updated
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTx); // Assume save returns updated

        paymentService.confirmTopUp(transactionId);

        // Verify balance increased
        assertEquals(0, new BigDecimal("1100.00").compareTo(tenantBalance.getBalance()));
        // Verify transaction status updated
        assertEquals(TransactionStatus.COMPLETED, pendingTx.getStatus());

        verify(transactionRepository).findById(transactionId);
        verify(userBalanceRepository).findByUserIdWithLock(TENANT_USER_ID);
        verify(userBalanceRepository).save(tenantBalance);
        verify(transactionRepository).save(pendingTx);
    }

    @Test
    @DisplayName("Confirm TopUp - Transaction Not Found")
    void confirmTopUp_TransactionNotFound() {
        long transactionId = 99L;
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.confirmTopUp(transactionId);
        });

        verify(transactionRepository).findById(transactionId);
        verify(userBalanceRepository, never()).findByUserIdWithLock(anyLong());
        verify(userBalanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Confirm TopUp - Transaction Not Pending")
    void confirmTopUp_TransactionNotPending() {
        long transactionId = 1L;
        BigDecimal topUpAmount = new BigDecimal("100.00");
        Transaction completedTx = new Transaction();
        completedTx.setTransactionId(transactionId);
        completedTx.setUserId(TENANT_USER_ID);
        completedTx.setAmount(topUpAmount);
        completedTx.setStatus(TransactionStatus.COMPLETED); // Already completed
        completedTx.setTransactionType(TransactionType.TOPUP);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(completedTx));

        // Should likely log a warning and do nothing, or throw specific exception
        assertThrows(InvalidOperationException.class, () -> {
            paymentService.confirmTopUp(transactionId);
        }, "Should throw InvalidOperationException if transaction is not pending");

        verify(transactionRepository).findById(transactionId);
        verify(userBalanceRepository, never()).findByUserIdWithLock(anyLong());
        verify(userBalanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any()); // Don't save if already completed
    }

    @Test
    @DisplayName("Confirm TopUp - User Balance Not Found (Should ideally not happen if user exists)")
    void confirmTopUp_UserBalanceNotFound() {
        long transactionId = 1L;
        BigDecimal topUpAmount = new BigDecimal("100.00");
        Transaction pendingTx = new Transaction();
        pendingTx.setTransactionId(transactionId);
        pendingTx.setUserId(TENANT_USER_ID);
        pendingTx.setAmount(topUpAmount);
        pendingTx.setStatus(TransactionStatus.PENDING);
        pendingTx.setTransactionType(TransactionType.TOPUP);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTx));
        when(userBalanceRepository.findByUserIdWithLock(TENANT_USER_ID)).thenReturn(Optional.empty()); // Balance missing

        // This indicates a data inconsistency. Should throw an internal error.
        assertThrows(PaymentProcessingException.class, () -> {
            paymentService.confirmTopUp(transactionId);
        });

        verify(transactionRepository).findById(transactionId);
        verify(userBalanceRepository).findByUserIdWithLock(TENANT_USER_ID);
        verify(userBalanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // --- Pay for Rental ---
    @Test
    @DisplayName("Pay For Rental - Success")
    void payForRental_Success() {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE);

        // Mock interactions
        when(rentalServiceClient.getRentalDetailsForPayment(RENTAL_ID)).thenReturn(rentalDetailsDto);
        when(userBalanceRepository.findByUserIdWithLock(TENANT_USER_ID)).thenReturn(Optional.of(tenantBalance));
        when(userBalanceRepository.findByUserIdWithLock(OWNER_USER_ID)).thenReturn(Optional.of(ownerBalance));

        // Capture saved transactions
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(transactionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0)); // Just return the saved obj

        // Execute
        TransactionDto resultDto = paymentService.payForRental(TENANT_USER_ID, request);

        // Assertions
        assertNotNull(resultDto);
        assertEquals(TransactionType.PAYMENT, resultDto.getTransactionType());
        assertEquals(TransactionStatus.COMPLETED, resultDto.getStatus());
        assertEquals(0, RENTAL_PRICE.compareTo(resultDto.getAmount()));
        assertEquals(TENANT_USER_ID, resultDto.getPayerUserId());
        assertEquals(OWNER_USER_ID, resultDto.getPayeeUserId());

        // Verify balance changes
        assertEquals(0, new BigDecimal("500.00").compareTo(tenantBalance.getBalance())); // 1000 - 500
        assertEquals(0, new BigDecimal("700.00").compareTo(ownerBalance.getBalance())); // 200 + 500

        // Verify mocks
        verify(rentalServiceClient).getRentalDetailsForPayment(RENTAL_ID);
        verify(userBalanceRepository).findByUserIdWithLock(TENANT_USER_ID);
        verify(userBalanceRepository).findByUserIdWithLock(OWNER_USER_ID);
        verify(userBalanceRepository, times(2)).save(any(UserBalance.class)); // Both balances saved
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // Tenant and Owner transactions saved

        // Check saved transactions (optional deep check)
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertThat(savedTransactions).hasSize(2);
        // ... check details of each saved transaction if necessary
    }


    @Test
    @DisplayName("Pay For Rental - Insufficient Balance")
    void payForRental_InsufficientBalance() {
        tenantBalance.setBalance(new BigDecimal("400.00")); // Not enough balance
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE);

        when(rentalServiceClient.getRentalDetailsForPayment(RENTAL_ID)).thenReturn(rentalDetailsDto);
        when(userBalanceRepository.findByUserIdWithLock(TENANT_USER_ID)).thenReturn(Optional.of(tenantBalance));
        // No need to mock owner balance fetch if tenant fails first

        assertThrows(InsufficientBalanceException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request);
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(RENTAL_ID);
        verify(userBalanceRepository).findByUserIdWithLock(TENANT_USER_ID);
        verify(userBalanceRepository, never()).findByUserIdWithLock(OWNER_USER_ID);
        verify(userBalanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Rental Not Found")
    void payForRental_RentalNotFound() {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE);
        when(rentalServiceClient.getRentalDetailsForPayment(RENTAL_ID))
                .thenThrow(new ResourceNotFoundException("Rental not found")); // Simulate rental service throwing error

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request);
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(RENTAL_ID);
        verify(userBalanceRepository, never()).findByUserIdWithLock(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Rental Not Approved")
    void payForRental_RentalNotApproved() {
        rentalDetailsDto.setStatus("PENDING_APPROVAL"); // Set wrong status
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE);

        when(rentalServiceClient.getRentalDetailsForPayment(RENTAL_ID)).thenReturn(rentalDetailsDto);

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request);
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(RENTAL_ID);
        verify(userBalanceRepository, never()).findByUserIdWithLock(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Wrong Tenant")
    void payForRental_WrongTenant() {
        long wrongTenantId = 99L;
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE);

        when(rentalServiceClient.getRentalDetailsForPayment(RENTAL_ID)).thenReturn(rentalDetailsDto); // Contains correct tenant ID

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.payForRental(wrongTenantId, request); // Call with wrong ID
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(RENTAL_ID);
        verify(userBalanceRepository, never()).findByUserIdWithLock(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Amount Mismatch")
    void payForRental_AmountMismatch() {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, new BigDecimal("499.00")); // Wrong amount in request

        when(rentalServiceClient.getRentalDetailsForPayment(RENTAL_ID)).thenReturn(rentalDetailsDto); // Contains correct price

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request);
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(RENTAL_ID);
        verify(userBalanceRepository, never()).findByUserIdWithLock(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    // --- Get Transaction History ---
    @Test
    @DisplayName("Get Transaction History - Success")
    void getTransactionHistory_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx1 = new Transaction(); // Setup some transactions
        tx1.setTransactionId(1L);
        tx1.setUserId(TENANT_USER_ID);
        tx1.setAmount(BigDecimal.TEN);
        tx1.setStatus(TransactionStatus.COMPLETED);
        tx1.setTransactionType(TransactionType.TOPUP);
        tx1.setCreatedAt(LocalDateTime.now().minusDays(1));

        Transaction tx2 = new Transaction();
        tx2.setTransactionId(2L);
        tx2.setUserId(TENANT_USER_ID);
        tx2.setAmount(BigDecimal.ONE);
        tx2.setStatus(TransactionStatus.COMPLETED);
        tx2.setTransactionType(TransactionType.PAYMENT);
        tx2.setCreatedAt(LocalDateTime.now());

        Page<Transaction> transactionPage = new PageImpl<>(List.of(tx2, tx1), pageable, 2); // Ensure order is correct (desc)

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(TENANT_USER_ID, pageable))
                .thenReturn(transactionPage);

        Page<TransactionDto> resultPage = paymentService.getTransactionHistory(TENANT_USER_ID, null, null, null, pageable);

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(1, resultPage.getTotalPages());
        assertEquals(2, resultPage.getContent().size());
        assertEquals(tx2.getTransactionId(), resultPage.getContent().get(0).getTransactionId()); // Verify order
        assertEquals(tx1.getTransactionId(), resultPage.getContent().get(1).getTransactionId());

        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(TENANT_USER_ID, pageable);
        // Verify filter methods NOT called
        verify(transactionRepository, never()).findUserTransactionsByFilter(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Get Transaction History - With Filters")
    void getTransactionHistory_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now();
        TransactionType type = TransactionType.PAYMENT;

        // Assume only tx2 matches filter criteria
        Transaction tx2 = new Transaction();
        tx2.setTransactionId(2L);
        tx2.setUserId(TENANT_USER_ID);
        tx2.setAmount(BigDecimal.ONE);
        tx2.setStatus(TransactionStatus.COMPLETED);
        tx2.setTransactionType(type);
        tx2.setCreatedAt(LocalDateTime.now());

        Page<Transaction> transactionPage = new PageImpl<>(List.of(tx2), pageable, 1);

        // Mock the specific filter query method
        when(transactionRepository.findUserTransactionsByFilter(
                eq(TENANT_USER_ID),
                eq(startDate.atStartOfDay()), // Convert LocalDate to LocalDateTime
                eq(endDate.plusDays(1).atStartOfDay()), // End date is exclusive, add 1 day
                eq(type),
                eq(pageable)))
                .thenReturn(transactionPage);


        Page<TransactionDto> resultPage = paymentService.getTransactionHistory(TENANT_USER_ID, startDate, endDate, type, pageable);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(1, resultPage.getContent().size());
        assertEquals(tx2.getTransactionId(), resultPage.getContent().get(0).getTransactionId());
        assertEquals(type, resultPage.getContent().get(0).getTransactionType());

        verify(transactionRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any()); // Default should not be called
        verify(transactionRepository).findUserTransactionsByFilter(any(), any(), any(), any(), any());
    }

}