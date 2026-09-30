package com.possystem.pos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Single error shape for every failing request, so the client has one thing to parse.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        /* Field name to message, populated only for validation failures. */
        Map<String, String> fieldErrors,
        List<String> details
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, Map.of(), List.of());
    }

    public static ApiError validation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, "Validation Failed", message, path, fieldErrors, List.of());
    }
}
