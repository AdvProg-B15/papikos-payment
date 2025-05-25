//package id.ac.ui.cs.advprog.papikos.payment.security;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import id.ac.ui.cs.advprog.papikos.payment.dto.VerifyTokenResponse;
//import id.ac.ui.cs.advprog.papikos.payment.dto.VerifyTokenResponse.Data;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.mock.web.MockHttpServletResponse;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.IOException;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class TokenAuthenticationFilterTest {
//
//    private RestTemplate restTemplate;
//    private ObjectMapper objectMapper;
//    private TokenAuthenticationFilter filter;
//
//    private HttpServletRequest request;
//    private HttpServletResponse response;
//    private FilterChain filterChain;
//
//    @BeforeEach
//    public void setup() {
//        restTemplate = mock(RestTemplate.class);
//        objectMapper = new ObjectMapper();
//        filter = new TokenAuthenticationFilter(restTemplate, objectMapper);
//
//        // Set required private fields manually
//        filter.authVerifyUrl = "http://auth-service";
//        filter.internalTokenSecret = "secret-internal-token";
//
//        request = mock(HttpServletRequest.class);
//        response = new MockHttpServletResponse();
//        filterChain = mock(FilterChain.class);
//
//        SecurityContextHolder.clearContext(); // Reset auth context
//    }
//
//    @Test
//    public void testValidInternalTokenShouldAuthenticate() throws ServletException, IOException {
//        when(request.getHeader("X-Internal-Token")).thenReturn("secret-internal-token");
//
//        filter.doFilterInternal(request, response, filterChain);
//
//        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
//        assertEquals("internal-service", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//        verify(filterChain).doFilter(request, response);
//    }
//
//    @Test
//    public void testInvalidInternalTokenShouldFail() throws ServletException, IOException {
//        when(request.getHeader("X-Internal-Token")).thenReturn("wrong-token");
//
//        MockHttpServletResponse servletResponse = (MockHttpServletResponse) response;
//        filter.doFilterInternal(request, servletResponse, filterChain);
//
//        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, servletResponse.getStatus());
//        assertTrue(servletResponse.getContentAsString().contains("Invalid internal token"));
//        verify(filterChain, never()).doFilter(any(), any());
//    }
//
//    @Test
//    public void testValidBearerTokenShouldAuthenticate() throws Exception {
//        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
//
//        Data data = new Data();
//        data.userId = "user-123";
//        data.role = "USER";
//
//        VerifyTokenResponse tokenResponse = new VerifyTokenResponse();
//        tokenResponse.data = data;
//
//        String responseBody = objectMapper.writeValueAsString(tokenResponse);
//
//        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
//        when(restTemplate.exchange(
//                eq("http://auth-service/api/v1/verify"),
//                eq(org.springframework.http.HttpMethod.POST),
//                any(),
//                eq(String.class))
//        ).thenReturn(responseEntity);
//
//        filter.doFilterInternal(request, response, filterChain);
//
//        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
//        assertEquals("user-123", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//        verify(filterChain).doFilter(request, response);
//    }
//
//    @Test
//    public void testInvalidBearerTokenShouldFail() throws Exception {
//        when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
//
//        when(restTemplate.exchange(
//                eq("http://auth-service/api/v1/verify"),
//                eq(org.springframework.http.HttpMethod.POST),
//                any(),
//                eq(String.class))
//        ).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid token"));
//
//        MockHttpServletResponse servletResponse = (MockHttpServletResponse) response;
//
//        filter.doFilterInternal(request, servletResponse, filterChain);
//
//        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, servletResponse.getStatus());
//        assertTrue(servletResponse.getContentAsString().contains("Invalid token"));
//        verify(filterChain, never()).doFilter(any(), any());
//    }
//
//    @Test
//    public void testNoAuthorizationHeaderShouldSkipAuth() throws Exception {
//        when(request.getHeader("Authorization")).thenReturn(null);
//        when(request.getHeader("X-Internal-Token")).thenReturn(null);
//
//        filter.doFilterInternal(request, response, filterChain);
//
//        assertNull(SecurityContextHolder.getContext().getAuthentication());
//        verify(filterChain).doFilter(request, response);
//    }
//}