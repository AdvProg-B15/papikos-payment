package id.ac.ui.cs.advprog.papikos.payment.model;

import jakarta.persistence.*;

@Entity
public class Balance {
    @Id
    private String userId;

    private long amount = 0;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // Business methods
    public void addAmount(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.amount += amount;
    }

    public void subtractAmount(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.amount < amount) throw new IllegalArgumentException("Insufficient balance");
        this.amount -= amount;
    }
}