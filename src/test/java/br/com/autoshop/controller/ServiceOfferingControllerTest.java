package br.com.autoshop.controller;

import br.com.autoshop.controller.dto.ServiceOfferingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.ServiceOfferingService;
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
class ServiceOfferingControllerTest {

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @InjectMocks
    private ServiceOfferingController serviceOfferingController;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void postServiceOfferingReturnsCreatedAndLocationWhenNameIsNew() {
        setRequest("POST", "/service-offering");
        when(serviceOfferingService.getByName("Troca de óleo")).thenReturn(null);
        when(serviceOfferingService.save(any(ServiceOfferingDTO.class))).thenReturn(42L);

        var response = serviceOfferingController.postServiceOffering(dto());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost/service-offering/42", response.getHeaders().getLocation().toString());
        verify(serviceOfferingService).save(any(ServiceOfferingDTO.class));
    }

    @Test
    void postServiceOfferingReturnsConflictWhenNameAlreadyExists() {
        when(serviceOfferingService.getByName("Troca de óleo")).thenReturn(dto());

        var response = serviceOfferingController.postServiceOffering(dto());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(serviceOfferingService, never()).save(any());
    }

    @Test
    void getServiceOfferingReturnsOkWhenFound() {
        ServiceOfferingDTO expected = dto();
        when(serviceOfferingService.getById(1L)).thenReturn(expected);

        var response = serviceOfferingController.getServiceOffering(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void getServiceOfferingReturnsNotFoundWhenMissing() {
        when(serviceOfferingService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, serviceOfferingController.getServiceOffering(1L).getStatusCode());
    }

    @Test
    void getAllReturnsPageAndUsesRequestedDescendingSort() {
        Page<ServiceOfferingDTO> expected = new PageImpl<>(List.of(dto()));
        when(serviceOfferingService.getAll(any(Pageable.class))).thenReturn(expected);

        var response = serviceOfferingController.getAll(2, 5, "category");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(serviceOfferingService).getAll(pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertTrue(pageable.getValue().getSort().getOrderFor("category").isDescending());
    }

    @Test
    void putUpdatesFoundServiceOffering() {
        when(serviceOfferingService.getById(1L)).thenReturn(dto());

        var response = serviceOfferingController.put(1L, dto());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceOfferingService).put(eq(1L), any(ServiceOfferingDTO.class));
    }

    @Test
    void putReturnsNoContentWhenServiceOfferingDoesNotExist() {
        when(serviceOfferingService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NO_CONTENT, serviceOfferingController.put(1L, dto()).getStatusCode());
        verify(serviceOfferingService, never()).put(anyLong(), any());
    }

    @Test
    void patchUpdatesFoundServiceOfferingAndPropagatesInvalidRequest() {
        when(serviceOfferingService.getById(1L)).thenReturn(dto());
        Map<String, Object> fields = Map.of("name", "Alinhamento");

        var response = serviceOfferingController.patch(1L, fields);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceOfferingService).patch(1L, fields);

        doThrow(new RequestInvalidException("invalid field")).when(serviceOfferingService).patch(1L, fields);
        assertThrows(RequestInvalidException.class, () -> serviceOfferingController.patch(1L, fields));
    }

    @Test
    void patchReturnsNoContentWhenServiceOfferingDoesNotExist() {
        when(serviceOfferingService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NO_CONTENT, serviceOfferingController.patch(1L, Map.of("name", "Alinhamento")).getStatusCode());
        verify(serviceOfferingService, never()).patch(anyLong(), any());
    }

    @Test
    void deleteRemovesExistingActiveServiceOffering() {
        when(serviceOfferingService.isServiceOfferingExistById(1L)).thenReturn(true);

        assertEquals(HttpStatus.OK, serviceOfferingController.deleteServiceOffering(1L).getStatusCode());
        verify(serviceOfferingService).deleteById(1L);
    }

    @Test
    void deleteReturnsNoContentWhenServiceOfferingDoesNotExistOrIsInactive() {
        when(serviceOfferingService.isServiceOfferingExistById(1L)).thenReturn(false);

        assertEquals(HttpStatus.NO_CONTENT, serviceOfferingController.deleteServiceOffering(1L).getStatusCode());
        verify(serviceOfferingService, never()).deleteById(anyLong());
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

    private static void setRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServerName("localhost");
        request.setScheme("http");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
