package id.ac.ui.cs.advprog.papikos.payment.client;

import id.ac.ui.cs.advprog.papikos.payment.dto.RentalDetailsDto; // Assuming this DTO is defined
import id.ac.ui.cs.advprog.papikos.payment.exception.ResourceNotFoundException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@FeignClient(name = "rentals-service", url = "${rental.service.url}")
public interface RentalServiceClient {

    @GetMapping("/api/v1/rentals/{rentalId}")
    RentalDetailsDto getRentalDetailsForPayment(UUID rentalId) throws ResourceNotFoundException;

}
