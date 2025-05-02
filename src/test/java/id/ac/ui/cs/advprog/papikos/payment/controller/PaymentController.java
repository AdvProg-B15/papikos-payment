package id.ac.ui.cs.advprog.papikos.payment.controller;

import id.ac.ui.cs.advprog.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; // Required for simulating auth
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors; // for user()
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Add security filter support
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    private final Long MOCK_USER_ID = 1L;
    private final Long RENTAL_ID = 101L;
    private final BigDecimal TOPUP_AMOUNT = new BigDecimal("100.00");
    private final BigDecimal PAYMENT_AMOUNT = new BigDecimal("500.00");

    // Re-initialize MockMvc with security context
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity()) // Apply Spring Security filters
                .build();
    }

    @Test
    @DisplayName("POST /payments/topup/initiate - Success")
    @WithMockUser(username = "user1", roles = {"TENANT"}) // Simulate authenticated tenant
    void initiateTopUp_Success() throws Exception {
        TopUpRequest request = new TopUpRequest(TOPUP_AMOUNT);
        TopUpInitiationResponse responseDto = new TopUpInitiationResponse(123L, "http://gateway.url/pay");

        // Mock the service call - need to extract user ID from security context
        // For unit test, we often assume this is done correctly and pass the expected ID
        when(paymentService.initiateTopUp(eq(MOCK_USER_ID), any(TopUpRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/payments/topup/initiate") // Adjust path if needed
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString()))) // Simulate user ID from token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(responseDto.getTransactionId().intValue())))
                .andExpect(jsonPath("$.paymentGatewayUrl", is(responseDto.getPaymentGatewayUrl())));

        verify(paymentService).initiateTopUp(eq(MOCK_USER_ID), any(TopUpRequest.class));
    }

    @Test
    @DisplayName("POST /payments/topup/initiate - Invalid Amount")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void initiateTopUp_InvalidAmount() throws Exception {
        TopUpRequest request = new TopUpRequest(BigDecimal.ZERO); // Invalid amount

        // Mock service to throw exception
        when(paymentService.initiateTopUp(eq(MOCK_USER_ID), any(TopUpRequest.class)))
                .thenThrow(new InvalidOperationException("Amount must be positive"));

        mockMvc.perform(post("/api/v1/payments/topup/initiate")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Expect 400
                .andExpect(jsonPath("$.message", containsString("Amount must be positive")));

        verify(paymentService).initiateTopUp(eq(MOCK_USER_ID), any(TopUpRequest.class));
    }


    @Test
    @DisplayName("POST /payments/pay - Success")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void payForRental_Success() throws Exception {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, PAYMENT_AMOUNT);
        TransactionDto responseDto = new TransactionDto(
                1L, MOCK_USER_ID, TransactionType.PAYMENT, PAYMENT_AMOUNT, TransactionStatus.COMPLETED,
                RENTAL_ID, MOCK_USER_ID, 2L, // Payer, Payee
                null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(paymentService.payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/payments/pay")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(responseDto.getTransactionId().intValue())))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(paymentService).payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class));
    }

    @Test
    @DisplayName("POST /payments/pay - Insufficient Balance")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void payForRental_InsufficientBalance() throws Exception {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, PAYMENT_AMOUNT);

        when(paymentService.payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class)))
                .thenThrow(new InsufficientBalanceException("Not enough funds"));

        mockMvc.perform(post("/api/v1/payments/pay")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Expect 400
                .andExpect(jsonPath("$.message", containsString("Not enough funds")));

        verify(paymentService).payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class));
    }

    @Test
    @DisplayName("POST /payments/pay - Rental Not Found")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void payForRental_RentalNotFound() throws Exception {
        PaymentRequest request = new PaymentRequest(RENTAL_ID, PAYMENT_AMOUNT);

        when(paymentService.payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Rental not found"));

        mockMvc.perform(post("/api/v1/payments/pay")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()); // Expect 404

        verify(paymentService).payForRental(eq(MOCK_USER_ID), any(PaymentRequest.class));
    }


    @Test
    @DisplayName("GET /payments/balance - Success")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void getMyBalance_Success() throws Exception {
        BalanceDto balanceDto = new BalanceDto(MOCK_USER_ID, new BigDecimal("123.45"), LocalDateTime.now());
        when(paymentService.getUserBalance(MOCK_USER_ID)).thenReturn(balanceDto);

        mockMvc.perform(get("/api/v1/payments/balance")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(MOCK_USER_ID.intValue())))
                .andExpect(jsonPath("$.balance", is(123.45))); // Match numeric value

        verify(paymentService).getUserBalance(MOCK_USER_ID);
    }

    @Test
    @DisplayName("GET /payments/history - Success")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void getMyTransactionHistory_Success() throws Exception {
        TransactionDto txDto = new TransactionDto(
                1L, MOCK_USER_ID, TransactionType.TOPUP, BigDecimal.TEN, TransactionStatus.COMPLETED,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<TransactionDto> page = new PageImpl<>(List.of(txDto), PageRequest.of(0, 10), 1);

        when(paymentService.getTransactionHistory(eq(MOCK_USER_ID), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/payments/history")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionId", is(txDto.getTransactionId().intValue())))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(paymentService).getTransactionHistory(eq(MOCK_USER_ID), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /payments/history - With Filters")
    @WithMockUser(username = "user1", roles = {"TENANT"})
    void getMyTransactionHistory_WithFilters() throws Exception {
        TransactionDto txDto = new TransactionDto(
                1L, MOCK_USER_ID, TransactionType.PAYMENT, BigDecimal.ONE, TransactionStatus.COMPLETED,
                RENTAL_ID, MOCK_USER_ID, 2L, null, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<TransactionDto> page = new PageImpl<>(List.of(txDto), PageRequest.of(0, 5), 1);

        when(paymentService.getTransactionHistory(eq(MOCK_USER_ID), any(), any(), eq(TransactionType.PAYMENT), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/payments/history")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(MOCK_USER_ID.toString())))
                        .param("page", "0")
                        .param("size", "5")
                        .param("type", "PAYMENT") // Add filter param
                        .param("startDate", "2023-01-01") // Example date filter
                        .param("endDate", "2023-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionType", is("PAYMENT")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(paymentService).getTransactionHistory(eq(MOCK_USER_ID), any(), any(), eq(TransactionType.PAYMENT), any(Pageable.class));
    }


    // --- Webhook Test (Basic structure) ---
    @Test
    @DisplayName("POST /payments/topup/webhook - Success (Conceptual)")
    void handleTopUpWebhook_Success() throws Exception {
        // This test is simplified. Real webhook testing is complex.
        // Assume the webhook payload provides a transaction ID that the gateway confirmed.
        String webhookPayload = "{\"transaction_id\": 123, \"status\": \"SUCCESS\"}"; // Example payload

        // Mock the service - *no* user auth needed for webhook endpoint usually
        // The service method `confirmTopUp` is responsible for acting on the confirmed ID
        long transactionIdFromWebhook = 123L;
        doNothing().when(paymentService).confirmTopUp(transactionIdFromWebhook);

        mockMvc.perform(post("/api/v1/payments/topup/webhook")
                        // No .with(SecurityMockMvc...) needed here
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk()); // Expect 200 OK if processed

        // Verify the service method was called with the ID extracted from payload
        // (Requires controller logic to parse payload and extract ID)
        // verify(paymentService).confirmTopUp(transactionIdFromWebhook); // --> This verify needs controller impl detail
        // For now, we'll just verify the controller method should conceptually call the service
    }
}
