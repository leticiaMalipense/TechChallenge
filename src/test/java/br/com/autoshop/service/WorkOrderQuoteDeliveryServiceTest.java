package br.com.autoshop.service;

import br.com.autoshop.controller.dto.WorkOrderResponseDTO;
import br.com.autoshop.exception.QuoteDeliveryException;
import br.com.autoshop.repository.WorkOrderEmailDeliveryRepository;
import br.com.autoshop.repository.model.ClientEntity;
import br.com.autoshop.repository.model.WorkOrderEmailDeliveryEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.util.EmailDeliveryStatus;
import br.com.autoshop.util.WorkOrderStatus;
import net.sf.jasperreports.engine.JRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class WorkOrderQuoteDeliveryServiceTest {

    @Mock
    private WorkOrderEmailDeliveryRepository deliveryRepository;
    @Mock
    private EstimatePDFService estimatePDFService;
    @Mock
    private QuoteEmailService quoteEmailService;
    @Mock
    private WorkOrderApprovalTokenService approvalTokenService;

    private WorkOrderQuoteDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderQuoteDeliveryService(
                deliveryRepository,
                estimatePDFService,
                quoteEmailService,
                approvalTokenService,
                "https://autoshop.example/work-order-approvals"
        );
        lenient().when(deliveryRepository.countByWorkOrder_Id(any())).thenReturn(0L);
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendsPdfToClientAndRecordsSuccessfulDelivery() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] pdf = "%PDF-test".getBytes();
        WorkOrderEntity order = order(id);
        WorkOrderResponseDTO response = WorkOrderResponseDTO.builder()
                .id(id)
                .status(WorkOrderStatus.AWAITING_APPROVAL)
                .build();
        when(approvalTokenService.issueToken(id)).thenReturn("opaque-token");
        when(estimatePDFService.generatePdf(response)).thenReturn(pdf);

        service.sendForApproval(order, response);

        verify(quoteEmailService).sendQuote(
                eq("customer@example.com"),
                eq(pdf),
                eq("orcamento-" + id + ".pdf"),
                eq("https://autoshop.example/work-order-approvals/opaque-token")
        );
        ArgumentCaptor<WorkOrderEmailDeliveryEntity> delivery =
                ArgumentCaptor.forClass(WorkOrderEmailDeliveryEntity.class);
        verify(deliveryRepository, times(2)).save(delivery.capture());
        assertEquals(EmailDeliveryStatus.SENT, delivery.getValue().getStatus());
        assertEquals(1, delivery.getValue().getAttemptNumber());
        assertNotNull(delivery.getValue().getSentAt());
    }

    @Test
    void recordsSmtpFailureAndAllowsCallerToRetry() throws JRException, IOException {
        UUID id = UUID.randomUUID();
        WorkOrderEntity order = order(id);
        WorkOrderResponseDTO response = WorkOrderResponseDTO.builder().id(id).build();
        when(approvalTokenService.issueToken(id)).thenReturn("failed-token", "retry-token");
        when(deliveryRepository.countByWorkOrder_Id(id)).thenReturn(0L, 1L);
        when(estimatePDFService.generatePdf(response)).thenReturn("%PDF-test".getBytes());
        doThrow(new MailSendException("SMTP unavailable"))
                .doNothing()
                .when(quoteEmailService).sendQuote(anyString(), any(), anyString(), anyString());

        QuoteDeliveryException exception = assertThrows(
                QuoteDeliveryException.class,
                () -> service.sendForApproval(order, response)
        );

        assertEquals(EmailDeliveryStatus.FAILED.name(), exception.getDeliveryStatus());
        assertEquals(id, exception.getWorkOrderId());
        assertEquals(WorkOrderStatus.AWAITING_APPROVAL, order.getStatus());

        service.sendForApproval(order, response);

        ArgumentCaptor<WorkOrderEmailDeliveryEntity> delivery =
                ArgumentCaptor.forClass(WorkOrderEmailDeliveryEntity.class);
        verify(deliveryRepository, times(4)).save(delivery.capture());
        assertEquals(EmailDeliveryStatus.FAILED, delivery.getAllValues().get(1).getStatus());
        assertEquals(EmailDeliveryStatus.SENT, delivery.getAllValues().get(3).getStatus());
        assertEquals(2, delivery.getAllValues().get(3).getAttemptNumber());
    }

    private static WorkOrderEntity order(UUID id) {
        return WorkOrderEntity.builder()
                .id(id)
                .client(ClientEntity.builder().email("customer@example.com").build())
                .status(WorkOrderStatus.AWAITING_APPROVAL)
                .build();
    }
}
