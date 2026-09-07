package com.jurisfacil.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractBusinessExceptionTest {

    @Test
    void exposesImmutableCodeAndDetailFromConstructor() {
        var exception = new ConcreteBusinessException("EXAMPLE_CODE", "Example detail");

        assertThat(exception.getCode()).isEqualTo("EXAMPLE_CODE");
        assertThat(exception.getMessage()).isEqualTo("Example detail");
    }

    @Test
    void preservesCauseWithTheCauseConstructor() {
        var cause = new IllegalStateException("root cause");
        var exception = new ConcreteBusinessException("EXAMPLE_CODE", "Example detail", cause);

        assertThat(exception.getCause()).isSameAs(cause);
    }

    private static final class ConcreteBusinessException extends AbstractBusinessException {

        private ConcreteBusinessException(String code, String detail) {
            super(code, detail);
        }

        private ConcreteBusinessException(String code, String detail, Throwable cause) {
            super(code, detail, cause);
        }
    }
}
