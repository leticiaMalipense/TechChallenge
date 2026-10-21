package br.com.autoshop.controller;

import br.com.autoshop.dto.WorkOrderRequestDTO;
import br.com.autoshop.dto.WorkOrderResponseDTO;
import br.com.autoshop.dto.WorkOrderTrackingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.WorkOrderService;
import br.com.autoshop.util.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderControllerTest {

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private WorkOrderController workOrderController;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createReturnsCreatedWithLocationAndBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/work-orders");
        request.setServerName("localhost");
        request.setScheme("http");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UUID id = UUID.randomUUID();
        WorkOrderResponseDTO expected = WorkOrderResponseDTO.builder()
                .id(id)
                .status(WorkOrderStatus.RECEIVED)
                .build();

        when(workOrderService.create(any(WorkOrderRequestDTO.class))).thenReturn(expected);

        var response = workOrderController.create(new WorkOrderRequestDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost/work-orders/" + id, response.getHeaders().getLocation().toString());
        assertSame(expected, response.getBody());
    }

    @Test
    void getByIdReturnsOkWhenFoundAndNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        WorkOrderResponseDTO dto = WorkOrderResponseDTO.builder().id(id).build();
        when(workOrderService.getById(id)).thenReturn(dto);

        var okResponse = workOrderController.getById(id);
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());
        assertSame(dto, okResponse.getBody());

        UUID missingId = UUID.randomUUID();
        when(workOrderService.getById(missingId)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, workOrderController.getById(missingId).getStatusCode());
    }

    @Test
    void trackWorkOrderDelegatesToServiceAndReturnsTrackingDto() {
        UUID id = UUID.randomUUID();
        WorkOrderTrackingDTO trackingDto = WorkOrderTrackingDTO.builder()
                .id(id)
                .status(WorkOrderStatus.IN_EXECUTION)
                .serviceNames(List.of("Troca de Óleo"))
                .build();

        when(workOrderService.trackWorkOrder(id, "123")).thenReturn(trackingDto);

        var response = workOrderController.trackWorkOrder(id, "123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(trackingDto, response.getBody());
        verify(workOrderService).trackWorkOrder(id, "123");
    }

    @Test
    void handleRequestInvalidExceptionReturnsBadRequestWithErrorMessage() {
        RequestInvalidException ex = new RequestInvalidException(WorkOrderService.SERVICES_REQUIRED_FOR_APPROVAL);

        var response = workOrderController.handleRequestInvalidException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(WorkOrderService.SERVICES_REQUIRED_FOR_APPROVAL, response.getBody().get("error"));
    }
}
