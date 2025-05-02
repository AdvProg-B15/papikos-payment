package id.ac.ui.cs.advprog.papikos.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator; // Preferred way in Hibernate 6+

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID; // Import UUID

@Entity
@Table(name = "user_balances")
@Getter
@Setter
@NoArgsConstructor
public class UserBalance {

    @Id
    // Assuming userId from Auth Service is also UUID
    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID userId; // Changed from Long to UUID

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructor for initial creation
    public UserBalance(UUID userId, BigDecimal balance) { // Changed Long to UUID
        this.userId = userId;
        this.balance = (balance != null && balance.compareTo(BigDecimal.ZERO) >= 0) ? balance : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserBalance that = (UserBalance) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserBalance{" +
                "userId=" + userId +
                ", balance=" + balance +
                ", updatedAt=" + updatedAt +
                '}';
    }
}