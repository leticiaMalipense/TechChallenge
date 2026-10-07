package br.com.autoshop.util;

import java.util.Arrays;

public enum RoleType {
    ADMIN,
    Client,
    Attendent,
    Mechanic;

    public static boolean isValueOf(String value) {
        return Arrays.stream(RoleType.class.getEnumConstants())
                .anyMatch(e -> e.name().equals(value));
    }
}
