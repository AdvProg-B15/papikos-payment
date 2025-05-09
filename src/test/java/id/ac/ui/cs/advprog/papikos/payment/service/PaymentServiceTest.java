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
import java.util.UUID; // Import UUID

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
// Import eq specifically for UUID if needed, or rely on direct value matching
import static org.mockito.ArgumentMatchers.eq;
// DO NOT import anyLong()
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private UserBalanceRepository userBalanceRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private RentalServiceClient rentalServiceClient;

    @InjectMocks private PaymentServiceImpl paymentService;

    // Define test UUIDs
    private final UUID TENANT_USER_ID = UUID.fromString("a1b7a39a-8f8f-4f19-a5e3-8d9c1b8a9b5a");
    private final UUID OWNER_USER_ID = UUID.fromString("b2c8b40b-9g9g-5g20-a6f5-7e0c2b7a8a4b");
    private final UUID RENTAL_ID = UUID.fromString("c1d9b54c-1e1e-4c20-b6f4-9e0d2c9b0c6b");
    private final UUID TRANSACTION_ID = UUID.fromString("d2e0c65d-2f2f-6d31-c7g6-0f1e3d0c1d7d");
    private final UUID NEW_TRANSACTION_ID = UUID.fromString("e3f1d76e-3a3a-7e42-d8h7-1g2f4e1d2e8e");

    private final BigDecimal RENTAL_PRICE = new BigDecimal("500.00");
    private UserBalance tenantBalance;
    private UserBalance ownerBalance;
    private RentalDetailsDto rentalDetailsDto;

    @BeforeEach
    void setUp() {
        // Use UUIDs in setup
        tenantBalance = new UserBalance(TENANT_USER_ID, new BigDecimal("1000.00"));
        tenantBalance.setUpdatedAt(LocalDateTime.now());

        ownerBalance = new UserBalance(OWNER_USER_ID, new BigDecimal("200.00"));
        ownerBalance.setUpdatedAt(LocalDateTime.now());

        rentalDetailsDto = new RentalDetailsDto();
        rentalDetailsDto.setRentalId(RENTAL_ID);
        rentalDetailsDto.setTenantUserId(TENANT_USER_ID);
        rentalDetailsDto.setOwnerUserId(OWNER_USER_ID);
        rentalDetailsDto.setStatus("APPROVED");
        rentalDetailsDto.setMonthlyRentPrice(RENTAL_PRICE);
    }

    // --- Get Balance ---
    @Test
    @DisplayName("Get User Balance - Success")
    void getUserBalance_Success() {
        // Mock with specific UUID
        when(userBalanceRepository.findByUserId(eq(TENANT_USER_ID))).thenReturn(Optional.of(tenantBalance));

        BalanceDto result = paymentService.getUserBalance(TENANT_USER_ID);

        assertNotNull(result);
        assertEquals(TENANT_USER_ID, result.userId()); // Assert UUID equality
        assertEquals(0, tenantBalance.getBalance().compareTo(result.balance()));
        // Verify with specific UUID
        verify(userBalanceRepository).findByUserId(eq(TENANT_USER_ID));
    }

    @Test
    @DisplayName("Get User Balance - First Time (Create and Return Zero)")
    void getUserBalance_FirstTime_CreatesAndReturnsZero() {
        // Mock with specific UUID
        when(userBalanceRepository.findByUserId(eq(TENANT_USER_ID))).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> {
            UserBalance newBalance = invocation.getArgument(0);
            newBalance.setUserId(TENANT_USER_ID); // Ensure correct ID is set if not already
            newBalance.setUpdatedAt(LocalDateTime.now());
            return newBalance;
        });
        // Mock auth service check with specific UUID
        when(authServiceClient.userExists(eq(TENANT_USER_ID))).thenReturn(true);


        BalanceDto result = paymentService.getUserBalance(TENANT_USER_ID);

        assertNotNull(result);
        assertEquals(TENANT_USER_ID, result.userId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.balance()));
        // Verify findByUserId with specific UUID
        verify(userBalanceRepository).findByUserId(eq(TENANT_USER_ID));
        // Verify save with argument captor or specific checks if needed
        verify(userBalanceRepository).save(argThat(ub -> ub.getUserId().equals(TENANT_USER_ID) && ub.getBalance().equals(BigDecimal.ZERO)));
        // Verify auth check with specific UUID
        verify(authServiceClient).userExists(eq(TENANT_USER_ID));
    }

    @Test
    @DisplayName("Get User Balance - User Not Found by Auth Service")
    void getUserBalance_UserNotFound_ThrowsResourceNotFound() {
        // Mock with specific UUID
        when(userBalanceRepository.findByUserId(eq(TENANT_USER_ID))).thenReturn(Optional.empty());
        // Mock auth service check returning false for the specific UUID
        when(authServiceClient.userExists(eq(TENANT_USER_ID))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.getUserBalance(TENANT_USER_ID);
        });

        verify(userBalanceRepository).findByUserId(eq(TENANT_USER_ID));
        verify(authServiceClient).userExists(eq(TENANT_USER_ID));
        verify(userBalanceRepository, never()).save(any());
    }

    // --- Initiate Top Up ---
    @Test
    @DisplayName("Initiate And Complete TopUp - Success")
    void initiateAndCompleteTopUp_Success() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        UUID userId = TENANT_USER_ID; // Assuming TENANT_USER_ID is a defined UUID
        UserBalance existingBalance = new UserBalance(userId, new BigDecimal("50.00"));

        // Mock finding the user's balance (with lock)
        when(userBalanceRepository.findByUserIdWithLock(eq(userId))).thenReturn(Optional.of(existingBalance));
        // Mock saving the updated balance
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Mock saving the transaction
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setTransactionId(UUID.randomUUID()); // Simulate ID generation
            tx.setCreatedAt(LocalDateTime.now());
            tx.setUpdatedAt(LocalDateTime.now());
            return tx;
        });

        TransactionDto resultDto = paymentService.TopUp(userId, request);

        assertNotNull(resultDto);
        assertEquals(userId, resultDto.userId());
        assertEquals(TransactionType.TOPUP, resultDto.transactionType());
        assertEquals(TransactionStatus.COMPLETED, resultDto.status()); // Check for COMPLETED
        assertEquals(0, request.amount().compareTo(resultDto.amount()));

        // Verify balance was updated
        // Old balance was 50.00, top-up 100.00 -> new balance 150.00
        assertEquals(0, new BigDecimal("150.00").compareTo(existingBalance.getBalance()));

        verify(userBalanceRepository).findByUserIdWithLock(eq(userId));
        verify(userBalanceRepository).save(existingBalance); // Verify balance save
        verify(transactionRepository).save(argThat(tx -> // Verify transaction save
                tx.getUserId().equals(userId) &&
                        tx.getAmount().compareTo(request.amount()) == 0 &&
                        tx.getStatus() == TransactionStatus.COMPLETED &&
                        tx.getTransactionType() == TransactionType.TOPUP
        ));
    }

    @Test
    @DisplayName("Initiate And Complete TopUp - User Balance Record Not Found (But User Exists - Creates Balance)")
    void initiateAndCompleteTopUp_UserBalanceNotFoundButUserExists() {
        TopUpRequest request = new TopUpRequest(new BigDecimal("100.00"));
        UUID userId = TENANT_USER_ID;

        when(userBalanceRepository.findByUserIdWithLock(eq(userId))).thenReturn(Optional.empty()); // No balance initially
        when(authServiceClient.userExists(eq(userId))).thenReturn(true); // User exists

        // Mock saving a *new* balance (first arg) and then the *updated* balance (second arg)
        ArgumentCaptor<UserBalance> balanceCaptor = ArgumentCaptor.forClass(UserBalance.class);
        when(userBalanceRepository.save(balanceCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock saving the transaction
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setTransactionId(UUID.randomUUID());
            return tx;
        });

        TransactionDto resultDto = paymentService.TopUp(userId, request);

        assertNotNull(resultDto);
        assertEquals(TransactionStatus.COMPLETED, resultDto.status());
        assertEquals(0, request.amount().compareTo(resultDto.amount()));

        // Verify balance was created and then updated
        verify(userBalanceRepository).findByUserIdWithLock(eq(userId));
        verify(authServiceClient).userExists(eq(userId));
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class)); // Only one save needed as it's done in one transaction

        List<UserBalance> capturedBalances = balanceCaptor.getAllValues();
        // The single captured balance should now have the topped-up amount
        assertThat(capturedBalances).hasSize(1);
        assertEquals(0, request.amount().compareTo(capturedBalances.get(0).getBalance()));


        verify(transactionRepository).save(any(Transaction.class));
    }


    // --- Pay for Rental ---
    @Test
    @DisplayName("Pay For Rental - Success")
    void payForRental_Success() {
        // Request DTO now uses UUID
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE);

        // Mock external calls with UUIDs
        when(rentalServiceClient.getRentalDetailsForPayment(eq(RENTAL_ID))).thenReturn(rentalDetailsDto);
        when(userBalanceRepository.findByUserIdWithLock(eq(TENANT_USER_ID))).thenReturn(Optional.of(tenantBalance));
        when(userBalanceRepository.findByUserIdWithLock(eq(OWNER_USER_ID))).thenReturn(Optional.of(ownerBalance));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        // Mock save to assign a new UUID
        when(transactionRepository.save(transactionCaptor.capture())).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            // Assign different IDs based on payer/payee if needed for verification
            if (tx.getUserId().equals(TENANT_USER_ID)) {
                tx.setTransactionId(TRANSACTION_ID);
            } else {
                tx.setTransactionId(NEW_TRANSACTION_ID);
            }
            return tx;
        });

        // Execute service call with UUID
        TransactionDto resultDto = paymentService.payForRental(TENANT_USER_ID, request);

        assertNotNull(resultDto);
        assertEquals(TransactionType.PAYMENT, resultDto.transactionType());
        assertEquals(TransactionStatus.COMPLETED, resultDto.status());
        assertEquals(0, RENTAL_PRICE.compareTo(resultDto.amount()));
        assertEquals(TENANT_USER_ID, resultDto.payerUserId()); // Assert UUIDs
        assertEquals(OWNER_USER_ID, resultDto.payeeUserId());
        assertEquals(TRANSACTION_ID, resultDto.transactionId()); // Ensure payer's tx ID is returned

        assertEquals(0, new BigDecimal("500.00").compareTo(tenantBalance.getBalance()));
        assertEquals(0, new BigDecimal("700.00").compareTo(ownerBalance.getBalance()));

        // Verify external calls with UUIDs
        verify(rentalServiceClient).getRentalDetailsForPayment(eq(RENTAL_ID));
        verify(userBalanceRepository).findByUserIdWithLock(eq(TENANT_USER_ID));
        verify(userBalanceRepository).findByUserIdWithLock(eq(OWNER_USER_ID));
        verify(userBalanceRepository, times(2)).save(any(UserBalance.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));

        // Optional: Check captured transactions more deeply
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertThat(savedTransactions).hasSize(2);
        // Find payer and payee transactions if needed for detailed checks
    }


    @Test
    @DisplayName("Pay For Rental - Insufficient Balance")
    void payForRental_InsufficientBalance() {
        tenantBalance.setBalance(new BigDecimal("400.00"));
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE); // Use UUID

        when(rentalServiceClient.getRentalDetailsForPayment(eq(RENTAL_ID))).thenReturn(rentalDetailsDto); // Mock with UUID
        when(userBalanceRepository.findByUserIdWithLock(eq(TENANT_USER_ID))).thenReturn(Optional.of(tenantBalance)); // Mock with UUID

        assertThrows(InsufficientBalanceException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request); // Call with UUID
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(eq(RENTAL_ID)); // Verify with UUID
        verify(userBalanceRepository).findByUserIdWithLock(eq(TENANT_USER_ID)); // Verify with UUID
        verify(userBalanceRepository, never()).findByUserIdWithLock(eq(OWNER_USER_ID)); // No owner lock needed
        verify(userBalanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Rental Not Found")
    void payForRental_RentalNotFound() {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE); // Use UUID
        // Mock rental client call with UUID to throw exception
        when(rentalServiceClient.getRentalDetailsForPayment(eq(RENTAL_ID)))
                .thenThrow(new ResourceNotFoundException("Rental not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request); // Call with UUID
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(eq(RENTAL_ID)); // Verify with UUID
        verify(userBalanceRepository, never()).findByUserIdWithLock(any(UUID.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Rental Not Approved")
    void payForRental_RentalNotApproved() {
        rentalDetailsDto.setStatus("PENDING_APPROVAL");
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE); // Use UUID

        when(rentalServiceClient.getRentalDetailsForPayment(eq(RENTAL_ID))).thenReturn(rentalDetailsDto); // Mock with UUID

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request); // Call with UUID
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(eq(RENTAL_ID)); // Verify with UUID
        verify(userBalanceRepository, never()).findByUserIdWithLock(any(UUID.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Wrong Tenant")
    void payForRental_WrongTenant() {
        UUID wrongTenantId = UUID.randomUUID(); // Different UUID
        PaymentRequest request = new PaymentRequest(RENTAL_ID, RENTAL_PRICE); // Use UUID

        when(rentalServiceClient.getRentalDetailsForPayment(eq(RENTAL_ID))).thenReturn(rentalDetailsDto); // Mock with UUID

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.payForRental(wrongTenantId, request); // Call with wrong tenant UUID
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(eq(RENTAL_ID)); // Verify with UUID
        verify(userBalanceRepository, never()).findByUserIdWithLock(any(UUID.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pay For Rental - Amount Mismatch")
    void payForRental_AmountMismatch() {
        // Request DTO uses UUID
        PaymentRequest request = new PaymentRequest(RENTAL_ID, new BigDecimal("499.00"));

        when(rentalServiceClient.getRentalDetailsForPayment(eq(RENTAL_ID))).thenReturn(rentalDetailsDto); // Mock with UUID

        assertThrows(InvalidOperationException.class, () -> {
            paymentService.payForRental(TENANT_USER_ID, request); // Call with UUID
        });

        verify(rentalServiceClient).getRentalDetailsForPayment(eq(RENTAL_ID)); // Verify with UUID
        verify(userBalanceRepository, never()).findByUserIdWithLock(any(UUID.class));
        verify(transactionRepository, never()).save(any());
    }

    // --- Get Transaction History ---
    @Test
    @DisplayName("Get Transaction History - Success")
    void getTransactionHistory_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx1 = new Transaction();
        tx1.setTransactionId(TRANSACTION_ID); // Use UUID
        tx1.setUserId(TENANT_USER_ID);
        tx1.setAmount(BigDecimal.TEN);
        tx1.setStatus(TransactionStatus.COMPLETED);
        tx1.setTransactionType(TransactionType.TOPUP);
        tx1.setCreatedAt(LocalDateTime.now().minusDays(1));

        Transaction tx2 = new Transaction();
        tx2.setTransactionId(NEW_TRANSACTION_ID); // Use different UUID
        tx2.setUserId(TENANT_USER_ID);
        tx2.setAmount(BigDecimal.ONE);
        tx2.setStatus(TransactionStatus.COMPLETED);
        tx2.setTransactionType(TransactionType.PAYMENT);
        tx2.setCreatedAt(LocalDateTime.now());

        Page<Transaction> transactionPage = new PageImpl<>(List.of(tx2, tx1), pageable, 2);

        // Mock repository call with UUID
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(TENANT_USER_ID), eq(pageable)))
                .thenReturn(transactionPage);

        // Call service with UUID
        Page<TransactionDto> resultPage = paymentService.getTransactionHistory(TENANT_USER_ID, null, null, null, pageable);

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(1, resultPage.getTotalPages());
        assertEquals(2, resultPage.getContent().size());
        assertEquals(tx2.getTransactionId(), resultPage.getContent().get(0).transactionId()); // Assert UUID equality
        assertEquals(tx1.getTransactionId(), resultPage.getContent().get(1).transactionId());

        // Verify repository call with UUID
        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(eq(TENANT_USER_ID), eq(pageable));
        verify(transactionRepository, never()).findUserTransactionsByFilter(any(UUID.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Get Transaction History - With Filters")
    void getTransactionHistory_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now();
        TransactionType type = TransactionType.PAYMENT;

        Transaction tx2 = new Transaction();
        tx2.setTransactionId(NEW_TRANSACTION_ID); // Use UUID
        tx2.setUserId(TENANT_USER_ID);
        tx2.setAmount(BigDecimal.ONE);
        tx2.setStatus(TransactionStatus.COMPLETED);
        tx2.setTransactionType(type);
        tx2.setCreatedAt(LocalDateTime.now());

        Page<Transaction> transactionPage = new PageImpl<>(List.of(tx2), pageable, 1);

        // Mock filter query with UUID
        when(transactionRepository.findUserTransactionsByFilter(
                eq(TENANT_USER_ID),
                any(LocalDateTime.class), // Use any() for dates unless specific value needed
                any(LocalDateTime.class),
                eq(type),
                eq(pageable)))
                .thenReturn(transactionPage);

        // Call service with UUID
        Page<TransactionDto> resultPage = paymentService.getTransactionHistory(TENANT_USER_ID, startDate, endDate, type, pageable);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(1, resultPage.getContent().size());
        assertEquals(tx2.getTransactionId(), resultPage.getContent().get(0).transactionId()); // Assert UUID equality
        assertEquals(type, resultPage.getContent().get(0).transactionType());

        verify(transactionRepository, never()).findByUserIdOrderByCreatedAtDesc(any(UUID.class), any());
        // Verify filter query call with UUID
        verify(transactionRepository).findUserTransactionsByFilter(eq(TENANT_USER_ID), any(), any(), eq(type), eq(pageable));
    }
}