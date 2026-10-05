package br.com.autoshop.controller;

import br.com.autoshop.dto.ClientDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.ClientService;
import br.com.autoshop.util.DocumentType;
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
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void postClientReturnsCreatedAndLocationWhenDocumentIsNew() {
        setRequest("POST", "/client");
        when(clientService.getByDocument("123")).thenReturn(null);
        when(clientService.save(any(ClientDTO.class))).thenReturn(42L);

        var response = clientController.postClient(dto());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost/client/42", response.getHeaders().getLocation().toString());
        verify(clientService).save(any(ClientDTO.class));
    }

    @Test
    void postClientReturnsConflictWhenDocumentAlreadyExists() {
        when(clientService.getByDocument("123")).thenReturn(dto());

        var response = clientController.postClient(dto());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(clientService, never()).save(any());
    }

    @Test
    void getClientReturnsOkWhenFound() {
        ClientDTO expected = dto();
        when(clientService.getById(1L)).thenReturn(expected);

        var response = clientController.getClient(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void getClientReturnsNotFoundWhenMissing() {
        when(clientService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, clientController.getClient(1L).getStatusCode());
    }

    @Test
    void getAllReturnsPageAndUsesRequestedDescendingSort() {
        Page<ClientDTO> expected = new PageImpl<>(List.of(dto()));
        when(clientService.getAll(any(Pageable.class))).thenReturn(expected);

        var response = clientController.getAll(2, 5, "email");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(clientService).getAll(pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertTrue(pageable.getValue().getSort().getOrderFor("email").isDescending());
    }

    @Test
    void putUpdatesFoundClient() {
        when(clientService.getById(1L)).thenReturn(dto());

        var response = clientController.put(1L, dto());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(clientService).put(eq(1L), any(ClientDTO.class));
    }

    @Test
    void putReturnsNoContentWhenClientDoesNotExist() {
        when(clientService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NO_CONTENT, clientController.put(1L, dto()).getStatusCode());
        verify(clientService, never()).put(anyLong(), any());
    }

    @Test
    void patchUpdatesFoundClientAndPropagatesInvalidRequest() {
        when(clientService.getById(1L)).thenReturn(dto());
        Map<String, Object> fields = Map.of("name", "Maria");

        var response = clientController.patch(1L, fields);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(clientService).patch(1L, fields);

        doThrow(new RequestInvalidException("invalid field")).when(clientService).patch(1L, fields);
        assertThrows(RequestInvalidException.class, () -> clientController.patch(1L, fields));
    }

    @Test
    void patchReturnsNoContentWhenClientDoesNotExist() {
        when(clientService.getById(1L)).thenReturn(null);

        assertEquals(HttpStatus.NO_CONTENT, clientController.patch(1L, Map.of("name", "Maria")).getStatusCode());
        verify(clientService, never()).patch(anyLong(), any());
    }

    @Test
    void deleteRemovesExistingActiveClient() {
        when(clientService.isClientExistById(1L)).thenReturn(true);

        assertEquals(HttpStatus.OK, clientController.deleteClient(1L).getStatusCode());
        verify(clientService).deleteById(1L);
    }

    @Test
    void deleteReturnsNoContentWhenClientDoesNotExistOrIsInactive() {
        when(clientService.isClientExistById(1L)).thenReturn(false);

        assertEquals(HttpStatus.NO_CONTENT, clientController.deleteClient(1L).getStatusCode());
        verify(clientService, never()).deleteById(anyLong());
    }

    private static ClientDTO dto() {
        return ClientDTO.builder().name("Ana Silva").document("123").documentType(DocumentType.CPF)
                .email("ana@example.com").phone("11999999999").build();
    }

    private static void setRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServerName("localhost");
        request.setScheme("http");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
