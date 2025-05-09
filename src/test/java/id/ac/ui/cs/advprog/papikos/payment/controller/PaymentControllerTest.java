package id.ac.ui.cs.advprog.papikos.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionStatus;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.papikos.payment.exception.InvalidOperationException;
import id.ac.ui.cs.advprog.papikos.payment.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PaymentService paymentService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WebApplicationContext context;

    // Define test UUIDs
    private final UUID MOCK_USER_ID = UUID.fromString("a1b7a39a-8f8f-4f19-a5e3-8d9c1b8a9b5a");
    private final UUID RENTAL_ID = UUID.fromString("c1d9b54c-1e1e-4c20-b6f4-9e0d2c9b0c6b");
    private final UUID TRANSACTION_ID = UUID.fromString("d2e0c65d-2f2f-6d31-c7g6-0f1e3d0c1d7d");
    private final UUID MOCK_OWNER_ID = UUID.fromString("b2c8b40b-9g9g-5g20-a6f5-7e0c2b7a8a4b");

    private final BigDecimal TOPUP_AMOUNT = new BigDecimal("100.00");
    private final BigDecimal PAYMENT_AMOUNT = new BigDecimal("500.00");

    // Setup MockMvc with Spring Security context support
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /payments/topup/initiate - Success (Immediate Completion)")
    @WithMockUser(username = "test-user", roles = {"TENANT"})
    void initiateAndCompleteTopUp_Success() throws Exception {
        TopUpRequest request = new TopUpRequest(TOPUP_AMOUNT);

        TransactionDto completedTransactionDto = new TransactionDto(
                TRANSACTION_ID,
                MOCK_USER_ID,
                TransactionType.TOPUP,
                TOPUP_AMOUNT,
                TransactionStatus.COMPLETED,
                null,
                null,
                null,
                "Internal top-up completed automatically.",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.topUp(eq(MOCK_USER_ID), any(TopUpRequest.class)))
                .thenReturn(completedTransactionDto);

        mockMvc.perform(post("/api/v1/payments/topup/initiate")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // Assert fields from the TransactionDto
                .andExpect(jsonPath("$.transactionId", is(TRANSACTION_ID.toString())))
                .andExpect(jsonPath("$.userId", is(MOCK_USER_ID.toString())))
                .andExpect(jsonPath("$.transactionType", is(TransactionType.TOPUP.toString())))
                .andExpect(jsonPath("$.amount", is(TOPUP_AMOUNT.doubleValue())))
                .andExpect(jsonPath("$.status", is(TransactionStatus.COMPLETED.toString())));

        verify(paymentService).topUp(eq(MOCK_USER_ID), any(TopUpRequest.class));
    }

    @Test
    @DisplayName("POST /payments/topup/initiate - Invalid Amount")
    @WithMockUser(username = "test-user", roles = {"TENANT"})
    void initiateAndCompleteTopUp_InvalidAmount() throws Exception {
        TopUpRequest request = new TopUpRequest(BigDecimal.ZERO);


        when(paymentService.topUp(eq(MOCK_USER_ID), any(TopUpRequest.class)))
                .thenThrow(new InvalidOperationException("Amount must be positive"));

        mockMvc.perform(post("/api/v1/payments/topup/initiate")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Amount must be positive")));


        verify(paymentService).topUp(eq(MOCK_USER_ID), any(TopUpRequest.class));
    }

    @Test
    @DisplayName("POST /payments/pay - Success")
    @WithMockUser(username = "test-user", roles = {"TENANT"})
    void payForRental_Success() throws Exception {
        // Request DTO uses UUID
        PaymentRequest request = new PaymentRequest(RENTAL_ID, PAYMENT_AMOUNT);
        // Response DTO uses UUIDs
        TransactionDto responseDto = new TransactionDto(
                TRANSACTION_ID, MOCK_USER_ID, TransactionType.PAYMENT, PAYMENT_AMOUNT, TransactionStatus.COMPLETED,
                RENTAL_ID, MOCK_USER_ID, MOCK_OWNER_ID, // Payer, Payee UUIDs
                null, LocalDateTime.now(), LocalDateTime.now()
        );

        // Mock service call with specific UUID
        when(paymentService.payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/payments/pay")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // Assert UUIDs as strings in JSON path
                .andExpect(jsonPath("$.transactionId", is(TRANSACTION_ID.toString())))
                .andExpect(jsonPath("$.userId", is(MOCK_USER_ID.toString())))
                .andExpect(jsonPath("$.relatedRentalId", is(RENTAL_ID.toString())))
                .andExpect(jsonPath("$.payerUserId", is(MOCK_USER_ID.toString())))
                .andExpect(jsonPath("$.payeeUserId", is(MOCK_OWNER_ID.toString())))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Verify service call with specific UUID
        verify(paymentService).payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class));
    }

    @Test
    @DisplayName("POST /payments/pay - Insufficient Balance")
    @WithMockUser(username = "test-user", roles = {"TENANT"})
    void payForRental_InsufficientBalance() throws Exception {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, PAYMENT_AMOUNT); // Uses UUID

        // Mock service call with specific UUID to throw exception
        when(paymentService.payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class)))
                .thenThrow(new InsufficientBalanceException("Not enough funds"));

        mockMvc.perform(post("/api/v1/payments/pay")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Mapped by GlobalExceptionHandler
                .andExpect(jsonPath("$.message", containsString("Not enough funds")));

        verify(paymentService).payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class)); // Verify with UUID
    }

    @Test
    @DisplayName("POST /payments/pay - Rental Not Found")
    @WithMockUser(username = "test-user", roles = {"TENANT"})
    void payForRental_RentalNotFound() throws Exception {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, PAYMENT_AMOUNT); // Uses UUID

        // Mock service call with specific UUID to throw exception
        when(paymentService.payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Rental not found"));

        mockMvc.perform(post("/api/v1/payments/pay")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()); // Mapped by GlobalExceptionHandler

        verify(paymentService).payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class)); // Verify with UUID
    }

    @Test
    @DisplayName("GET /payments/balance - Success")
    @WithMockUser(username = "test-user", roles = {"TENANT"}) // Or OWNER
    void getMyBalance_Success() throws Exception {
        // DTO uses UUID
        BalanceDto balanceDto = new BalanceDto(MOCK_USER_ID, new BigDecimal("123.45"), LocalDateTime.now());
        // Mock service call with specific UUID
        when(paymentService.getUserBalance(eq(MOCK_USER_ID))).thenReturn(balanceDto);

        mockMvc.perform(get("/api/v1/payments/balance")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString()))))
                .andExpect(status().isOk())
                // Assert UUID as string in JSON path
                .andExpect(jsonPath("$.userId", is(MOCK_USER_ID.toString())))
                .andExpect(jsonPath("$.balance", is(123.45)));

        // Verify service call with specific UUID
        verify(paymentService).getUserBalance(eq(MOCK_USER_ID));
    }

    @Test
    @DisplayName("GET /payments/history - Success")
    @WithMockUser(username = "test-user", roles = {"TENANT"}) // Or OWNER
    void getMyTransactionHistory_Success() throws Exception {
        // DTO uses UUIDs
        TransactionDto txDto = new TransactionDto(
                TRANSACTION_ID, MOCK_USER_ID, TransactionType.TOPUP, BigDecimal.TEN, TransactionStatus.COMPLETED,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<TransactionDto> page = new PageImpl<>(List.of(txDto), PageRequest.of(0, 10), 1);

        // Mock service call with specific UUID and Pageable
        when(paymentService.getTransactionHistory(eq(MOCK_USER_ID), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/payments/history")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                // Assert UUID as string in JSON path
                .andExpect(jsonPath("$.content[0].transactionId", is(TRANSACTION_ID.toString())))
                .andExpect(jsonPath("$.totalElements", is(1)));

        // Verify service call with specific UUID
        verify(paymentService).getTransactionHistory(eq(MOCK_USER_ID), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /payments/history - With Filters")
    @WithMockUser(username = "test-user", roles = {"TENANT"})
    void getMyTransactionHistory_WithFilters() throws Exception {
        // DTO uses UUIDs
        TransactionDto txDto = new TransactionDto(
                TRANSACTION_ID, MOCK_USER_ID, TransactionType.PAYMENT, BigDecimal.ONE, TransactionStatus.COMPLETED,
                RENTAL_ID, MOCK_USER_ID, MOCK_OWNER_ID, null, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<TransactionDto> page = new PageImpl<>(List.of(txDto), PageRequest.of(0, 5), 1);

        // Mock service call with specific UUID and filters
        when(paymentService.getTransactionHistory(eq(MOCK_USER_ID), any(), any(), eq(TransactionType.PAYMENT), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/payments/history")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .param("page", "0")
                        .param("size", "5")
                        .param("type", "PAYMENT")
                        .param("startDate", "2023-01-01")
                        .param("endDate", "2023-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                // Assert UUID as string
                .andExpect(jsonPath("$.content[0].transactionId", is(TRANSACTION_ID.toString())))
                .andExpect(jsonPath("$.content[0].transactionType", is("PAYMENT")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        // Verify service call with specific UUID and filters
        verify(paymentService).getTransactionHistory(eq(MOCK_USER_ID), any(), any(), eq(TransactionType.PAYMENT), any(Pageable.class));
    }
}

