package id.ac.ui.cs.advprog.papikos.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;

class PaymentControllerTest {
    @Test
    void testTopUpEndpoint() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);

        ResponseEntity<?> response = controller.topUp("user1", 100000);
        assertEquals(200, response.getStatusCode().value());
    }
}
