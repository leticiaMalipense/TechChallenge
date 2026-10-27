package br.com.autoshop.service;

import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.PartRepository;
import br.com.autoshop.repository.model.PartEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartStockServiceTest {

    @Test
    void newPartStartsWithZeroStock() {
        assertEquals(BigDecimal.ZERO, PartEntity.builder().build().getStockQuantity());
    }

    @Test
    void entryAddsFractionalQuantityAndExitCanConsumeEntireBalance() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().id(1L).stockQuantity(new BigDecimal("2.5")).build();
        when(repository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(part));
        PartService service = new PartService(repository);

        assertEquals(new BigDecimal("3.25"), service.addStock(1L, new BigDecimal("0.75")).getStockQuantity());
        assertEquals(0, service.removeStock(1L, new BigDecimal("3.25")).getStockQuantity().signum());
        verify(repository, times(2)).save(part);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "0.0001", "10000000000000000"})
    void rejectsInvalidQuantityBeforeAccessingRepository(String value) {
        PartRepository repository = mock(PartRepository.class);
        PartService service = new PartService(repository);
        BigDecimal quantity = value == null ? null : new BigDecimal(value);

        assertThrows(RequestInvalidException.class, () -> service.addStock(1L, quantity));
        assertThrows(RequestInvalidException.class, () -> service.removeStock(1L, quantity));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInsufficientStockWithoutChangingBalance() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().stockQuantity(BigDecimal.ONE).build();
        when(repository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(part));

        assertThrows(RequestInvalidException.class,
                () -> new PartService(repository).removeStock(1L, BigDecimal.TEN));

        assertEquals(BigDecimal.ONE, part.getStockQuantity());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsBalanceOverflowWithoutSaving() {
        PartRepository repository = mock(PartRepository.class);
        PartEntity part = PartEntity.builder().stockQuantity(new BigDecimal("9999999999999999.999")).build();
        when(repository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(part));

        assertThrows(RequestInvalidException.class,
                () -> new PartService(repository).addStock(1L, BigDecimal.ONE));

        assertEquals(new BigDecimal("9999999999999999.999"), part.getStockQuantity());
        verify(repository, never()).save(any());
    }

    @Test
    void unavailablePartCannotReceiveMovements() {
        PartRepository repository = mock(PartRepository.class);
        PartService service = new PartService(repository);

        assertNull(service.addStock(1L, BigDecimal.ONE));
        assertNull(service.removeStock(1L, BigDecimal.ONE));
        verify(repository, never()).save(any());
    }
}
