package id.ac.ui.cs.advprog.papikos.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class RentalDetailsDto {
    private UUID rentalId;
    private UUID tenantUserId;
    private UUID ownerUserId;
    private String status; // e.g., "APPROVED", "ACTIVE"
    private BigDecimal monthlyRentPrice;
    // Add getters/setters or use record if immutable
    // Getters Setters...
    public UUID getRentalId() { return rentalId; }
    public void setRentalId(UUID rentalId) { this.rentalId = rentalId; }
    public UUID getTenantUserId() { return tenantUserId; }
    public void setTenantUserId(UUID tenantUserId) { this.tenantUserId = tenantUserId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getMonthlyRentPrice() { return monthlyRentPrice; }
    public void setMonthlyRentPrice(BigDecimal monthlyRentPrice) { this.monthlyRentPrice = monthlyRentPrice; }
}
