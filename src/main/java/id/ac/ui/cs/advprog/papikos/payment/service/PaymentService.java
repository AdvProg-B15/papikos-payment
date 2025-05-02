package id.ac.ui.cs.advprog.papikos.payment.service;

import id.ac.ui.cs.advprog.papikos.payment.commands.PaymentCommand;
import id.ac.ui.cs.advprog.papikos.payment.commands.TopUpCommand;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final UserBalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

    public PaymentService(UserBalanceRepository balanceRepository,
                          TransactionRepository transactionRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    public void topUp(String userId, long amount) {
        PaymentCommand command = new TopUpCommand(
                userId, amount, balanceRepository, transactionRepository);
        command.execute();
    }
}
