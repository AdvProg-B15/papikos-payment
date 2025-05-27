//package id.ac.ui.cs.advprog.papikos.payment.config;
//
//import id.ac.ui.cs.advprog.papikos.payment.security.TokenAuthenticationFilter;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
//import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
//import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertSame;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SecurityConfigTest {
//
//    @Mock
//    private TokenAuthenticationFilter tokenAuthenticationFilter;
//
//    @Mock
//    private HttpSecurity httpSecurity;
//
//    // Mocks for the configurers returned by HttpSecurity methods
//    @Mock
//    private CsrfConfigurer<HttpSecurity> csrfConfigurer;
//
//    @Mock
//    private SessionManagementConfigurer<HttpSecurity> sessionManagementConfigurer;
//
//    @Mock
//    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorizeRequestsRegistry;
//
//    @Mock
//    private SecurityFilterChain mockFilterChain; // To mock the result of http.build()
//
//    @InjectMocks // This will inject tokenAuthenticationFilter into SecurityConfig constructor
//    private SecurityConfig securityConfig;
//
//
//    @BeforeEach
//    void setupHttpSecurityMocks() throws Exception {
//        // http.csrf(Customizer)
//        // When httpSecurity.csrf(customizer) is called, we simulate the customizer
//        // operating on our csrfConfigurer mock, and then return httpSecurity for chaining.
//        when(httpSecurity.csrf(any(Customizer.class))).thenAnswer(invocation -> {
//            Customizer<CsrfConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
//            customizer.customize(csrfConfigurer); // The lambda will call disable() on this
//            return httpSecurity;
//        });
//        // When csrfConfigurer.disable() is called (by the lambda), it should return httpSecurity
//        when(csrfConfigurer.disable()).thenReturn(httpSecurity);
//
//
//        // http.sessionManagement(Customizer)
//        when(httpSecurity.sessionManagement(any(Customizer.class))).thenAnswer(invocation -> {
//            Customizer<SessionManagementConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
//            customizer.customize(sessionManagementConfigurer); // The lambda will call sessionCreationPolicy on this
//            return httpSecurity;
//        });
//        // When sessionManagementConfigurer.sessionCreationPolicy(...) is called, it returns itself (or the parent HttpSecurity).
//        // For simplicity in mocking, let's assume it returns the sessionManagementConfigurer.
//        when(sessionManagementConfigurer.sessionCreationPolicy(any(SessionCreationPolicy.class)))
//                .thenReturn(sessionManagementConfigurer);
//
//
//        // http.authorizeHttpRequests(Customizer)
//        when(httpSecurity.authorizeHttpRequests(any(Customizer.class))).thenAnswer(invocation -> {
//            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> customizer = invocation.getArgument(0);
//            customizer.customize(authorizeRequestsRegistry); // The lambda will call anyRequest().permitAll() on this
//            return httpSecurity;
//        });
//        // When authorizeRequestsRegistry.anyRequest() is called, it returns itself
//        when(authorizeRequestsRegistry.anyRequest()).thenReturn(authorizeRequestsRegistry);
//        // When authorizeRequestsRegistry.permitAll() is called, it returns itself
//        when(authorizeRequestsRegistry.permitAll()).thenReturn(authorizeRequestsRegistry);
//
//        // http.addFilterBefore(...) returns httpSecurity
//        when(httpSecurity.addFilterBefore(any(TokenAuthenticationFilter.class), any()))
//                .thenReturn(httpSecurity);
//
//        // http.build()
//        when(httpSecurity.build()).thenReturn(mockFilterChain);
//    }
//
//
//    @Test
//    void testSecurityFilterChainConfiguration() throws Exception {
//        // Act
//        SecurityFilterChain filterChain = securityConfig.securityFilterChain(httpSecurity);
//
//        // Assert
//        assertNotNull(filterChain);
//        assertSame(mockFilterChain, filterChain, "Should return the SecurityFilterChain built by HttpSecurity");
//
//        // 1. Verify CSRF is configured to be disabled
//        // We capture the customizer and then simulate its execution on a new mock
//        // to verify what the lambda does.
//        ArgumentCaptor<Customizer<CsrfConfigurer<HttpSecurity>>> csrfCustomizerCaptor = ArgumentCaptor.forClass(Customizer.class);
//        verify(httpSecurity).csrf(csrfCustomizerCaptor.capture());
//        CsrfConfigurer<HttpSecurity> capturedCsrfConfigurer = mock(CsrfConfigurer.class); // A fresh mock for verification
//        csrfCustomizerCaptor.getValue().customize(capturedCsrfConfigurer);
//        verify(capturedCsrfConfigurer).disable(); // Verify the lambda calls disable()
//
//        // 2. Verify Session Management is configured for STATELESS
//        ArgumentCaptor<Customizer<SessionManagementConfigurer<HttpSecurity>>> sessionCustomizerCaptor = ArgumentCaptor.forClass(Customizer.class);
//        verify(httpSecurity).sessionManagement(sessionCustomizerCaptor.capture());
//        SessionManagementConfigurer<HttpSecurity> capturedSessionConfigurer = mock(SessionManagementConfigurer.class);
//        sessionCustomizerCaptor.getValue().customize(capturedSessionConfigurer);
//        verify(capturedSessionConfigurer).sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//
//        // 3. Verify authorizeHttpRequests configuration (anyRequest().permitAll())
//        ArgumentCaptor<Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>> authCustomizerCaptor =
//                ArgumentCaptor.forClass(Customizer.class);
//        verify(httpSecurity).authorizeHttpRequests(authCustomizerCaptor.capture());
//        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry capturedAuthRegistry =
//                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
//        // Stub the chained calls on this verification mock
//        when(capturedAuthRegistry.anyRequest()).thenReturn(capturedAuthRegistry);
//        authCustomizerCaptor.getValue().customize(capturedAuthRegistry);
//        verify(capturedAuthRegistry).anyRequest();
//        verify(capturedAuthRegistry).permitAll();
//
//
//        // 4. Verify addFilterBefore is called with the correct filter and class
//        verify(httpSecurity).addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//
//        // 5. Verify httpSecurity.build() is called
//        verify(httpSecurity).build();
//    }
//}