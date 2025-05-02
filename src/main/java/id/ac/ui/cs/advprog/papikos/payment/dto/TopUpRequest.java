package id.ac.ui.cs.advprog.papikos.payment.dto;

import jakarta.validation.constraints.DecimalMin; // Use Jakarta validation
import jakarta.validation.constraints.NotNull;   // Use Jakarta validation
import java.math.BigDecimal;


public record TopUpRequest(

        @NotNull(message = "Top-up amount cannot be null.")
        @DecimalMin(value = "0.01", message = "Top-up amount must be positive.")
        BigDecimal amount
) { }
