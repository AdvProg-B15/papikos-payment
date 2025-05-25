package id.ac.ui.cs.advprog.papikos.payment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
    }

    @Test
    void testRestTemplateBeanCreation() {

        RestTemplate restTemplate = appConfig.restTemplate();

        assertNotNull(restTemplate, "RestTemplate bean should not be null.");

        assertInstanceOf(RestTemplate.class, restTemplate, "Bean should be an instance of RestTemplate.");
    }
}