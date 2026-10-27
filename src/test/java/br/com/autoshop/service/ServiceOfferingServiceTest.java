package br.com.autoshop.service;

import br.com.autoshop.controller.dto.ServiceOfferingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.ServiceOfferingRepository;
import br.com.autoshop.repository.model.ServiceOfferingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceTest {

    @Mock
    private ServiceOfferingRepository serviceOfferingRepository;

    private ServiceOfferingService serviceOfferingService;

    @BeforeEach
    void setUp() {
        serviceOfferingService = new ServiceOfferingService(serviceOfferingRepository);
    }

    @Test
    void getByIdReturnsMappedServiceOfferingWhenFound() {
        ServiceOfferingEntity entity = serviceOffering(1L);
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(entity));

        ServiceOfferingDTO result = serviceOfferingService.getById(1L);

        assertServiceOfferingDto(result);
    }

    @Test
    void getByIdReturnsNullWhenMissing() {
        when(serviceOfferingRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(serviceOfferingService.getById(99L));
    }

    @Test
    void getByNameReturnsMappedServiceOfferingWhenFoundAndNullWhenMissing() {
        when(serviceOfferingRepository.findByName("Troca de óleo")).thenReturn(Optional.of(serviceOffering(1L)));
        when(serviceOfferingRepository.findByName("missing")).thenReturn(Optional.empty());

        assertServiceOfferingDto(serviceOfferingService.getByName("Troca de óleo"));
        assertNull(serviceOfferingService.getByName("missing"));
    }

    @Test
    void savePersistsMappedEntityAndReturnsGeneratedId() {
        // save() returns the locally built entity's id, which only becomes non-null because
        // a real JPA repository mutates the generated id onto the entity it was given.
        doAnswer(invocation -> {
            ServiceOfferingEntity saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        }).when(serviceOfferingRepository).save(any(ServiceOfferingEntity.class));

        Long id = serviceOfferingService.save(dto());

        assertEquals(42L, id);
        verify(serviceOfferingRepository).save(argThat(saved -> saved.getName().equals("Troca de óleo")
                && saved.getDescription().equals("Troca de óleo do motor")
                && saved.getBaseCost().equals(150.0)
                && saved.getEstimatedDurationMinutes().equals(30)
                && saved.getCategory().equals("Manutenção")));
    }

    @Test
    void putPersistsEntityWithProvidedId() {
        serviceOfferingService.put(7L, dto());

        verify(serviceOfferingRepository).save(argThat(saved -> saved.getId().equals(7L)
                && saved.getName().equals("Troca de óleo")));
    }

    @Test
    void patchUpdatesRecognizedNonNullFieldsAndSaves() {
        ServiceOfferingEntity entity = serviceOffering(1L);
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(entity));
        LocalDateTime previousUpdateDate = entity.getUpdateDate();

        serviceOfferingService.patch(1L, Map.of(
                "name", "Alinhamento",
                "description", "Alinhamento e balanceamento",
                "category", "Suspensão",
                "baseCost", "200.0",
                "estimatedDurationMinutes", "45"));

        assertEquals("Alinhamento", entity.getName());
        assertEquals("Alinhamento e balanceamento", entity.getDescription());
        assertEquals("Suspensão", entity.getCategory());
        assertEquals(200.0, entity.getBaseCost());
        assertEquals(45, entity.getEstimatedDurationMinutes());
        assertTrue(entity.getUpdateDate().isAfter(previousUpdateDate) || entity.getUpdateDate().isEqual(previousUpdateDate));
        verify(serviceOfferingRepository).save(entity);
    }

    @Test
    void patchWithNoRecognizedNonNullChangesDoesNotSave() {
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(serviceOffering(1L)));

        assertDoesNotThrow(() -> serviceOfferingService.patch(1L, Map.of("unknown", "value")));

        verify(serviceOfferingRepository, never()).save(any());
    }

    @Test
    void patchWithNonNumericBaseCostThrowsAndDoesNotSave() {
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(serviceOffering(1L)));

        RequestInvalidException exception = assertThrows(RequestInvalidException.class,
                () -> serviceOfferingService.patch(1L, Map.of("baseCost", "not-a-number")));

        assertEquals(ServiceOfferingService.BASE_COST_IS_INVALID, exception.getMessage());
        verify(serviceOfferingRepository, never()).save(any());
    }

    @Test
    void patchWithNonPositiveBaseCostThrowsAndDoesNotSave() {
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(serviceOffering(1L)));

        RequestInvalidException exception = assertThrows(RequestInvalidException.class,
                () -> serviceOfferingService.patch(1L, Map.of("baseCost", "-10")));

        assertEquals(ServiceOfferingService.BASE_COST_IS_INVALID, exception.getMessage());
        verify(serviceOfferingRepository, never()).save(any());
    }

    @Test
    void patchWithNonNumericEstimatedDurationThrowsAndDoesNotSave() {
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(serviceOffering(1L)));

        RequestInvalidException exception = assertThrows(RequestInvalidException.class,
                () -> serviceOfferingService.patch(1L, Map.of("estimatedDurationMinutes", "not-a-number")));

        assertEquals(ServiceOfferingService.ESTIMATED_DURATION_IS_INVALID, exception.getMessage());
        verify(serviceOfferingRepository, never()).save(any());
    }

    @Test
    void patchWithNonPositiveEstimatedDurationThrowsAndDoesNotSave() {
        when(serviceOfferingRepository.findById(1L)).thenReturn(Optional.of(serviceOffering(1L)));

        RequestInvalidException exception = assertThrows(RequestInvalidException.class,
                () -> serviceOfferingService.patch(1L, Map.of("estimatedDurationMinutes", "0")));

        assertEquals(ServiceOfferingService.ESTIMATED_DURATION_IS_INVALID, exception.getMessage());
        verify(serviceOfferingRepository, never()).save(any());
    }

    @Test
    void patchDoesNothingWhenServiceOfferingDoesNotExist() {
        when(serviceOfferingRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> serviceOfferingService.patch(99L, Map.of("name", "Alinhamento")));

        verify(serviceOfferingRepository, never()).save(any());
    }

    @Test
    void deleteByIdMarksServiceOfferingInactive() {
        serviceOfferingService.deleteById(3L);

        verify(serviceOfferingRepository).updateActiveById(false, 3L);
    }

    @Test
    void isServiceOfferingExistByIdReflectsRepositoryResult() {
        when(serviceOfferingRepository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(serviceOffering(1L)));
        when(serviceOfferingRepository.getByIdAndActiveIsTrue(2L)).thenReturn(Optional.empty());

        assertTrue(serviceOfferingService.isServiceOfferingExistById(1L));
        assertFalse(serviceOfferingService.isServiceOfferingExistById(2L));
    }

    @Test
    void getAllMapsPageContentAndPreservesPageMetadata() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ServiceOfferingEntity> entities = new PageImpl<>(List.of(serviceOffering(1L)), pageable, 1);
        when(serviceOfferingRepository.findAll(pageable)).thenReturn(entities);

        Page<ServiceOfferingDTO> result = serviceOfferingService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertServiceOfferingDto(result.getContent().get(0));
    }

    private static ServiceOfferingDTO dto() {
        return ServiceOfferingDTO.builder()
                .name("Troca de óleo")
                .description("Troca de óleo do motor")
                .baseCost(150.0)
                .estimatedDurationMinutes(30)
                .category("Manutenção")
                .build();
    }

    private static ServiceOfferingEntity serviceOffering(Long id) {
        return ServiceOfferingEntity.builder()
                .id(id)
                .name("Troca de óleo")
                .description("Troca de óleo do motor")
                .baseCost(150.0)
                .estimatedDurationMinutes(30)
                .category("Manutenção")
                .build();
    }

    private static void assertServiceOfferingDto(ServiceOfferingDTO result) {
        assertNotNull(result);
        assertEquals("Troca de óleo", result.getName());
        assertEquals("Troca de óleo do motor", result.getDescription());
        assertEquals(150.0, result.getBaseCost());
        assertEquals(30, result.getEstimatedDurationMinutes());
        assertEquals("Manutenção", result.getCategory());
    }
}
