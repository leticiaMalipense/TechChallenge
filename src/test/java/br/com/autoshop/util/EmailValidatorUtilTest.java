package br.com.autoshop.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailValidatorUtilTest {

    @Test
    void acceptsValidEmailAddresses() {
        assertTrue(EmailValidatorUtil.isValidEmail("ana@example.com"));
        assertTrue(EmailValidatorUtil.isValidEmail("first.last+tag@example.co.uk"));
    }

    @Test
    void rejectsMalformedEmailAddresses() {
        assertFalse(EmailValidatorUtil.isValidEmail("invalid-email"));
        assertFalse(EmailValidatorUtil.isValidEmail("ana@"));
        assertFalse(EmailValidatorUtil.isValidEmail("@example.com"));
        //assertFalse(EmailValidatorUtil.isValidEmail("ana@example"));
    }

    @Test
    void acceptsNullBecauseEmailConstraintDoesNotRequireAValue() {
        assertTrue(EmailValidatorUtil.isValidEmail(null));
    }
}
