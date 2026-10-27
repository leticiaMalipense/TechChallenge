package br.com.autoshop.controller;

import br.com.autoshop.exception.AlreadyExistsException;
import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import java.math.BigDecimal;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.exception.RequestInvalidException;
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
        when(partService.getById(1L)).thenReturn(dto());

        var response = partController.put(1L, dto());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(partService).put(eq(1L), any(PartDTO.class));
    }

    @Test
    void putReturnsNotFoundWhenPartDoesNotExist() {
        when(partService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, partController.put(1L, dto()).getStatusCode());
        verify(partService, never()).put(anyLong(), any());
    }

    @Test
    void patchUpdatesFoundPartAndPropagatesInvalidRequest() {
        when(partService.getById(1L)).thenReturn(dto());
        Map<String, Object> fields = Map.of("name", "Alinhamento");

        var response = partController.patch(1L, fields);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(partService).patch(1L, fields);

        doThrow(new RequestInvalidException("invalid field")).when(partService).patch(1L, fields);
        assertThrows(RequestInvalidException.class, () -> partController.patch(1L, fields));
    }

    @Test
    void patchReturnsNotFoundWhenPartDoesNotExist() {
        when(partService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, partController.patch(1L, Map.of("name", "Alinhamento")).getStatusCode());
        verify(partService, never()).patch(anyLong(), any());
    }

    @Test
    void deleteRemovesExistingActivePart() {
        when(partService.isPartExistById(1L)).thenReturn(true);

        assertEquals(HttpStatus.OK, partController.deletePart(1L).getStatusCode());
        verify(partService).deleteById(1L);
    }

    @Test
    void deleteReturnsNotFoundWhenPartDoesNotExistOrIsInactive() {
        when(partService.isPartExistById(1L)).thenReturn(false);

        assertEquals(HttpStatus.NOT_FOUND, partController.deletePart(1L).getStatusCode());
        verify(partService, never()).deleteById(anyLong());
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
