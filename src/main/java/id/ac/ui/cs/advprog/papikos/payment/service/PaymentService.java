package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.dto.BalanceDto;
import id.ac.ui.cs.advprog.papikos.payment.dto.PaymentRequest;
import id.ac.ui.cs.advprog.papikos.payment.dto.TopUpRequest;
import id.ac.ui.cs.advprog.papikos.payment.dto.TransactionDto;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID; // Use UUID for IDs

public interface PaymentService {

    BalanceDto getUserBalance(UUID userId);

    TransactionDto TopUp(UUID userId, TopUpRequest request);

    TransactionDto payForRental(UUID tenantUserId, PaymentRequest request);

    Page<TransactionDto> getTransactionHistory(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type, Pageable pageable);
}
