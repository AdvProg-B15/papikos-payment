package id.ac.ui.cs.advprog.papikos.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component; // Make it a Spring bean

import java.util.UUID;

@Component
@Slf4j
public class AuthServiceClientStub implements AuthServiceClient {

    private static final UUID KNOWN_USER_ID = UUID.fromString("a1b7a39a-8f8f-4f19-a5e3-8d9c1b8a9b5a");

    @Override
    public boolean userExists(UUID userId) {
        log.warn("AuthServiceClient STUB: Checking existence for userId: {}", userId);
        boolean exists = KNOWN_USER_ID.equals(userId);
        log.warn("AuthServiceClient STUB: Returning exists = {}", exists);
        return exists;
    }
}