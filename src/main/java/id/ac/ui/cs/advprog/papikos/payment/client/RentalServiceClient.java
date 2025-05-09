package id.ac.ui.cs.advprog.papikos.payment.client;

import id.ac.ui.cs.advprog.papikos.payment.dto.RentalDetailsDto; // Assuming this DTO is defined
import id.ac.ui.cs.advprog.papikos.payment.exception.ResourceNotFoundException;
//import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

//@FeignClient(name = "rental-service", url = "http://localhost:8081")
public interface RentalServiceClient {

    //@GetMapping("/api/rentals/{rentalId}")
    RentalDetailsDto getRentalDetailsForPayment(UUID rentalId) throws ResourceNotFoundException;

}
