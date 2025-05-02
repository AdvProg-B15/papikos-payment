package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.dto.*;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID; // Import UUID

public interface PaymentService {

    BalanceDto getUserBalance(UUID userId); // Changed Long to UUID

    TopUpInitiationResponse initiateTopUp(UUID userId, TopUpRequest request); // Changed Long to UUID

    void confirmTopUp(UUID transactionId); // Changed Long to UUID

    TransactionDto payForRental(UUID tenantUserId, PaymentRequest request); // Changed Long to UUID

    Page<TransactionDto> getTransactionHistory(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type, Pageable pageable); // Changed Long to UUID
}
