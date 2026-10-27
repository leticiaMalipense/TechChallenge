package br.com.autoshop.service;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.exception.AlreadyExistsException;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.PartRepository;
import br.com.autoshop.repository.model.PartEntity;
import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PartServiceTest {

    @Test
    void savePersistsNewPartAndReturnsGeneratedId() {
        PartRepository repository = mock(PartRepository.class);
        PartDTO dto = PartDTO.builder().id(99L).name("Filter").code("F01")
                .description("Oil filter").category("Filters")
                .partType(PartType.PART).unit(PartUnit.UNIT)
                .costPrice(BigDecimal.ONE).salePrice(BigDecimal.TEN).build();
        when(repository.save(any())).thenAnswer(invocation -> {
            PartEntity part = invocation.getArgument(0);
            assertNull(part.getId());
            part.setId(1L);
            return part;
        });

        assertEquals(1L, new PartService(repository).save(dto));

        var captor = ArgumentCaptor.forClass(PartEntity.class);
        verify(repository).save(captor.capture());
        PartEntity saved = captor.getValue();
        assertEquals(dto.getName(), saved.getName());
        assertEquals(dto.getCode(), saved.getCode());
        assertEquals(dto.getDescription(), saved.getDescription());
        assertEquals(dto.getCategory(), saved.getCategory());
        assertEquals(dto.getPartType(), saved.getPartType());
        assertEquals(dto.getUnit(), saved.getUnit());
        assertEquals(dto.getCostPrice(), saved.getCostPrice());
        assertEquals(dto.getSalePrice(), saved.getSalePrice());
        assertEquals(true, saved.getActive());
        assertNotNull(saved.getCreationDate());
    }

    @Test
    void saveRejectsReservedCodeWithoutSaving() {
        PartRepository repository = mock(PartRepository.class);
        when(repository.existsByCode("F01")).thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> new PartService(repository).save(PartDTO.builder().code("F01").build()));

        verify(repository, never()).save(any());
    }

    @Test
    void putRejectsDuplicateCodeWithoutSaving() {
        PartRepository repository = mock(PartRepository.class);
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(
                Optional.of(PartEntity.builder().id(1L).code("F01").build()));
        when(repository.existsByCode("F02")).thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> new PartService(repository).put(1L, PartDTO.builder().code("F02").build()));

        verify(repository, never()).save(any());
    }

    @Test
    void putAllowsUnchangedCode() {
        PartRepository repository = mock(PartRepository.class);
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(
                Optional.of(PartEntity.builder().id(1L).code("F01").build()));

        new PartService(repository).put(1L, PartDTO.builder().code("F01").build());

        verify(repository).save(any());
        verify(repository, never()).existsByCode(anyString());
    }

    @Test
    void deleteDeactivatesPartWithoutPhysicalDeletion() {
        PartRepository repository = mock(PartRepository.class);

        new PartService(repository).deleteById(1L);

        verify(repository).updateActiveById(false, 1L);
        verify(repository, never()).deleteById(anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"partType", "unit"})
    void patchRejectsUnknownEnumValues(String field) {
        PartRepository repository = mock(PartRepository.class);
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(
                Optional.of(PartEntity.builder().id(1L).build()));

        assertThrows(RequestInvalidException.class,
                () -> new PartService(repository).patch(1L, Map.of(field, "INVALID")));

        verify(repository, never()).save(any());
    }

    @Test
    void patchUpdatesOptionalFieldsAndEnums() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).name("Filter").code("F01").build();
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));

        new PartService(repository).patch(1L, Map.of("description", "Oil", "category", "Fluids",
                "partType", "SUPPLY", "unit", "LITER"));

        assertEquals("Oil", part.getDescription());
        assertEquals("Fluids", part.getCategory());
        assertEquals(PartType.SUPPLY, part.getPartType());
        assertEquals(PartUnit.LITER, part.getUnit());
        assertEquals("Filter", part.getName());
        assertEquals("F01", part.getCode());
        verify(repository).save(part);
    }

    @Test
    void patchIgnoresNullAndUnknownFieldsWithoutSaving() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).name("Filter").build();
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", null);
        fields.put("active", false);
        fields.put("id", 99L);

        new PartService(repository).patch(1L, fields);

        assertEquals("Filter", part.getName());
        assertEquals(1L, part.getId());
        assertEquals(true, part.getActive());
        verify(repository, never()).save(any());
    }

    @Test
    void putPreservesIdentityCreationDateAndActiveState() {
        PartRepository repository = mock(PartRepository.class);
        var creationDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        PartEntity part = PartEntity.builder().id(1L).code("F01").creationDate(creationDate).build();
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));
        PartDTO dto = PartDTO.builder().id(99L).name("New filter").code("F02")
                .description("Updated").category("Filters")
                .partType(PartType.PART)
                .unit(PartUnit.UNIT)
                .costPrice(BigDecimal.ONE).salePrice(BigDecimal.TEN).build();

        new PartService(repository).put(1L, dto);

        var captor = ArgumentCaptor.forClass(PartEntity.class);
        verify(repository).save(captor.capture());
        PartEntity saved = captor.getValue();
        assertEquals(1L, saved.getId());
        assertEquals(creationDate, saved.getCreationDate());
        assertEquals(true, saved.getActive());
        assertEquals(dto.getName(), saved.getName());
        assertEquals(dto.getCode(), saved.getCode());
        assertEquals(dto.getDescription(), saved.getDescription());
        assertEquals(dto.getCategory(), saved.getCategory());
        assertEquals(dto.getPartType(), saved.getPartType());
        assertEquals(dto.getUnit(), saved.getUnit());
        assertEquals(dto.getCostPrice(), saved.getCostPrice());
        assertEquals(dto.getSalePrice(), saved.getSalePrice());
    }

    @Test
    void unavailablePartCannotBeReadOrUpdated() {
        PartRepository repository = mock(PartRepository.class);
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.empty());
        when(repository.findByCodeAndActiveTrue("F01")).thenReturn(Optional.empty());
        PartService service = new PartService(repository);

        assertNull(service.getById(1L));
        assertNull(service.getByCode("F01"));
        assertFalse(service.isPartExistById(1L));
        service.put(1L, PartDTO.builder().code("F01").build());
        service.patch(1L, Map.of("name", "Updated"));

        verify(repository, never()).save(any());
    }

    @Test
    void queriesReturnPartId() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).code("F01").build();
        var pageable = PageRequest.of(0, 10);
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));
        when(repository.findByCodeAndActiveTrue("F01")).thenReturn(Optional.of(part));
        when(repository.findByActiveTrue(pageable)).thenReturn(
                new PageImpl<>(List.of(part)));
        PartService service = new PartService(repository);

        assertEquals(1L, service.getById(1L).getId());
        assertEquals(1L, service.getByCode("F01").getId());
        assertEquals(1L, service.getAll(pageable).getContent().get(0).getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void patchRejectsBlankNameAndCodeWithoutChangingEntity(String value) {
        for (String field : new String[]{"name", "code"}) {
            PartRepository repository = mock(PartRepository.class);
            PartEntity part = PartEntity.builder().id(1L).name("Filter").code("F01").build();
            when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));

            assertThrows(RequestInvalidException.class,
                    () -> new PartService(repository).patch(1L, Map.of(field, value)));

            assertEquals("Filter", part.getName());
            assertEquals("F01", part.getCode());
            verify(repository, never()).save(any());
        }
    }

    @Test
    void patchUpdatesNameAndCode() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).name("Filter").code("F01").build();
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));

        new PartService(repository).patch(1L, Map.of("name", "Oil filter", "code", "F02"));

        assertEquals("Oil filter", part.getName());
        assertEquals("F02", part.getCode());
        verify(repository).save(part);
    }

    @Test
    void patchRejectsDuplicateCodeWithoutChangingEntity() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).code("F01").build();
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));
        when(repository.existsByCode("F02")).thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> new PartService(repository).patch(1L, Map.of("code", "F02")));

        assertEquals("F01", part.getCode());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-10", "10.999", "100000000000000000", "1E+17", "0.001"})
    void patchRejectsInvalidPricesWithoutSaving(String value) {
        for (String field : new String[]{"costPrice", "salePrice"}) {
            PartRepository repository = mock(PartRepository.class);
            PartEntity part = PartEntity.builder().id(1L).build();
            when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));

            assertThrows(RequestInvalidException.class,
                    () -> new PartService(repository).patch(1L, Map.of(field, value)));

            verify(repository, never()).save(any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.01", "10.99", "99999999999999999.99", "1E+16"})
    void patchAcceptsValidPricesWithoutRounding(String value) {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).build();
        when(repository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(part));

        new PartService(repository).patch(1L, Map.of("costPrice", value, "salePrice", value));

        assertEquals(new BigDecimal(value), part.getCostPrice());
        assertEquals(new BigDecimal(value), part.getSalePrice());
        verify(repository).save(part);
    }
}
