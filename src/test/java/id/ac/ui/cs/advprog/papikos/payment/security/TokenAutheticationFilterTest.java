package id.ac.ui.cs.advprog.papikos.payment.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.papikos.payment.dto.VerifyTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException; // Import ServletException
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException; // Import IOException
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @InjectMocks
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    private final String TEST_AUTH_VERIFY_URL = "http://localhost:8080/auth";
    private final String TEST_INTERNAL_TOKEN_SECRET = "test-secret-token";
    private final String TEST_USER_ID = "test-user-id";
    private final String TEST_USER_ROLE = "USER";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenAuthenticationFilter, "authVerifyUrl", TEST_AUTH_VERIFY_URL);
        ReflectionTestUtils.setField(tokenAuthenticationFilter, "internalTokenSecret", TEST_INTERNAL_TOKEN_SECRET);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenValidInternalToken_thenSetsAuthenticationAndProceeds() throws ServletException, IOException { // Added exceptions
        request.addHeader("X-Internal-Token", TEST_INTERNAL_TOKEN_SECRET);

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("internal-service", authentication.getName());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("INTERNAL")));
        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenInvalidInternalToken_thenSetsUnauthorizedAndReturns() throws ServletException, IOException { // Added exceptions
        request.addHeader("X-Internal-Token", "invalid-internal-token");
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(stringWriter.toString().contains("Authentication Failed: Invalid internal token."));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void whenValidBearerToken_thenSetsAuthenticationAndProceeds() throws ServletException, IOException { // Added exceptions
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        VerifyTokenResponse.Data tokenData = VerifyTokenResponse.Data.builder()
                .userId(TEST_USER_ID).role(TEST_USER_ROLE).email("test@example.com").status("ACTIVE").build();
        VerifyTokenResponse verifyResponseDto = VerifyTokenResponse.builder()
                .status(200).message("Token verified").data(tokenData).timestamp(System.currentTimeMillis()).build();
        String verifyResponseJson = new ObjectMapper().writeValueAsString(verifyResponseDto);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(verifyResponseJson, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(TEST_AUTH_VERIFY_URL + "/api/v1/verify"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readValue(eq(verifyResponseJson), eq(VerifyTokenResponse.class))).thenReturn(verifyResponseDto);

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(TEST_USER_ID, authentication.getName());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority(TEST_USER_ROLE)));
        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenBearerTokenVerificationFailsNon2xx_thenSetsUnauthorizedAndReturns() throws ServletException, IOException { // Added exceptions
        request.addHeader("Authorization", "Bearer expired-jwt-token");
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        ResponseEntity<String> responseEntity = new ResponseEntity<>("Token expired", HttpStatus.UNAUTHORIZED);
        VerifyTokenResponse dummyVerifyResponseDto = VerifyTokenResponse.builder().build();

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readValue(eq("Token expired"), eq(VerifyTokenResponse.class))).thenReturn(dummyVerifyResponseDto);

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(stringWriter.toString().contains("Authentication Failed: Token verification unsuccessful"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void whenBearerTokenVerificationHttpClientError_thenSetsUnauthorizedAndReturns() throws ServletException, IOException { // Added exceptions
        request.addHeader("Authorization", "Bearer invalid-jwt-token");
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid token structure"));

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(stringWriter.toString().contains("Authentication Failed: Invalid token or authentication service error."));
        verify(objectMapper, never()).readValue(anyString(), ArgumentMatchers.any(Class.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void whenBearerTokenVerificationRestClientError_thenSetsInternalServerErrorAndReturns() throws ServletException, IOException { // Added exceptions
        request.addHeader("Authorization", "Bearer valid-looking-token");
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(stringWriter.toString().contains("Authentication Failed: Could not connect to authentication service."));
        verify(objectMapper, never()).readValue(anyString(), ArgumentMatchers.any(Class.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void whenBearerTokenVerificationJsonProcessingError_thenExceptionPropagates() throws IOException, ServletException { // Added ServletException
        request.addHeader("Authorization", "Bearer token-yielding-bad-json");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("this is not json", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readValue(eq("this is not json"), eq(VerifyTokenResponse.class)))
                .thenThrow(new JsonProcessingException("Failed to parse"){});

        assertThrows(JsonProcessingException.class, () -> {
            // Note: doFilterInternal itself throws ServletException, IOException
            // JsonProcessingException is an IOException
            tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);
        });

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void whenNoToken_thenNoAuthenticationSetAndProceeds() throws ServletException, IOException { // Added exceptions
        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenMalformedBearerToken_thenNoAuthenticationSetAndProceeds() throws ServletException, IOException { // Added exceptions
        request.addHeader("Authorization", "NotBearer token");

        tokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }
}