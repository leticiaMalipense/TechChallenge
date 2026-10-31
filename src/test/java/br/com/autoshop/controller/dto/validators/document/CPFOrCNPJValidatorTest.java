package br.com.autoshop.controller.dto.validators.document;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CPFOrCNPJValidatorTest {

    private CPFOrCNPJValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CPFOrCNPJValidator();
    }

    @Test
    void acceptsValidCpfWithAndWithoutFormatting() {
        assertTrue(isValid("52998224725"));
        assertTrue(isValid("529.982.247-25"));
    }

    @Test
    void acceptsValidCnpjWithAndWithoutFormatting() {
        assertTrue(isValid("11222333000181"));
        assertTrue(isValid("11.222.333/0001-81"));
    }

    @Test
    void acceptsNullAndBlankValues() {
        assertTrue(isValid(null));
        assertTrue(isValid(""));
        assertTrue(isValid("   "));
    }

    @Test
    void rejectsInvalidCpfAndCnpjChecksums() {
        assertFalse(isValid("52998224724"));
        assertFalse(isValid("11222333000180"));
    }

    @Test
    void rejectsValuesWithUnsupportedNumberOfDigits() {
        assertFalse(isValid("1234567890"));
        assertFalse(isValid("123456789012345"));
    }

    private boolean isValid(String value) {
        return validator.isValid(value, (ConstraintValidatorContext) null);
    }
}
