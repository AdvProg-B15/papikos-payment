package id.ac.ui.cs.advprog.papikos.payment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRabbitMQConfigTest {

    private PaymentRabbitMQConfig paymentRabbitMQConfig;

    @BeforeEach
    void setUp() {
        paymentRabbitMQConfig = new PaymentRabbitMQConfig();
    }

    @Test
    void testRentalTopicExchangeBean() {
        TopicExchange exchange = paymentRabbitMQConfig.rentalTopicExchange();
        assertNotNull(exchange, "TopicExchange bean should not be null.");
        assertEquals(PaymentRabbitMQConfig.TOPIC_EXCHANGE_NAME, exchange.getName(), "Exchange name should match the constant.");
    }

    @Test
    void testPaymentQueueBean() {
        Queue queue = paymentRabbitMQConfig.paymentQueue();
        assertNotNull(queue, "Queue bean should not be null.");
        assertEquals(PaymentRabbitMQConfig.PAYMENT_QUEUE_NAME, queue.getName(), "Queue name should match the constant.");
        assertTrue(queue.isDurable(), "Queue should be durable.");
        // You can also check other properties like autoDelete, exclusive if they were set.
        // For Queue(name, durable), other properties default to false.
        assertFalse(queue.isAutoDelete(), "Queue should not be auto-delete by default.");
        assertFalse(queue.isExclusive(), "Queue should not be exclusive by default.");
    }

    @Test
    void testPaymentBindingBean() {
        // We need actual instances of Queue and TopicExchange to pass to the binding method
        Queue queue = new Queue(PaymentRabbitMQConfig.PAYMENT_QUEUE_NAME);
        TopicExchange exchange = new TopicExchange(PaymentRabbitMQConfig.TOPIC_EXCHANGE_NAME);

        Binding binding = paymentRabbitMQConfig.paymentBinding(queue, exchange);
        assertNotNull(binding, "Binding bean should not be null.");
        assertEquals(PaymentRabbitMQConfig.PAYMENT_QUEUE_NAME, binding.getDestination(), "Binding destination should be the payment queue name.");
        assertEquals(PaymentRabbitMQConfig.TOPIC_EXCHANGE_NAME, binding.getExchange(), "Binding exchange should be the rental topic exchange name.");
        assertEquals(PaymentRabbitMQConfig.ROUTING_KEY_RENTAL_CREATED, binding.getRoutingKey(), "Binding routing key should match the constant.");
    }

    @Test
    void testJsonMessageConverterBean() {
        MessageConverter converter = paymentRabbitMQConfig.jsonMessageConverter();
        assertNotNull(converter, "MessageConverter bean should not be null.");
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter, "Converter should be an instance of Jackson2JsonMessageConverter.");
    }

    @Test
    void testRabbitTemplateBean() {
        // Mock the ConnectionFactory dependency
        ConnectionFactory mockConnectionFactory = Mockito.mock(ConnectionFactory.class);

        RabbitTemplate rabbitTemplate = paymentRabbitMQConfig.rabbitTemplate(mockConnectionFactory);
        assertNotNull(rabbitTemplate, "RabbitTemplate bean should not be null.");
        assertSame(mockConnectionFactory, rabbitTemplate.getConnectionFactory(), "RabbitTemplate should use the provided ConnectionFactory.");
        assertInstanceOf(Jackson2JsonMessageConverter.class, rabbitTemplate.getMessageConverter(), "RabbitTemplate should use Jackson2JsonMessageConverter.");
    }
}