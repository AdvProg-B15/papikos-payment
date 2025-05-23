package id.ac.ui.cs.advprog.papikos.payment.listener;

import id.ac.ui.cs.advprog.papikos.payment.dto.RentalEvent;
import id.ac.ui.cs.advprog.papikos.payment.config.PaymentRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventListener.class);

    @RabbitListener(queues = PaymentRabbitMQConfig.PAYMENT_QUEUE_NAME)
    public void handleRentalCreatedEvent(RentalEvent event) {
        LOGGER.info("PaymentService: Received rental.created event: {}", event);
        // Simulate payment deduction and transaction logging
        LOGGER.info("PaymentService: Processing payment for rentalId: {} amount: {} from userId: {}",
                event.getRentalId(), event.getPrice(), event.getUserId());

        boolean paymentSuccessful = true; // Simulate success
        try {
            Thread.sleep(200); // Simulate payment processing
            if (paymentSuccessful) {
                LOGGER.info("PaymentService: Payment successful for rentalId: {}. Transaction logged.", event.getRentalId());
                // Potentially publish a "payment.succeeded" event
            } else {
                LOGGER.warn("PaymentService: Payment failed for rentalId: {}.", event.getRentalId());
                // Potentially publish a "payment.failed" event
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("PaymentService: Payment processing simulation interrupted", e);
        }
    }
}
