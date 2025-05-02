package id.ac.ui.cs.advprog.papikos.payment.client;

import id.ac.ui.cs.advprog.papikos.payment.dto.RentalDetailsDto; // Assuming this DTO is defined
import id.ac.ui.cs.advprog.papikos.payment.exception.ResourceNotFoundException;

import java.util.UUID;

public interface RentalServiceClient {

    RentalDetailsDto getRentalDetailsForPayment(UUID rentalId) throws ResourceNotFoundException;

}
