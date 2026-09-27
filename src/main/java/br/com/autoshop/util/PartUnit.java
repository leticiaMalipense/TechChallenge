package br.com.autoshop.util;

public enum PartUnit {
    UNIT,
    KILOGRAM,
    LITER,
    METER;

    public static boolean isValueOf(String value) {
        if(value == null) {
            return false;
        }
        for (PartUnit partUnit : PartUnit.values()) {
            if (partUnit.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
