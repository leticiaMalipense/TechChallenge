package br.com.autoshop.service;

import br.com.autoshop.Helper.VehicleHelper;
import br.com.autoshop.dto.VehicleDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.model.VehicleEntity;
import br.com.autoshop.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest extends VehicleHelper {

    @Mock
    private VehicleRepository vehicleRepository;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository);
    }

    @Test
    void getByPlateNumberReturnsMappedVehicleWhenFound() {
        VehicleEntity entity = createVehicleEntity();
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE)).thenReturn(Optional.of(entity));

        VehicleDTO result = vehicleService.getByPlateNumber(DEFAULT_PLATE);

        assertVehicleDto(result);
    }

    @Test
    void getByPlateNumberReturnsNullWhenMissing() {
        when(vehicleRepository.findByPlateNumber("MISSING")).thenReturn(Optional.empty());

        assertNull(vehicleService.getByPlateNumber("MISSING"));
    }

    @Test
    void savePersistsMappedEntityAndReturnsPlateNumber() {
        VehicleDTO dto = createVehicleDTO();

        String result = vehicleService.save(dto);

        assertEquals(DEFAULT_PLATE, result);
        verify(vehicleRepository).save(argThat(saved ->
                DEFAULT_PLATE.equals(saved.getPlateNumber()) &&
                DEFAULT_LAST_REVIEW.equals(saved.getLastReview())
        ));
    }

    @Test
    void putUpdatesExistingVehiclePreservingIdAndCreationDate() {
        VehicleEntity existing = createVehicleEntity(10L, DEFAULT_PLATE);
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE)).thenReturn(Optional.of(existing));

        LocalDateTime newReview = LocalDateTime.of(2025, 6, 1, 10, 0);
        VehicleDTO updateDto = createVehicleDTO(DEFAULT_PLATE, newReview);

        vehicleService.put(DEFAULT_PLATE, updateDto);

        verify(vehicleRepository).save(argThat(saved ->
                Long.valueOf(10L).equals(saved.getId()) &&
                existing.getCreationDate().equals(saved.getCreationDate()) &&
                DEFAULT_PLATE.equals(saved.getPlateNumber()) &&
                newReview.equals(saved.getLastReview())
        ));
    }

    @Test
    void putCreatesNewVehicleWhenVehicleDoesNotExist() {
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE)).thenReturn(Optional.empty());

        VehicleDTO dto = createVehicleDTO();

        vehicleService.put(DEFAULT_PLATE, dto);

        verify(vehicleRepository).save(argThat(saved ->
                saved.getId() == null &&
                DEFAULT_PLATE.equals(saved.getPlateNumber()) &&
                DEFAULT_LAST_REVIEW.equals(saved.getLastReview())
        ));
    }

    @Test
    void patchUpdatesRecognizedNonNullFieldsAndSaves() {
        VehicleEntity entity = createVehicleEntity();
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE)).thenReturn(Optional.of(entity));

        LocalDateTime updatedReview = LocalDateTime.of(2026, 2, 20, 15, 45);
        Map<String, Object> fields = Map.of(
                "plateNumber", "XYZ9876",
                "lastReview", "2026-02-20T15:45:00"
        );

        vehicleService.patch(DEFAULT_PLATE, fields);

        assertEquals("XYZ9876", entity.getPlateNumber());
        assertEquals(updatedReview, entity.getLastReview());
        verify(vehicleRepository).save(entity);
    }

    @Test
    void patchWithNoRecognizedNonNullChangesDoesNotSave() {
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE))
                .thenReturn(Optional.of(createVehicleEntity()));

        assertDoesNotThrow(() -> vehicleService.patch(DEFAULT_PLATE, Map.of("unknown", "value")));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void patchWithNullValueDoesNotUpdateNorSave() {
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE))
                .thenReturn(Optional.of(createVehicleEntity()));

        assertDoesNotThrow(() -> vehicleService.patch(DEFAULT_PLATE, Collections.singletonMap("plateNumber", null)));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void patchWithInvalidLastReviewThrowsAndDoesNotSave() {
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE))
                .thenReturn(Optional.of(createVehicleEntity()));

        RequestInvalidException exception = assertThrows(RequestInvalidException.class,
                () -> vehicleService.patch(DEFAULT_PLATE, Map.of("lastReview", "invalid-date-format")));

        assertEquals(VehicleService.LAST_REVIEW_IS_INVALID, exception.getMessage());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void patchDoesNothingWhenVehicleDoesNotExist() {
        when(vehicleRepository.findByPlateNumber("MISSING")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> vehicleService.patch("MISSING", Map.of("plateNumber", "XYZ9876")));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void deleteByPlateNumberRemovesVehicleWhenFound() {
        VehicleEntity entity = createVehicleEntity();
        when(vehicleRepository.findByPlateNumber(DEFAULT_PLATE)).thenReturn(Optional.of(entity));

        vehicleService.deleteByPlateNumber(DEFAULT_PLATE);

        verify(vehicleRepository).delete(entity);
    }

    @Test
    void deleteByPlateNumberDoesNothingWhenMissing() {
        when(vehicleRepository.findByPlateNumber("MISSING")).thenReturn(Optional.empty());

        vehicleService.deleteByPlateNumber("MISSING");

        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void isVehicleExistByPlateNumberReflectsRepositoryResult() {
        when(vehicleRepository.findByPlateNumber("FOUND")).thenReturn(Optional.of(createVehicleEntity()));
        when(vehicleRepository.findByPlateNumber("MISSING")).thenReturn(Optional.empty());

        assertTrue(vehicleService.isVehicleExistByPlateNumber("FOUND"));
        assertFalse(vehicleService.isVehicleExistByPlateNumber("MISSING"));
    }

    @Test
    void getAllMapsPageContentAndPreservesPageMetadata() {
        PageRequest pageable = PageRequest.of(0, 10);
        VehicleEntity entity = createVehicleEntity();
        Page<VehicleEntity> entities = new PageImpl<>(List.of(entity), pageable, 1);
        when(vehicleRepository.findAll(pageable)).thenReturn(entities);

        Page<VehicleDTO> result = vehicleService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertVehicleDto(result.getContent().get(0));
    }

    @Test
    void builderVehicleEntityMapsDtoFieldsCorrectly() {
        VehicleDTO dto = createVehicleDTO("XYZ9876", LocalDateTime.of(2025, 5, 10, 12, 0));

        VehicleEntity.VehicleEntityBuilder builder = vehicleService.builderVehicleEntity(dto);
        VehicleEntity entity = builder.build();

        assertEquals("XYZ9876", entity.getPlateNumber());
        assertEquals(LocalDateTime.of(2025, 5, 10, 12, 0), entity.getLastReview());
    }
}
