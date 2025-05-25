//package id.ac.ui.cs.advprog.papikos.payment.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import id.ac.ui.cs.advprog.papikos.payment.dto.*;
//import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionStatus;
//import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
//import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.MediaType;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(PaymentController.class) // Test only the PaymentController layer
//class PaymentControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean // Mocks the PaymentService dependency
//    private PaymentService paymentService;
//
//    @Autowired
//    private ObjectMapper objectMapper; // For converting objects to JSON strings
//
//    @Autowired
//    private WebApplicationContext context;
//
//
//    private UUID testUserId;
//    private Authentication mockAuthentication;
//
//    @BeforeEach
//    void setUp() {
//        // Set up MockMvc to use Spring Security context
//        // This is important for SecurityMockMvcRequestPostProcessors.authentication to work
//        // if your SecurityConfig was more complex. For now, might not be strictly needed if SecurityConfig is permitAll for tests.
//        // However, it's good practice.
//        mockMvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity()) // Apply Spring Security test support
//                .build();
//
//
//        testUserId = UUID.randomUUID();
//        // Create a mock Authentication object. The 'name' will be used by getUserIdFromAuthentication.
//        mockAuthentication = new UsernamePasswordAuthenticationToken(testUserId.toString(), null, Collections.emptyList());
//    }
//
//    private TransactionDto createMockTransactionDto(UUID userId, BigDecimal amount, TransactionType type) {
//        return new TransactionDto(
//                UUID.randomUUID(), userId, type, amount, TransactionStatus.COMPLETED,
//                UUID.randomUUID(), userId, UUID.randomUUID(), "Mock notes",
//                LocalDateTime.now(), LocalDateTime.now()
//        );
//    }
//
//
//    @Test
//    void topUp_whenValidRequest_shouldReturnOkAndTransactionDto() throws Exception {
//        TopUpRequest topUpRequest = new TopUpRequest(new BigDecimal("100.00"));
//        TransactionDto mockTransactionDto = createMockTransactionDto(testUserId, topUpRequest.amount(), TransactionType.TOPUP);
//
//        when(paymentService.topUp(eq(testUserId), any(TopUpRequest.class))).thenReturn(mockTransactionDto);
//
//        mockMvc.perform(post("/api/v1/payment/topup")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(mockAuthentication)) // Provide Authentication
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(topUpRequest)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
//                .andExpect(jsonPath("$.amount").value(topUpRequest.amount().doubleValue()))
//                .andExpect(jsonPath("$.transactionType").value(TransactionType.TOPUP.toString()));
//    }
//
//    @Test
//    void topUp_whenAuthenticationNameIsInvalidUuid_shouldBeHandledByGlobalExceptionHandler() throws Exception {
//        TopUpRequest topUpRequest = new TopUpRequest(new BigDecimal("100.00"));
//        Authentication invalidAuth = new UsernamePasswordAuthenticationToken("not-a-uuid", null, Collections.emptyList());
//
//        // We expect an IllegalArgumentException from getUserIdFromAuthentication,
//        // which should be caught by GlobalExceptionHandler and turned into a 400 or 500 response.
//        // Let's assume GlobalExceptionHandler turns IllegalArgumentException into 400.
//        // If not, this might be 500.
//        mockMvc.perform(post("/api/v1/payment/topup")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(invalidAuth))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(topUpRequest)))
//                .andExpect(status().isBadRequest()); // Or .isInternalServerError() if that's how it's handled
//    }
//
//    @Test
//    void topUp_whenAuthenticationIsNull_shouldBeHandledByGlobalExceptionHandler() throws Exception {
//        TopUpRequest topUpRequest = new TopUpRequest(new BigDecimal("100.00"));
//        // Note: Providing null directly to .with(authentication(null)) might not work as expected.
//        // The controller method signature is `Authentication authentication` not `@AuthenticationPrincipal`.
//        // If Spring Security context is empty, `authentication` argument might be null or an AnonymousAuthenticationToken.
//        // Let's test the case where the Authentication object itself is null when passed to getUserIdFromAuthentication.
//        // This is harder to simulate directly with MockMvc for a method argument if it's not @AuthenticationPrincipal.
//        // The most direct test for getUserIdFromAuthentication(null) would be a direct unit test of that method.
//        // For controller behavior, if no auth is provided AND security allows, `authentication` arg might be null.
//        // If security enforces auth, it would be a 401/403 before controller method.
//
//        // This test assumes a scenario where Spring MVC somehow injects a null Authentication object.
//        // A more realistic scenario is testing getUserIdFromAuthentication itself or relying on security filter behavior.
//        // For this to work, the controller method needs to be callable without full security enforcement leading to an early 401/403.
//        // We can't directly make MockMvc pass null for the `Authentication authentication` parameter easily if it's not from `@AuthenticationPrincipal`.
//        // The private method test is more suitable for this.
//        // Let's assume the controller test ensures that if getUserId throws, GlobalExceptionHandler handles it.
//        // The previous test `topUp_whenAuthenticationNameIsInvalidUuid` already covers that an exception from getUserId is handled.
//        // If authentication is null, it will throw IllegalStateException.
//        Authentication nullNameAuth = new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList());
//
//        mockMvc.perform(post("/api/v1/payment/topup")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(nullNameAuth)) // Simulates auth obj with null name
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(topUpRequest)))
//                .andExpect(status().isBadRequest()); // Or .isInternalServerError() depending on GlobalExceptionHandler
//    }
//
//
//    @Test
//    void payForRental_whenValidRequest_shouldReturnOkAndTransactionDto() throws Exception {
//        PaymentRequest paymentRequest = new PaymentRequest(UUID.randomUUID(), new BigDecimal("200.00"));
//        TransactionDto mockTransactionDto = createMockTransactionDto(testUserId, paymentRequest.amount(), TransactionType.PAYMENT);
//        mockTransactionDto = new TransactionDto( // more specific mock
//                mockTransactionDto.transactionId(), testUserId, TransactionType.PAYMENT, paymentRequest.amount(),
//                TransactionStatus.COMPLETED, paymentRequest.rentalId(), testUserId, UUID.randomUUID(),
//                "Mock rental payment", LocalDateTime.now(), LocalDateTime.now()
//        );
//
//
//        when(paymentService.payForRental(eq(testUserId), any(PaymentRequest.class))).thenReturn(mockTransactionDto);
//
//        mockMvc.perform(post("/api/v1/payment/pay")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(mockAuthentication))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(paymentRequest)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
//                .andExpect(jsonPath("$.amount").value(paymentRequest.amount().doubleValue()))
//                .andExpect(jsonPath("$.transactionType").value(TransactionType.PAYMENT.toString()))
//                .andExpect(jsonPath("$.relatedRentalId").value(paymentRequest.rentalId().toString()));
//    }
//
//    @Test
//    void getMyBalance_shouldReturnOkAndBalanceDto() throws Exception {
//        BalanceDto mockBalanceDto = new BalanceDto(testUserId, new BigDecimal("500.00"), LocalDateTime.now());
//        when(paymentService.getUserBalance(eq(testUserId))).thenReturn(mockBalanceDto);
//
//        mockMvc.perform(get("/api/v1/payment/balance")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(mockAuthentication)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
//                .andExpect(jsonPath("$.balance").value(mockBalanceDto.balance().doubleValue()));
//    }
//
//    @Test
//    void getMyTransactionHistory_noFilters_shouldReturnOkAndPageOfTransactionDto() throws Exception {
//        Pageable pageable = PageRequest.of(0, 10);
//        TransactionDto mockTxDto = createMockTransactionDto(testUserId, new BigDecimal("10.00"), TransactionType.TOPUP);
//        Page<TransactionDto> mockPage = new PageImpl<>(Collections.singletonList(mockTxDto), pageable, 1);
//
//        when(paymentService.getTransactionHistory(eq(testUserId), eq(null), eq(null), eq(null), any(Pageable.class)))
//                .thenReturn(mockPage);
//
//        mockMvc.perform(get("/api/v1/payment/history")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(mockAuthentication)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.content[0].userId").value(testUserId.toString()))
//                .andExpect(jsonPath("$.totalElements").value(1));
//    }
//
//    @Test
//    void getMyTransactionHistory_withFilters_shouldReturnOkAndPageOfTransactionDto() throws Exception {
//        Pageable pageable = PageRequest.of(0, 5);
//        LocalDate startDate = LocalDate.of(2023, 1, 1);
//        LocalDate endDate = LocalDate.of(2023, 1, 31);
//        TransactionType type = TransactionType.PAYMENT;
//        TransactionDto mockTxDto = createMockTransactionDto(testUserId, new BigDecimal("10.00"), TransactionType.PAYMENT);
//        Page<TransactionDto> mockPage = new PageImpl<>(Collections.singletonList(mockTxDto), pageable, 1);
//
//        when(paymentService.getTransactionHistory(eq(testUserId), eq(startDate), eq(endDate), eq(type), any(Pageable.class)))
//                .thenReturn(mockPage);
//
//        mockMvc.perform(get("/api/v1/payment/history")
//                        .param("startDate", "2023-01-01")
//                        .param("endDate", "2023-01-31")
//                        .param("type", "PAYMENT")
//                        .param("page", "0")
//                        .param("size", "5")
//                        .with(SecurityMockMvcRequestPostProcessors.authentication(mockAuthentication)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.content[0].transactionType").value(TransactionType.PAYMENT.toString()))
//                .andExpect(jsonPath("$.totalElements").value(1));
//    }
//}