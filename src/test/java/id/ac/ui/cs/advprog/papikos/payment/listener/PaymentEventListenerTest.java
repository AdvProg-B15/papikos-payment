package id.ac.ui.cs.advprog.papikos.payment.listener;

import id.ac.ui.cs.advprog.papikos.payment.dto.RentalEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
// SLF4J Test (Optional, for asserting log messages if needed, requires additional setup)
// import uk.org.lidalia.slf4jtest.TestLogger;
// import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
// Note: RentalEvent uses String for IDs, not UUID, so adjust sample data.
// If your RentalEvent was updated to use UUIDs, this test needs to match.
// Based on your provided RentalEvent code, IDs are Strings.

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    // To test logging more thoroughly, you'd use a test logger.
    // Example (requires slf4j-test dependency and setup):
    // TestLogger logger = TestLoggerFactory.getTestLogger(PaymentEventListener.class);

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    private RentalEvent sampleEvent;
    private String rentalIdStr;
    private String userIdStr;
    private String kosIdStr;
    private String kosOwnerIdStr;

    @BeforeEach
    void setUp() {
        rentalIdStr = "rental-event-id-123";
        userIdStr = "user-event-id-456";
        kosIdStr = "kos-event-id-789";
        kosOwnerIdStr = "owner-event-id-000";

        sampleEvent = new RentalEvent(
                rentalIdStr,
                userIdStr,
                kosIdStr,
                kosOwnerIdStr,
                LocalDate.now(),
                new BigDecimal("100.00"),
                "BOOKING_INITIATED"
        );

        // Clear any static state from previous tests if using TestLoggerFactory
        // TestLoggerFactory.clear();
    }

    @AfterEach
    void tearDown() {
        // TestLoggerFactory.clear();
    }


    @Test
    void testHandleRentalCreatedEvent_executesWithoutError() {
        // This test primarily ensures the method logic runs without unexpected exceptions.
        assertDoesNotThrow(() -> paymentEventListener.handleRentalCreatedEvent(sampleEvent),
                "handleRentalCreatedEvent should process a valid event without throwing an error.");

        // If you were using a TestLogger, you could assert log messages here:
        // List<LoggingEvent> events = logger.getLoggingEvents();
        // assertTrue(events.stream().anyMatch(e -> e.getMessage().contains("Received rental.created event")));
        // assertTrue(events.stream().anyMatch(e -> e.getMessage().contains("Processing payment for rentalId: " + rentalIdStr)));
        // assertTrue(events.stream().anyMatch(e -> e.getMessage().contains("Payment successful for rentalId: " + rentalIdStr)));
    }

    @Test
    void testHandleRentalCreatedEvent_simulatesProcessingDelay() {
        long startTime = System.currentTimeMillis();
        paymentEventListener.handleRentalCreatedEvent(sampleEvent);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Check that Thread.sleep(200) had an effect.
        // Allow some margin for overhead.
        assertTrue(duration >= 190 && duration < 500, // Adjusted upper bound slightly
                "Processing should take roughly 200ms due to Thread.sleep(200). Duration was: " + duration);
    }

    @Test
    void testHandleRentalCreatedEvent_whenInterrupted_reInterruptsThread() throws InterruptedException {
        final Thread testThread = Thread.currentThread(); // The thread running this test method

        Thread interruptingThread = new Thread(() -> {
            try {
                // Give the main logic a moment to enter the try block and potentially Thread.sleep()
                Thread.sleep(50); // Shorter than the 200ms sleep in the listener
                if (!testThread.isInterrupted()) { // Only interrupt if not already (e.g. by test framework)
                    testThread.interrupt();
                }
            } catch (InterruptedException ignored) {
                // This interrupting thread itself was interrupted, which is fine.
            }
        });

        // Clear interrupted status before starting the test logic in case it's already set
        Thread.interrupted();

        interruptingThread.start();
        paymentEventListener.handleRentalCreatedEvent(sampleEvent); // This call will be interrupted

        // Wait for the interrupting thread to complete its action
        interruptingThread.join(500); // Join with a timeout

        // After the method handles InterruptedException, it calls Thread.currentThread().interrupt() again.
        // So, the interrupted status of the testThread should now be true.
        assertTrue(testThread.isInterrupted(),
                "The current thread should be re-interrupted after handling InterruptedException.");

        // Clear the interrupted status for subsequent tests if any run in the same thread context (though JUnit usually isolates)
        Thread.interrupted();
    }

    @Test
    void testHandleRentalCreatedEvent_withNullEvent_throwsNullPointerException() {
        // The current RentalEvent listener method directly accesses event fields (e.g., event.getRentalId()).
        // If 'event' is null, this will cause a NullPointerException.
        assertThrows(NullPointerException.class, () -> {
            paymentEventListener.handleRentalCreatedEvent(null);
        }, "Handling a null event should result in NullPointerException when accessing its fields.");
    }
}