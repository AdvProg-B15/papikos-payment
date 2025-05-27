package id.ac.ui.cs.advprog.papikos.payment.response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void testBuilder_setsAllFieldsAndBuildsCorrectly() {
        String testMessage = "Custom success message";
        Map<String, String> testData = Map.of("key", "value");
        HttpStatus testStatus = HttpStatus.ACCEPTED;

        long beforeTimestamp = System.currentTimeMillis();
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .status(testStatus)
                .message(testMessage)
                .data(testData)
                .build();
        long afterTimestamp = System.currentTimeMillis();

        assertNotNull(response);
        assertEquals(testStatus.value(), response.getStatus());
        assertEquals(testMessage, response.getMessage());
        assertEquals(testData, response.getData());
        assertTrue(response.getTimestamp() >= beforeTimestamp && response.getTimestamp() <= afterTimestamp,
                "Timestamp should be set around the time of creation.");
    }

    @Test
    void testBuilder_buildsWithDefaultStatusOK_whenStatusNotSet() {
        String testMessage = "Default OK";
        ApiResponse<String> response = ApiResponse.<String>builder()
                .message(testMessage)
                .data("some data")
                // .status(..) // Status not set
                .build();

        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus(), "Default status should be OK if not set.");
        assertEquals(testMessage, response.getMessage());
        assertEquals("some data", response.getData());
    }

    @Test
    void testBuilder_canBuildWithNullData() {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("Success with no data")
                .data(null) // Explicitly setting null data
                .build();

        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("Success with no data", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testBuilder_canBuildWithNullMessage() {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .status(HttpStatus.OK)
                .message(null) // Explicitly setting null message
                .data("data")
                .build();

        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertNull(response.getMessage());
        assertEquals("data", response.getData());
    }

    @Test
    void testBuilder_badRequestConvenienceMethod() {
        String errorMessage = "Invalid input provided";
        // Note: The generic type T for badRequest without data will be Object if not specified
        ApiResponse<Object> response = ApiResponse.builder().badRequest(errorMessage);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData(), "Data should be null for badRequest convenience method without data param.");
    }

    @Test
    void testBuilder_notFoundConvenienceMethod() {
        String errorMessage = "Resource not found";
        ApiResponse<Void> response = ApiResponse.<Void>builder().notFound(errorMessage); // Explicit type for Void data

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testBuilder_internalErrorConvenienceMethod() {
        String errorMessage = "An unexpected error occurred";
        ApiResponse<?> response = ApiResponse.builder().internalError(errorMessage); // Wildcard for data type

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
    }

    // --- Test Getters (implicitly covered, but can be explicit) ---
    @Test
    void testGetters() {
        String message = "Getter test";
        Integer data = 123;
        HttpStatus status = HttpStatus.I_AM_A_TEAPOT;

        ApiResponse<Integer> response = ApiResponse.<Integer>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();

        assertEquals(status.value(), response.getStatus());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertTrue(response.getTimestamp() <= System.currentTimeMillis() && response.getTimestamp() > 0);
    }
}