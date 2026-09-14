package br.com.autoshop.util;

import java.util.Arrays;

public enum DocumentType {
    CNPJ,
    CPF;

    public static boolean isValueOf(String value) {
        return Arrays.stream(DocumentType.class.getEnumConstants())
                .anyMatch(e -> e.name().equals(value));
    }
}
