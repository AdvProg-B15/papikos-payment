package id.ac.ui.cs.advprog.papikos.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator; // Use UuidGenerator

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID; // Import UUID

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // AUTO often defaults to UUID with modern Hibernate
    @UuidGenerator // Explicitly use Hibernate's UUID generator strategy
    @Column(name = "transaction_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID transactionId; // Changed from Long to UUID

    // Assuming userId from Auth Service is also UUID
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId; // Changed from Long to UUID

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    // Assuming rentalId from Rental Service is also UUID
    @Column(name = "related_rental_id", columnDefinition = "uuid")
    private UUID relatedRentalId; // Changed from Long to UUID

    // Assuming payerUserId from Auth Service is also UUID
    @Column(name = "payer_user_id", columnDefinition = "uuid")
    private UUID payerUserId; // Changed from Long to UUID

    // Assuming payeeUserId from Auth Service is also UUID
    @Column(name = "payee_user_id", columnDefinition = "uuid")
    private UUID payeeUserId; // Changed from Long to UUID

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        // Use getter in case of proxy objects
        return getTransactionId() != null && Objects.equals(getTransactionId(), that.getTransactionId());
    }

    @Override
    public int hashCode() {
        // Use a constant for detached entities without ID
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", userId=" + userId +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", status=" + status +
                ", relatedRentalId=" + relatedRentalId +
                ", payerUserId=" + payerUserId +
                ", payeeUserId=" + payeeUserId +
                ", createdAt=" + createdAt +
                '}';
    }
}
