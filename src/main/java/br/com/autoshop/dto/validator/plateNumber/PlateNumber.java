package br.com.autoshop.dto.validator.plateNumber;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PlateNumberValidator.class) // Aponta para a classe que valida
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlateNumber {

    String message() default "Placa de carro inválida"; // Mensagem padrão

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
