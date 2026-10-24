package br.com.autoshop.controller;

import br.com.autoshop.Helper.VehicleHelper;
import br.com.autoshop.controller.dto.VehicleDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.VehicleService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest extends VehicleHelper {

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleController vehicleController;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void postVehicleReturnsCreatedAndLocationWhenPlateIsNew() {
        setRequest("POST", "/vehicle");
        when(vehicleService.getByPlateNumber(DEFAULT_PLATE)).thenReturn(null);
        when(vehicleService.save(any(VehicleDTO.class))).thenReturn(DEFAULT_PLATE);

        var response = vehicleController.postVehicle(createVehicleDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost/vehicle/" + DEFAULT_PLATE,
                response.getHeaders().getLocation().toString());
        verify(vehicleService).save(any(VehicleDTO.class));
    }

    @Test
    void postVehicleReturnsConflictWhenPlateAlreadyExists() {
        when(vehicleService.getByPlateNumber(DEFAULT_PLATE)).thenReturn(createVehicleDTO());

        var response = vehicleController.postVehicle(createVehicleDTO());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(vehicleService, never()).save(any());
    }

    @Test
    void getVehicleReturnsOkWhenFound() {
        VehicleDTO expected = createVehicleDTO();
        when(vehicleService.getByPlateNumber(DEFAULT_PLATE)).thenReturn(expected);

        var response = vehicleController.getVehicle(DEFAULT_PLATE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void getVehicleReturnsNotFoundWhenMissing() {
        when(vehicleService.getByPlateNumber("MISSING")).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, vehicleController.getVehicle("MISSING").getStatusCode());
    }

    @Test
    void getAllReturnsPageAndUsesRequestedDescendingSort() {
        Page<VehicleDTO> expected = new PageImpl<>(List.of(createVehicleDTO()));
        when(vehicleService.getAll(any(Pageable.class))).thenReturn(expected);

        var response = vehicleController.getAll(2, 5, "lastReview");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(vehicleService).getAll(pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertTrue(pageable.getValue().getSort().getOrderFor("lastReview").isDescending());
    }

    @Test
    void putUpdatesFoundVehicle() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(true);

        var response = vehicleController.put(DEFAULT_PLATE, createVehicleDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vehicleService).put(eq(DEFAULT_PLATE), any(VehicleDTO.class));
    }

    @Test
    void putReturnsNoContentWhenVehicleDoesNotExist() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(false);

        var response = vehicleController.put(DEFAULT_PLATE, createVehicleDTO());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleService, never()).put(anyString(), any());
    }

    @Test
    void patchUpdatesFoundVehicle() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(true);
        Map<String, Object> fields = Map.of("plateNumber", "XYZ9876");

        var response = vehicleController.patch(DEFAULT_PLATE, fields);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vehicleService).patch(DEFAULT_PLATE, fields);
    }

    @Test
    void patchThrowsExceptionWhenServiceThrowsRequestInvalidException() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(true);
        Map<String, Object> fields = Map.of("lastReview", "invalid");
        doThrow(new RequestInvalidException(VehicleService.LAST_REVIEW_IS_INVALID))
                .when(vehicleService).patch(DEFAULT_PLATE, fields);

        assertThrows(RequestInvalidException.class, () -> vehicleController.patch(DEFAULT_PLATE, fields));
    }

    @Test
    void patchReturnsNoContentWhenVehicleDoesNotExist() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(false);

        var response = vehicleController.patch(DEFAULT_PLATE, Map.of("plateNumber", "XYZ9876"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleService, never()).patch(anyString(), any());
    }

    @Test
    void deleteVehicleRemovesExistingVehicle() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(true);

        var response = vehicleController.deleteVehicle(DEFAULT_PLATE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vehicleService).deleteByPlateNumber(DEFAULT_PLATE);
    }

    @Test
    void deleteVehicleReturnsNoContentWhenVehicleDoesNotExist() {
        when(vehicleService.isVehicleExistByPlateNumber(DEFAULT_PLATE)).thenReturn(false);

        var response = vehicleController.deleteVehicle(DEFAULT_PLATE);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleService, never()).deleteByPlateNumber(anyString());
    }

    private static void setRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServerName("localhost");
        request.setScheme("http");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
