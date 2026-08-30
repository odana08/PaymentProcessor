package org.example.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> violations
) {

    public ApiErrorResponse {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public record FieldViolation(String field, String message) {
    }
}
