package br.com.autoshop.controller.dto.validators.plateNumber;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PlateNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlateNumber {

    String message() default "Vehicle plate number is invalid"; // Mensagem padrão

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
