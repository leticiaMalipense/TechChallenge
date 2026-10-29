package br.com.autoshop.controller;

import br.com.autoshop.exception.InvalidApprovalTokenException;
import br.com.autoshop.service.WorkOrderApprovalTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderApprovalControllerTest {

    @Mock
    private WorkOrderApprovalTokenService approvalTokenService;

    private WorkOrderApprovalController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new WorkOrderApprovalController(
                approvalTokenService,
                "https://autoshop.example/work-order-approvals"
        );
    }

    @Test
    void openingLinkOnlyDisplaysConfirmationAndDoesNotApprove() {
        when(approvalTokenService.isValid("token")).thenReturn(true);

        var response = controller.showConfirmation("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("method=\"post\""));
        assertTrue(response.getBody().contains("https://autoshop.example/work-order-approvals/token"));
        assertTrue(response.getHeaders().getFirst("Cache-Control").contains("no-store"));
        assertEquals("no-referrer", response.getHeaders().getFirst("Referrer-Policy"));
        verify(approvalTokenService, never()).approve(anyString());
    }

    @Test
    void explicitPostConfirmsApproval() {
        var response = controller.confirmApproval("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Orçamento aprovado"));
        verify(approvalTokenService).approve("token");
    }

    @Test
    void invalidTokenReturnsExpiredLinkPage() {
        when(approvalTokenService.isValid("invalid")).thenReturn(false);

        var response = controller.showConfirmation("invalid");

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertTrue(response.getBody().contains("inválido ou expirou"));
    }

    @Test
    void postInvalidTokenReturnsGone() {
        doThrow(new InvalidApprovalTokenException()).when(approvalTokenService).approve("invalid");

        assertThrows(InvalidApprovalTokenException.class, () -> controller.confirmApproval("invalid"));
        var response = controller.handleInvalidToken();

        assertEquals(HttpStatus.GONE, response.getStatusCode());
    }
}
