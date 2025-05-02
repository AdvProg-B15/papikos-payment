package id.ac.ui.cs.advprog.papikos.payment.client;

import java.util.UUID;


public interface AuthServiceClient {

    boolean userExists(UUID userId);

}
