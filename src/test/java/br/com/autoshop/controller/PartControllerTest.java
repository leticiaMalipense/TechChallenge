package br.com.autoshop.controller;

import br.com.autoshop.exception.AlreadyExistsException;
import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import java.math.BigDecimal;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.controller.dto.StockMovementDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.exception.PartNotFoundException;
import br.com.autoshop.service.PartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartControllerTest {

    @Mock
    private PartService partService;

    @InjectMocks
    private PartController partController;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void postPartReturnsCreatedAndLocationWhenNameIsNew() {
        setRequest("POST", "/part");
        when(partService.save(any(PartDTO.class))).thenReturn(42L);

        var response = partController.postPart(dto());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost/part/42", response.getHeaders().getLocation().toString());
        verify(partService).save(any(PartDTO.class));
    }

    @Test
    void postPartPropagatesDuplicateCode() {
        when(partService.save(any(PartDTO.class)))
                .thenThrow(new AlreadyExistsException("Part code already exists"));

        assertThrows(AlreadyExistsException.class,
                () -> partController.postPart(dto()));
    }

    @Test
    void getPartReturnsOkWhenFound() {
        PartDTO expected = dto();
        when(partService.getById(1L)).thenReturn(expected);

        var response = partController.getPart(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void getPartReturnsNotFoundWhenMissing() {
        when(partService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, partController.getPart(1L).getStatusCode());
    }

    @Test
    void getAllReturnsPageAndUsesRequestedDescendingSort() {
        Page<PartDTO> expected = new PageImpl<>(List.of(dto()));
        when(partService.getAll(any(Pageable.class))).thenReturn(expected);

        var response = partController.getAll(2, 5, "category");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(partService).getAll(pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertTrue(pageable.getValue().getSort().getOrderFor("category").isDescending());
    }

    @Test
    void putUpdatesFoundPart() {

        var response = partController.put(1L, dto());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(partService).put(eq(1L), any(PartDTO.class));
    }

    @Test
    void putPropagatesNotFoundFromTransactionalUpdate() {
        doThrow(new PartNotFoundException("Part not found or inactive"))
                .when(partService).put(eq(1L), any());

        assertThrows(PartNotFoundException.class, () -> partController.put(1L, dto()));
        verify(partService, never()).getById(anyLong());
    }

    @Test
    void patchUpdatesFoundPartAndPropagatesInvalidRequest() {
        Map<String, Object> fields = Map.of("name", "Alinhamento");

        var response = partController.patch(1L, fields);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(partService).patch(1L, fields);

        doThrow(new RequestInvalidException("invalid field")).when(partService).patch(1L, fields);
        assertThrows(RequestInvalidException.class, () -> partController.patch(1L, fields));
    }

    @Test
    void patchPropagatesNotFoundFromTransactionalUpdate() {
        doThrow(new PartNotFoundException("Part not found or inactive"))
                .when(partService).patch(eq(1L), any());

        assertThrows(PartNotFoundException.class,
                () -> partController.patch(1L, Map.of("name", "Updated")));
        verify(partService, never()).getById(anyLong());
    }

    @Test
    void deleteRemovesExistingActivePart() {

        assertEquals(HttpStatus.OK, partController.deletePart(1L).getStatusCode());
        verify(partService).deleteById(1L);
    }

    @Test
    void deletePropagatesNotFoundFromTransactionalUpdate() {
        doThrow(new PartNotFoundException("Part not found or inactive"))
                .when(partService).deleteById(1L);

        assertThrows(PartNotFoundException.class, () -> partController.deletePart(1L));
        verify(partService, never()).isPartExistById(anyLong());
    }

    @Test
    void stockEntryReturnsUpdatedBalance() {
        PartDTO expected = dto();
        expected.setStockQuantity(BigDecimal.TEN);
        when(partService.addStock(1L, BigDecimal.TEN)).thenReturn(expected);

        var response = partController.addStock(1L, StockMovementDTO.builder().quantity(BigDecimal.TEN).build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void stockExitReturnsUpdatedBalance() {
        PartDTO expected = dto();
        expected.setStockQuantity(BigDecimal.ZERO);
        when(partService.removeStock(1L, BigDecimal.TEN)).thenReturn(expected);

        var response = partController.removeStock(1L, StockMovementDTO.builder().quantity(BigDecimal.TEN).build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void stockMovementsReturnNotFoundForUnavailablePart() {
        StockMovementDTO movement = StockMovementDTO.builder().quantity(BigDecimal.ONE).build();

        assertEquals(HttpStatus.NOT_FOUND, partController.addStock(1L, movement).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, partController.removeStock(1L, movement).getStatusCode());
    }

    private static PartDTO dto() {
        return PartDTO.builder()
                .name("Troca de óleo")
                .description("Troca de óleo do motor")
                .code("OIL01")
                .partType(PartType.SUPPLY)
                .unit(PartUnit.LITER)
                .costPrice(BigDecimal.TEN)
                .salePrice(BigDecimal.valueOf(20))
                .category("Manutenção")
                .build();
    }

    private static void setRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServerName("localhost");
        request.setScheme("http");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
