package br.com.autoshop.util;

import java.util.Arrays;

public enum HoleType {
    Client,
    Attendent,
    Mechanic;

    public static boolean isValueOf(String value) {
        return Arrays.stream(HoleType.class.getEnumConstants())
                .anyMatch(e -> e.name().equals(value));
    }
}
