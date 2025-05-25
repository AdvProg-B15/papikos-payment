package id.ac.ui.cs.advprog.papikos.payment.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects; // Import for hashCode and equals comparison

import static org.junit.jupiter.api.Assertions.*;

class RentalEventTest {

    private String rentalId;
    private String userId;
    private String kosId;
    private String kosOwnerId;
    private LocalDate bookingDate;
    private BigDecimal price;
    private String status;

    private RentalEvent event1;
    private RentalEvent event2; // For equality tests

    @BeforeEach
    void setUp() {
        rentalId = "rental-uuid-123";
        userId = "user-uuid-456";
        kosId = "kos-uuid-789";
        kosOwnerId = "owner-uuid-000";
        bookingDate = LocalDate.now();
        price = new BigDecimal("1200.50");
        status = "BOOKING_INITIATED";

        event1 = new RentalEvent(rentalId, userId, kosId, kosOwnerId, bookingDate, price, status);
    }

    @Test
    void testDefaultConstructor() {
        RentalEvent defaultEvent = new RentalEvent();
        assertNotNull(defaultEvent, "Event should be creatable with default constructor.");
        assertNull(defaultEvent.getRentalId());
        assertNull(defaultEvent.getUserId());
        assertNull(defaultEvent.getKosId());
        assertNull(defaultEvent.getKosOwnerId());
        assertNull(defaultEvent.getBookingDate());
        assertNull(defaultEvent.getPrice());
        assertNull(defaultEvent.getStatus());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        // event1 is created with all-args constructor in setUp
        assertEquals(rentalId, event1.getRentalId());
        assertEquals(userId, event1.getUserId());
        assertEquals(kosId, event1.getKosId());
        assertEquals(kosOwnerId, event1.getKosOwnerId());
        assertEquals(bookingDate, event1.getBookingDate());
        assertEquals(price, event1.getPrice());
        assertEquals(status, event1.getStatus());
    }

    @Test
    void testSetters() {
        RentalEvent eventToSet = new RentalEvent();

        String newRentalId = "new-rental-id";
        String newUserId = "new-user-id";
        String newKosId = "new-kos-id";
        String newKosOwnerId = "new-owner-id";
        LocalDate newBookingDate = LocalDate.now().plusDays(1);
        BigDecimal newPrice = new BigDecimal("200.00");
        String newStatus = "CONFIRMED";

        eventToSet.setRentalId(newRentalId);
        eventToSet.setUserId(newUserId);
        eventToSet.setKosId(newKosId);
        eventToSet.setKosOwnerId(newKosOwnerId);
        eventToSet.setBookingDate(newBookingDate);
        eventToSet.setPrice(newPrice);
        eventToSet.setStatus(newStatus);

        assertEquals(newRentalId, eventToSet.getRentalId());
        assertEquals(newUserId, eventToSet.getUserId());
        assertEquals(newKosId, eventToSet.getKosId());
        assertEquals(newKosOwnerId, eventToSet.getKosOwnerId());
        assertEquals(newBookingDate, eventToSet.getBookingDate());
        assertEquals(newPrice, eventToSet.getPrice());
        assertEquals(newStatus, eventToSet.getStatus());
    }

    @Test
    void testEquals_sameObject() {
        assertTrue(event1.equals(event1));
    }

    @Test
    void testEquals_nullObject() {
        assertFalse(event1.equals(null));
    }

    @Test
    void testEquals_differentClass() {
        assertFalse(event1.equals(new Object()));
    }

    @Test
    void testEquals_sameValues() {
        event2 = new RentalEvent(rentalId, userId, kosId, kosOwnerId, bookingDate, price, status);
        assertTrue(event1.equals(event2));
        assertEquals(event1, event2); // For good measure
    }

    @Test
    void testEquals_oneFieldDifferent() {
        event2 = new RentalEvent(rentalId, userId, kosId, kosOwnerId, bookingDate, price, "DIFFERENT_STATUS");
        assertFalse(event1.equals(event2));
    }

    @Test
    void testEquals_withNullFields() {
        RentalEvent eventWithNulls1 = new RentalEvent(null, userId, null, kosOwnerId, null, price, null);
        RentalEvent eventWithNulls2 = new RentalEvent(null, userId, null, kosOwnerId, null, price, null);
        RentalEvent eventWithNulls3 = new RentalEvent("id", userId, null, kosOwnerId, null, price, null);

        assertTrue(eventWithNulls1.equals(eventWithNulls2));
        assertFalse(eventWithNulls1.equals(eventWithNulls3));
        assertFalse(eventWithNulls3.equals(eventWithNulls1));
    }


    @Test
    void testHashCode_consistentWithEquals() {
        event2 = new RentalEvent(rentalId, userId, kosId, kosOwnerId, bookingDate, price, status);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testHashCode_differentForDifferentObjects() {
        event2 = new RentalEvent(rentalId, userId, kosId, kosOwnerId, bookingDate, price, "DIFFERENT_STATUS");
        assertNotEquals(event1.hashCode(), event2.hashCode()); // Usually true, barring collisions
    }

    @Test
    void testHashCode_withNullFields() {
        RentalEvent eventWithNulls = new RentalEvent(null, userId, null, kosOwnerId, null, price, null);
        int expectedHashCode = Objects.hash(null, userId, null, kosOwnerId, null, price, null);
        assertEquals(expectedHashCode, eventWithNulls.hashCode());
    }


    @Test
    void testToString() {
        String toString = event1.toString();
        assertNotNull(toString);
        assertTrue(toString.startsWith("RentalEvent{"));
        assertTrue(toString.contains("rentalId='" + rentalId + "'"));
        assertTrue(toString.contains("userId='" + userId + "'"));
        assertTrue(toString.contains("kosId='" + kosId + "'"));
        assertTrue(toString.contains("kosOwnerId='" + kosOwnerId + "'"));
        assertTrue(toString.contains("bookingDate=" + bookingDate));
        assertTrue(toString.contains("price=" + price));
        assertTrue(toString.contains("status='" + status + "'"));
        assertTrue(toString.endsWith("}"));
    }

    @Test
    void testSerializationProxyId() {
        // This is a simple check for the presence of serialVersionUID,
        // not a full serialization test.
        // Full serialization tests are more involved.
        try {
            java.lang.reflect.Field field = RentalEvent.class.getDeclaredField("serialVersionUID");
            field.setAccessible(true);
            assertEquals(1L, field.getLong(null), "serialVersionUID should be 1L.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("serialVersionUID field not found or not accessible: " + e.getMessage());
        }
    }
}