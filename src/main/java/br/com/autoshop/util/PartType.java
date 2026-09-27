package br.com.autoshop.util;

public enum PartType {
    PART,
    SUPPLY;

    public static boolean isValueOf(String value) {
        if(value == null) {
            return false;
        }
        for (PartType partType : PartType.values()) {
            if (partType.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
    
}
