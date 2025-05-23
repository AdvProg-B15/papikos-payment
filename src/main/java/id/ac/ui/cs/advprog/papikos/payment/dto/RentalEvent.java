package id.ac.ui.cs.advprog.papikos.payment.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class RentalEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rentalId;
    private String userId;
    private String kosId; // The ID of the kos/property
    private String kosOwnerId;
    private LocalDate bookingDate;
    private BigDecimal price;
    private String status; // e.g., "BOOKING_INITIATED"

    // Default constructor for JSON deserialization
    public RentalEvent() {}

    public RentalEvent(String rentalId, String userId, String kosId, String kosOwnerId, LocalDate bookingDate, BigDecimal price, String status) {
        this.rentalId = rentalId;
        this.userId = userId;
        this.kosId = kosId;
        this.kosOwnerId = kosOwnerId;
        this.bookingDate = bookingDate;
        this.price = price;
        this.status = status;
    }

    // Getters
    public String getRentalId() { return rentalId; }
    public String getUserId() { return userId; }
    public String getKosId() { return kosId; }
    public String getKosOwnerId() { return kosOwnerId; }
    public LocalDate getBookingDate() { return bookingDate; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }

    // Setters (useful for object mappers and some frameworks)
    public void setRentalId(String rentalId) { this.rentalId = rentalId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setKosId(String kosId) { this.kosId = kosId; }
    public void setKosOwnerId(String kosOwnerId) { this.kosOwnerId = kosOwnerId; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "RentalEvent{" +
                "rentalId='" + rentalId + '\'' +
                ", userId='" + userId + '\'' +
                ", kosId='" + kosId + '\'' +
                ", kosOwnerId='" + kosOwnerId + '\'' +
                ", bookingDate=" + bookingDate +
                ", price=" + price +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RentalEvent that = (RentalEvent) o;
        return Objects.equals(rentalId, that.rentalId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(kosId, that.kosId) &&
                Objects.equals(kosOwnerId, that.kosOwnerId) &&
                Objects.equals(bookingDate, that.bookingDate) &&
                Objects.equals(price, that.price) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rentalId, userId, kosId, kosOwnerId, bookingDate, price, status);
    }
}
