package id.ac.ui.cs.advprog.papikos.payment.model;

public enum TransactionType {
    TOPUP,
    PAYMENT,
    WITHDRAWAL, // Although not explicitly in features, good to include potentially
    REFUND      // For handling cancellations/refunds
}
