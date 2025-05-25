package id.ac.ui.cs.advprog.papikos.payment.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyTokenResponseTest {

    private int status;
    private String message;
    private VerifyTokenResponse.Data data;
    private long timestamp;

    private String dataUserId;
    private String dataEmail;
    private String dataRole;
    private String dataStatus;


    @BeforeEach
    void setUpOuter() {
        status = 200;
        message = "Token verified successfully";
        timestamp = System.currentTimeMillis();

        dataUserId = "user-123";
        dataEmail = "test@example.com";
        dataRole = "USER";
        dataStatus = "ACTIVE";
        data = VerifyTokenResponse.Data.builder()
                .userId(dataUserId)
                .email(dataEmail)
                .role(dataRole)
                .status(dataStatus)
                .build();
    }

    @Nested
    class DataNestedClassTest {
        private VerifyTokenResponse.Data data1;
        private VerifyTokenResponse.Data data2;

        @BeforeEach
        void setUpInner() {
            // Use values from outer setup for consistency
            data1 = VerifyTokenResponse.Data.builder()
                    .userId(dataUserId)
                    .email(dataEmail)
                    .role(dataRole)
                    .status(dataStatus)
                    .build();
            data2 = VerifyTokenResponse.Data.builder()
                    .userId(dataUserId)
                    .email(dataEmail)
                    .role(dataRole)
                    .status(dataStatus)
                    .build();
        }

        @Test
        void testDataNoArgsConstructor() {
            VerifyTokenResponse.Data emptyData = new VerifyTokenResponse.Data();
            assertNotNull(emptyData);
            assertNull(emptyData.userId); // Public fields will be null
        }

        @Test
        void testDataAllArgsConstructorAndGetters() {
            VerifyTokenResponse.Data constructedData = new VerifyTokenResponse.Data(dataUserId, dataEmail, dataRole, dataStatus);
            assertEquals(dataUserId, constructedData.getUserId()); // Lombok @Data generates getters
            assertEquals(dataEmail, constructedData.getEmail());
            assertEquals(dataRole, constructedData.getRole());
            assertEquals(dataStatus, constructedData.getStatus());
        }

        @Test
        void testDataSetters() {
            VerifyTokenResponse.Data dataToSet = new VerifyTokenResponse.Data();
            dataToSet.setUserId("new-user");
            dataToSet.setEmail("new@example.com");
            dataToSet.setRole("ADMIN");
            dataToSet.setStatus("INACTIVE");

            assertEquals("new-user", dataToSet.getUserId());
            assertEquals("new@example.com", dataToSet.getEmail());
            assertEquals("ADMIN", dataToSet.getRole());
            assertEquals("INACTIVE", dataToSet.getStatus());
        }


        @Test
        void testDataBuilder() {
            assertEquals(dataUserId, data1.getUserId());
            assertEquals(dataEmail, data1.getEmail());
            assertEquals(dataRole, data1.getRole());
            assertEquals(dataStatus, data1.getStatus());
        }

        @Test
        void testDataEqualsAndHashCode() {
            assertEquals(data1, data2, "Data objects with same field values should be equal.");
            assertEquals(data1.hashCode(), data2.hashCode(), "Hashcodes of equal Data objects should be same.");

            VerifyTokenResponse.Data differentData = VerifyTokenResponse.Data.builder().userId("diff-user").build();
            assertNotEquals(data1, differentData, "Data objects with different field values should not be equal.");
            assertNotEquals(data1.hashCode(), differentData.hashCode(), "Hashcodes of unequal Data objects should ideally be different.");
        }

        @Test
        void testDataToString() {
            String toString = data1.toString();
            assertNotNull(toString);
            assertTrue(toString.contains(dataUserId));
            assertTrue(toString.contains(dataEmail));
            assertTrue(toString.contains(dataRole));
            assertTrue(toString.contains(dataStatus));
        }
    }

    @Test
    void testVerifyTokenResponseNoArgsConstructor() {
        VerifyTokenResponse response = new VerifyTokenResponse();
        assertNotNull(response);
        assertEquals(0, response.status); // Default for int
        assertNull(response.message);
        assertNull(response.data);
        assertEquals(0L, response.timestamp); // Default for long
    }

    @Test
    void testVerifyTokenResponseAllArgsConstructorAndGetters() {
        VerifyTokenResponse response = new VerifyTokenResponse(status, message, data, timestamp);
        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
        assertSame(data, response.getData());
        assertEquals(timestamp, response.getTimestamp());
    }

    @Test
    void testVerifyTokenResponseSetters() {
        VerifyTokenResponse response = new VerifyTokenResponse();
        response.setStatus(201);
        response.setMessage("New Message");
        VerifyTokenResponse.Data newData = VerifyTokenResponse.Data.builder().userId("anotherUser").build();
        response.setData(newData);
        long newTimestamp = System.currentTimeMillis() + 1000;
        response.setTimestamp(newTimestamp);

        assertEquals(201, response.getStatus());
        assertEquals("New Message", response.getMessage());
        assertSame(newData, response.getData());
        assertEquals(newTimestamp, response.getTimestamp());
    }

    @Test
    void testVerifyTokenResponseBuilder() {
        VerifyTokenResponse response = VerifyTokenResponse.builder()
                .status(status)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();

        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
        assertSame(data, response.getData());
        assertEquals(timestamp, response.getTimestamp());
    }

    @Test
    void testVerifyTokenResponseEqualsAndHashCode() {
        VerifyTokenResponse response1 = VerifyTokenResponse.builder()
                .status(status).message(message).data(data).timestamp(timestamp).build();
        VerifyTokenResponse response2 = VerifyTokenResponse.builder()
                .status(status).message(message).data(data).timestamp(timestamp).build(); // Same data for equality

        assertEquals(response1, response2, "VerifyTokenResponse objects with same field values should be equal.");
        assertEquals(response1.hashCode(), response2.hashCode(), "Hashcodes of equal objects should be same.");

        VerifyTokenResponse differentResponse = VerifyTokenResponse.builder().status(404).build();
        assertNotEquals(response1, differentResponse, "Objects with different field values should not be equal.");
        // Note: Hashcode might collide, but typically different for different objects.
        // For a more robust hashcode inequality test, ensure more fields differ or use specific non-colliding values.
    }

    @Test
    void testVerifyTokenResponseToString() {
        VerifyTokenResponse response = VerifyTokenResponse.builder()
                .status(status).message(message).data(data).timestamp(timestamp).build();
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains(String.valueOf(status)));
        assertTrue(toString.contains(message));
        assertTrue(toString.contains(data.toString())); // Check if nested object's toString is part of it
        assertTrue(toString.contains(String.valueOf(timestamp)));
    }
}