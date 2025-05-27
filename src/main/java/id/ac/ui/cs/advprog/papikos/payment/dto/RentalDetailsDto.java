package id.ac.ui.cs.advprog.papikos.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
public class RentalDetailsDto {
    // Add getters/setters or use record if immutable
    // Getters Setters...
    private UUID rentalId;
    private UUID tenantUserId;
    private UUID ownerUserId;
    private String status; // e.g., "APPROVED", "ACTIVE"
    private BigDecimal monthlyRentPrice;

}
