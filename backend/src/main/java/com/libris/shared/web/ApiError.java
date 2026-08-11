package com.libris.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Single error shape returned by the whole API, so a client never has to guess how a
 * failure is rendered. {@code fieldErrors} is the one field that is omitted rather than
 * null, because it only means anything on a validation failure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(Instant timestamp, int status, String code, String message, String path) {
        return new ApiError(timestamp, status, code, message, path, null);
    }

    public static ApiError withFieldErrors(
            Instant timestamp, int status, String code, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(timestamp, status, code, message, path, fieldErrors);
    }
}
