package br.com.autoshop.dto.validators.plateNumber;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlateNumberValidator implements ConstraintValidator<PlateNumber, String> {

    // Regex
    // Model ABC-1234 or ABC1234
    // Model Mercosul: ABC1D23 ou ABC-1D23
    private static final String PLACA_REGEX = "^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String placaFormatada = value.trim().toUpperCase();
        return placaFormatada.matches(PLACA_REGEX);
    }
}