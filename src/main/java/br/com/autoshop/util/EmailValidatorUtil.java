package br.com.autoshop.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;

import java.util.Set;

public class EmailValidatorUtil {
    public static final String VALID_EMAIL_ADDRESS = "Please provide a valid email address";

    private static final Validator validator;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    public static boolean isValidEmail(String email) {
        Set<ConstraintViolation<EmailHolder>> violations =
                validator.validateValue(EmailHolder.class, "email", email);

        return violations.isEmpty();
    }

    private static class EmailHolder {
        @Email
        String email;
    }
}