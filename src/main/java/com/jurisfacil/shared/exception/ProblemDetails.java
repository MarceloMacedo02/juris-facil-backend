package com.jurisfacil.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ProblemDetails(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        List<InvalidParam> invalidParams,
        OffsetDateTime timestamp) {

    public ProblemDetails {
        invalidParams = invalidParams == null ? List.of() : List.copyOf(invalidParams);
    }
}
