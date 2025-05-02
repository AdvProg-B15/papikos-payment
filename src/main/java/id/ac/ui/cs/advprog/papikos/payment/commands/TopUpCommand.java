package id.ac.ui.cs.advprog.papikos.payment.commands;

import id.ac.ui.cs.advprog.papikos.payment.model.Balance;
import id.ac.ui.cs.advprog.papikos.payment.model.PaymentType;
import id.ac.ui.cs.advprog.papikos.payment.model.Transaction;
import id.ac.ui.cs.advprog.papikos.payment.repository.UserBalanceRepository;
import id.ac.ui.cs.advprog.papikos.payment.repository.TransactionRepository;

public class TopUpCommand implements PaymentCommand {
    private final String userId;
    private final long amount;
    private final UserBalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

    public TopUpCommand(String userId, long amount,
                        UserBalanceRepository balanceRepository,
                        TransactionRepository transactionRepository) {
        this.userId = userId;
        this.amount = amount;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void execute() {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Balance newBalance = new Balance();
                    newBalance.setUserId(userId);
                    return balanceRepository.save(newBalance);
                });

        balance.addAmount(amount);
        balanceRepository.save(balance);

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setType(PaymentType.TOPUP);
        transactionRepository.save(transaction);
    }
}
