package br.com.autoshop.service;

import br.com.autoshop.controller.dto.WorkOrderEmailDeliveryDTO;
import br.com.autoshop.controller.dto.WorkOrderResponseDTO;
import br.com.autoshop.exception.QuoteDeliveryException;
import br.com.autoshop.repository.WorkOrderEmailDeliveryRepository;
import br.com.autoshop.repository.model.WorkOrderEmailDeliveryEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.util.EmailDeliveryStatus;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WorkOrderQuoteDeliveryService {

    private final WorkOrderEmailDeliveryRepository deliveryRepository;
    private final EstimatePDFService estimatePDFService;
    private final QuoteEmailService quoteEmailService;
    private final WorkOrderApprovalTokenService approvalTokenService;
    private final String approvalBaseUrl;

    public WorkOrderQuoteDeliveryService(
            WorkOrderEmailDeliveryRepository deliveryRepository,
            EstimatePDFService estimatePDFService,
            QuoteEmailService quoteEmailService,
            WorkOrderApprovalTokenService approvalTokenService,
            @Value("${autoshop.approval.base-url:http://localhost:8080/autoshop/work-order-approvals}")
            String approvalBaseUrl
    ) {
        this.deliveryRepository = deliveryRepository;
        this.estimatePDFService = estimatePDFService;
        this.quoteEmailService = quoteEmailService;
        this.approvalTokenService = approvalTokenService;
        this.approvalBaseUrl = approvalBaseUrl.replaceAll("/+$", "");
    }

    public void sendForApproval(WorkOrderEntity workOrder, WorkOrderResponseDTO response) {
        WorkOrderEmailDeliveryEntity delivery = deliveryRepository.save(WorkOrderEmailDeliveryEntity.builder()
                .workOrder(workOrder)
                .status(EmailDeliveryStatus.PENDING)
                .attemptNumber(Math.toIntExact(deliveryRepository.countByWorkOrder_Id(workOrder.getId()) + 1))
                .createdAt(LocalDateTime.now())
                .build());

        try {
            String token = approvalTokenService.issueToken(workOrder.getId());
            byte[] pdf = estimatePDFService.generatePdf(response);
            quoteEmailService.sendQuote(
                    workOrder.getClient().getEmail(),
                    pdf,
                    "orcamento-" + workOrder.getId() + ".pdf",
                    approvalBaseUrl + "/" + token
            );
            delivery.setStatus(EmailDeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());
        } catch (JRException | IOException | MailException | IllegalArgumentException e) {
            delivery.setStatus(EmailDeliveryStatus.FAILED);
            delivery.setErrorMessage(sanitizeError(e.getMessage()));
            deliveryRepository.save(delivery);
            throw new QuoteDeliveryException(
                    "The work order is awaiting approval, but the quote email could not be sent",
                    EmailDeliveryStatus.FAILED.name(),
                    workOrder.getId(),
                    e
            );
        }

        deliveryRepository.save(delivery);
    }

    public WorkOrderEmailDeliveryDTO getLatestDelivery(UUID workOrderId) {
        return deliveryRepository.findTopByWorkOrder_IdOrderByCreatedAtDesc(workOrderId)
                .map(this::toDto)
                .orElse(null);
    }

    public void revokeApprovalTokens(UUID workOrderId) {
        approvalTokenService.revokeTokens(workOrderId);
    }

    private WorkOrderEmailDeliveryDTO toDto(WorkOrderEmailDeliveryEntity entity) {
        return new WorkOrderEmailDeliveryDTO(
                entity.getId(),
                entity.getStatus(),
                entity.getAttemptNumber(),
                entity.getCreatedAt(),
                entity.getSentAt(),
                entity.getErrorMessage()
        );
    }

    private static String sanitizeError(String message) {
        if (message == null || message.isBlank()) {
            return "Email delivery failed";
        }
        String sanitized = message.replaceAll("[\\r\\n\\p{Cntrl}]", " ");
        return sanitized.substring(0, Math.min(sanitized.length(), 1000));
    }
}
