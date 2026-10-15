package br.com.autoshop.Helper;

import br.com.autoshop.dto.VehicleDTO;
import br.com.autoshop.model.VehicleEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class VehicleHelper {

    public static final String DEFAULT_PLATE = "ABC1D23";
    public static final LocalDateTime DEFAULT_LAST_REVIEW = LocalDateTime.of(2025, 1, 15, 10, 30);
    public static final LocalDateTime DEFAULT_CREATION_DATE = LocalDateTime.of(2024, 1, 10, 8, 0);

    public VehicleDTO createVehicleDTO() {
        return createVehicleDTO(DEFAULT_PLATE, DEFAULT_LAST_REVIEW);
    }

    public VehicleDTO createVehicleDTO(String plateNumber) {
        return createVehicleDTO(plateNumber, DEFAULT_LAST_REVIEW);
    }

    public VehicleDTO createVehicleDTO(String plateNumber, LocalDateTime lastReview) {
        return VehicleDTO.builder()
                .plateNumber(plateNumber)
                .lastReview(lastReview)
                .build();
    }

    public VehicleEntity createVehicleEntity() {
        return createVehicleEntity(1L, DEFAULT_PLATE, DEFAULT_CREATION_DATE, DEFAULT_LAST_REVIEW);
    }

    public VehicleEntity createVehicleEntity(Long id) {
        return createVehicleEntity(id, DEFAULT_PLATE, DEFAULT_CREATION_DATE, DEFAULT_LAST_REVIEW);
    }

    public VehicleEntity createVehicleEntity(Long id, String plateNumber) {
        return createVehicleEntity(id, plateNumber, DEFAULT_CREATION_DATE, DEFAULT_LAST_REVIEW);
    }

    public VehicleEntity createVehicleEntity(Long id, String plateNumber, LocalDateTime creationDate, LocalDateTime lastReview) {
        return VehicleEntity.builder()
                .id(id)
                .plateNumber(plateNumber)
                .creationDate(creationDate)
                .lastReview(lastReview)
                .build();
    }

    public void assertVehicleDto(VehicleDTO result) {
        assertNotNull(result);
        assertEquals(DEFAULT_PLATE, result.getPlateNumber());
        assertEquals(DEFAULT_LAST_REVIEW, result.getLastReview());
    }
}
