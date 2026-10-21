package br.com.autoshop.util;

import java.util.Arrays;

public enum WorkOrderStatus {
    RECEIVED,
    IN_DIAGNOSIS,
    AWAITING_APPROVAL,
    IN_EXECUTION,
    FINISHED,
    DELIVERED;

    public static boolean isValueOf(String value) {
        return Arrays.stream(WorkOrderStatus.class.getEnumConstants())
                .anyMatch(e -> e.name().equals(value));
    }
}
