package id.ac.ui.cs.advprog.papikos.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID; // Import UUID

public record BalanceDto(
        UUID userId, // Changed Long to UUID
        BigDecimal balance,
        LocalDateTime updatedAt
) {}
