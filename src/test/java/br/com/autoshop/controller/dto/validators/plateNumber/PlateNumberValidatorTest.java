package br.com.autoshop.controller.dto.validators.plateNumber;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateNumberValidatorTest {

    private PlateNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlateNumberValidator();
    }

    @Test
    void acceptsValidOldFormatWithAndWithoutHyphen() {
        assertTrue(isValid("ABC-1234"));
        assertTrue(isValid("ABC1234"));
    }

    @Test
    void acceptsValidMercosulFormatWithAndWithoutHyphen() {
        assertTrue(isValid("ABC-1D23"));
        assertTrue(isValid("ABC1D23"));
    }

    @Test
    void ignoresCaseAndSurroundingWhitespace() {
        assertTrue(isValid(" abc-1d23 "));
    }

    @Test
    void acceptsNullAndBlankValues() {
        assertTrue(isValid(null));
        assertTrue(isValid(""));
        assertTrue(isValid("   "));
    }

    @Test
    void rejectsPlatesWithInvalidCharactersOrStructure() {
        assertFalse(isValid("AB-1234"));
        assertFalse(isValid("ABCD123"));
        assertFalse(isValid("ABC-1D2E"));
        assertFalse(isValid("123-ABCD"));
    }

    private boolean isValid(String value) {
        return validator.isValid(value, (ConstraintValidatorContext) null);
    }
}
