package id.ac.ui.cs.advprog.papikos.payment.dto;

import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionStatus;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID; // Import UUID

public record TransactionDto(
        UUID transactionId, // Changed Long to UUID
        UUID userId, // Changed Long to UUID
        TransactionType transactionType,
        BigDecimal amount,
        TransactionStatus status,
        UUID relatedRentalId, // Changed Long to UUID
        UUID payerUserId, // Changed Long to UUID
        UUID payeeUserId, // Changed Long to UUID
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
