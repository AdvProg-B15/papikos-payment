package id.ac.ui.cs.advprog.papikos.payment.commands;

import id.ac.ui.cs.advprog.papikos.payment.entity.UserBalance;
import id.ac.ui.cs.advprog.papikos.payment.entity.TransactionType;
import id.ac.ui.cs.advprog.papikos.payment.entity.Transaction;
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
        UserBalance balance = balanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    return balanceRepository.save(newBalance);
                });

        balance.addAmount(amount);
        balanceRepository.save(balance);

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TOPUP);
        transactionRepository.save(transaction);
    }
}
