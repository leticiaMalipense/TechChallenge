package br.com.autoshop.exception;

import br.com.autoshop.controller.PartController;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Set;

@RestControllerAdvice(assignableTypes = PartController.class)
public class PartExceptionHandler {

    // Keep compatibility with databases created before the constraint received an explicit name.
    private static final Set<String> CODE_CONSTRAINTS = Set.of("uk_part_code", "uk62uo6x993lqg2jjk3k8u0n8v5");

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Map<String, String> handleCodeConflict(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && CODE_CONSTRAINTS.contains(violation.getConstraintName())) {
                return Map.of("error", "Part code already exists");
            }
        }
        throw exception;
    }
}
