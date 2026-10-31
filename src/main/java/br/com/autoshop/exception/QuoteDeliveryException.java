package br.com.autoshop.exception;

import java.util.UUID;

public class QuoteDeliveryException extends RuntimeException {

    private final String deliveryStatus;
    private final UUID workOrderId;

    public QuoteDeliveryException(String message, String deliveryStatus, UUID workOrderId, Throwable cause) {
        super(message, cause);
        this.deliveryStatus = deliveryStatus;
        this.workOrderId = workOrderId;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }
}
