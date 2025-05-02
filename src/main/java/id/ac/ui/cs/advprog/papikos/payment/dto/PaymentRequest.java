package id.ac.ui.cs.advprog.papikos.payment.dto;

import jakarta.validation.constraints.NotNull; // Use Jakarta validation
import java.math.BigDecimal;
import java.util.UUID; // Import UUID

public record PaymentRequest(
        @NotNull UUID rentalId, // Changed Long to UUID
        @NotNull BigDecimal amount
) {}
