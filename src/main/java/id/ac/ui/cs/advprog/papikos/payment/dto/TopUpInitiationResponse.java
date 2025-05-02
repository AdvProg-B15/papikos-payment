package id.ac.ui.cs.advprog.papikos.payment.dto;

import java.util.UUID; // Import UUID

// Assuming transactionId generated is UUID
public record TopUpInitiationResponse(
        UUID transactionId, // Changed Long to UUID
        String paymentGatewayUrl // Or other gateway details
) {}
