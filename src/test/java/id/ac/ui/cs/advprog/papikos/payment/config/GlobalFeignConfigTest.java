package id.ac.ui.cs.advprog.papikos.payment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Not strictly necessary for this test if no @Mock fields, but good practice
class GlobalFeignConfigTest {

    private GlobalFeignConfig globalFeignConfig;
    private RequestTemplate mockRequestTemplate;

    private final String TEST_SECRET_TOKEN = "my-super-secret-test-token";

    @BeforeEach
    void setUp() {
        globalFeignConfig = new GlobalFeignConfig();
        mockRequestTemplate = mock(RequestTemplate.class); // Create a fresh mock for each test
    }

    @Test
    void propertyBasedRequestInterceptor_whenSecretIsPresent_addsHeader() {
        // Arrange
        ReflectionTestUtils.setField(globalFeignConfig, "internalTokenSecret", TEST_SECRET_TOKEN);
        RequestInterceptor interceptor = globalFeignConfig.propertyBasedRequestInterceptor();
        assertNotNull(interceptor);

        // Act
        interceptor.apply(mockRequestTemplate);

        // Assert
        ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockRequestTemplate, times(1)).header(headerNameCaptor.capture(), headerValueCaptor.capture());
        assertEquals("X-Internal-Token", headerNameCaptor.getValue());
        assertEquals(TEST_SECRET_TOKEN, headerValueCaptor.getValue());
    }

    @Test
    void propertyBasedRequestInterceptor_whenSecretIsNull_doesNotAddHeader() {
        // Arrange
        ReflectionTestUtils.setField(globalFeignConfig, "internalTokenSecret", null);
        RequestInterceptor interceptor = globalFeignConfig.propertyBasedRequestInterceptor();
        assertNotNull(interceptor);

        // Act
        interceptor.apply(mockRequestTemplate);

        // Assert
        verify(mockRequestTemplate, never()).header(anyString(), anyString());
    }

    @Test
    void propertyBasedRequestInterceptor_whenSecretIsEmpty_doesNotAddHeader() {
        // Arrange
        ReflectionTestUtils.setField(globalFeignConfig, "internalTokenSecret", "");
        RequestInterceptor interceptor = globalFeignConfig.propertyBasedRequestInterceptor();
        assertNotNull(interceptor);

        // Act
        interceptor.apply(mockRequestTemplate);

        // Assert
        verify(mockRequestTemplate, never()).header(anyString(), anyString());
    }

    @Test
    void propertyBasedRequestInterceptor_beanCreation() {
        // Simple test to ensure the bean method itself works and returns an interceptor
        RequestInterceptor interceptor = globalFeignConfig.propertyBasedRequestInterceptor();
        assertNotNull(interceptor, "RequestInterceptor bean should not be null.");
    }
}