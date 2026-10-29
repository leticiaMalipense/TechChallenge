package br.com.autoshop.controller.dto;

import br.com.autoshop.util.EmailDeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkOrderEmailDeliveryDTO(
        UUID id,
        EmailDeliveryStatus status,
        Integer attemptNumber,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        String errorMessage
) {
}
