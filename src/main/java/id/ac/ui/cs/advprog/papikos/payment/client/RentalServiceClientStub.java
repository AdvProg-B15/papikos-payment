package id.ac.ui.cs.advprog.papikos.payment.client;

import id.ac.ui.cs.advprog.papikos.payment.dto.RentalDetailsDto;
import id.ac.ui.cs.advprog.papikos.payment.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component; // Make it a Spring bean
// import org.springframework.context.annotation.Profile; // Optional: Activate only for testing/local

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class RentalServiceClientStub implements RentalServiceClient {

    private static final Map<UUID, RentalDetailsDto> stubRentals = new HashMap<>();

    static {
        UUID knownRentalId = UUID.fromString("c1d9b54c-1e1e-4c20-b6f4-9e0d2c9b0c6b");
        UUID knownTenantId = UUID.fromString("a1b7a39a-8f8f-4f19-a5e3-8d9c1b8a9b5a");
        UUID knownOwnerId = UUID.fromString("b2c8b40b-9g9g-5g20-a6f5-7e0c2b7a8a4b");

        RentalDetailsDto rental = new RentalDetailsDto();
        rental.setRentalId(knownRentalId);
        rental.setTenantUserId(knownTenantId);
        rental.setOwnerUserId(knownOwnerId);
        rental.setStatus("APPROVED"); // Important for payment tests
        rental.setMonthlyRentPrice(new BigDecimal("500.00"));
        stubRentals.put(knownRentalId, rental);
    }

    @Override
    public RentalDetailsDto getRentalDetailsForPayment(UUID rentalId) throws ResourceNotFoundException {
        log.warn("RentalServiceClient STUB: Fetching details for rentalId: {}", rentalId);
        RentalDetailsDto rental = stubRentals.get(rentalId);
        if (rental == null) {
            log.warn("RentalServiceClient STUB: Rental {} not found in stub data.", rentalId);
            throw new ResourceNotFoundException("STUB: Rental not found with ID: " + rentalId);
        }
        log.warn("RentalServiceClient STUB: Returning rental details for {}", rentalId);
        return rental;
    }
}
