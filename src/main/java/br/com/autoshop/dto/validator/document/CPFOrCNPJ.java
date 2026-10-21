package br.com.autoshop.dto.validator.document;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CPFOrCNPJValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CPFOrCNPJ {

    String message() default "Document invalid, it should be a CPF or CNPJ.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}