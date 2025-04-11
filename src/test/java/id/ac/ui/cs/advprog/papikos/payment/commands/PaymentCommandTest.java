package id.ac.ui.cs.advprog.papikos.payment.commands;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import id.ac.ui.cs.advprog.payment.model.Balance;
import id.ac.ui.cs.advprog.payment.repository.BalanceRepository;

class PaymentCommandTest {
    @Test
    void testCommandExecution() {
        BalanceRepository balanceRepository = mock(BalanceRepository.class);
        PaymentCommand command = mock(PaymentCommand.class);

        command.execute();
        verify(command, times(1)).execute();
    }
}
