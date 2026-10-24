package br.com.autoshop.controller.dto.validators.document;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CPFOrCNPJValidator implements ConstraintValidator<CPFOrCNPJ, String> {
    public static final int CPF_LENGTH = 11;
    public static final int CNPJ_LENGTH = 14;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String digits = value.replaceAll("[^0-9]", "");

        CPFValidator cpfValidator = new CPFValidator();
        CNPJValidator cnpjValidator = new CNPJValidator();

        try {
            if (digits.length() == 11) {
                cpfValidator.assertValid(digits);
                return true;
            } else if (digits.length() == 14) {
                cnpjValidator.assertValid(digits);
                return true;
            }

            return false;

        } catch (InvalidStateException e) {
            return false;
        }
    }
}
