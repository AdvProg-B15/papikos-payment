package id.ac.ui.cs.advprog.papikos.payment.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RentalDetailsDtoTest {

    private RentalDetailsDto rentalDetailsDto;

    private UUID rentalId;
    private UUID tenantUserId;
    private UUID ownerUserId;
    private String status;
    private BigDecimal monthlyRentPrice;

    @BeforeEach
    void setUp() {
        rentalDetailsDto = new RentalDetailsDto(); // Using the default constructor

        // Initialize sample values
        rentalId = UUID.randomUUID();
        tenantUserId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();
        status = "APPROVED";
        monthlyRentPrice = new BigDecimal("1500.75");
    }

    @Test
    void testDefaultConstructor() {
        RentalDetailsDto newDto = new RentalDetailsDto();
        assertNotNull(newDto, "Dto should be creatable with default constructor.");
        // Fields will be null or default initially
        assertNull(newDto.getRentalId());
        assertNull(newDto.getTenantUserId());
        assertNull(newDto.getOwnerUserId());
        assertNull(newDto.getStatus());
        assertNull(newDto.getMonthlyRentPrice());
    }

    @Test
    void testSetAndGetRentalId() {
        rentalDetailsDto.setRentalId(rentalId);
        assertEquals(rentalId, rentalDetailsDto.getRentalId());
    }

    @Test
    void testSetAndGetTenantUserId() {
        rentalDetailsDto.setTenantUserId(tenantUserId);
        assertEquals(tenantUserId, rentalDetailsDto.getTenantUserId());
    }

    @Test
    void testSetAndGetOwnerUserId() {
        rentalDetailsDto.setOwnerUserId(ownerUserId);
        assertEquals(ownerUserId, rentalDetailsDto.getOwnerUserId());
    }

    @Test
    void testSetAndGetStatus() {
        rentalDetailsDto.setStatus(status);
        assertEquals(status, rentalDetailsDto.getStatus());
    }

    @Test
    void testSetAndGetMonthlyRentPrice() {
        rentalDetailsDto.setMonthlyRentPrice(monthlyRentPrice);
        assertEquals(monthlyRentPrice, rentalDetailsDto.getMonthlyRentPrice());
    }

    @Test
    void testSetNullValues() {
        rentalDetailsDto.setRentalId(null);
        rentalDetailsDto.setTenantUserId(null);
        rentalDetailsDto.setOwnerUserId(null);
        rentalDetailsDto.setStatus(null);
        rentalDetailsDto.setMonthlyRentPrice(null);

        assertNull(rentalDetailsDto.getRentalId());
        assertNull(rentalDetailsDto.getTenantUserId());
        assertNull(rentalDetailsDto.getOwnerUserId());
        assertNull(rentalDetailsDto.getStatus());
        assertNull(rentalDetailsDto.getMonthlyRentPrice());
    }

    @Test
    void testChainSettersAndGetters() {
        // Set all values and then get them
        rentalDetailsDto.setRentalId(rentalId);
        rentalDetailsDto.setTenantUserId(tenantUserId);
        rentalDetailsDto.setOwnerUserId(ownerUserId);
        rentalDetailsDto.setStatus(status);
        rentalDetailsDto.setMonthlyRentPrice(monthlyRentPrice);

        assertEquals(rentalId, rentalDetailsDto.getRentalId());
        assertEquals(tenantUserId, rentalDetailsDto.getTenantUserId());
        assertEquals(ownerUserId, rentalDetailsDto.getOwnerUserId());
        assertEquals(status, rentalDetailsDto.getStatus());
        assertEquals(monthlyRentPrice, rentalDetailsDto.getMonthlyRentPrice());
    }
}