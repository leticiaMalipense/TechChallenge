package br.com.autoshop.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class PartExceptionHandlerTest {

    @ParameterizedTest
    @ValueSource(strings = {"uk_part_code", "uk62uo6x993lqg2jjk3k8u0n8v5"})
    void recognizesOnlyPartCodeUniqueConstraints(String constraint) {
        var cause = new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"), constraint);
        var exception = new DataIntegrityViolationException("duplicate", cause);

        assertEquals("Part code already exists", new PartExceptionHandler().handleCodeConflict(exception).get("error"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"part_pkey", "another_unique_constraint"})
    void propagatesOtherUniqueConstraints(String constraint) {
        var cause = new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"), constraint);
        var exception = new DataIntegrityViolationException("duplicate", cause);

        assertSame(exception, assertThrows(DataIntegrityViolationException.class,
                () -> new PartExceptionHandler().handleCodeConflict(exception)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"23502", "23514"})
    void propagatesOtherIntegrityFailures(String state) {
        var cause = new ConstraintViolationException("invalid", new SQLException("invalid", state), "uk_part_code");
        var exception = new DataIntegrityViolationException("invalid", cause);

        assertSame(exception, assertThrows(DataIntegrityViolationException.class,
                () -> new PartExceptionHandler().handleCodeConflict(exception)));
    }
}
