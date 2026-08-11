package com.libris.shared.web;

import java.time.Instant;
import java.util.Map;

/**
 * Single error shape returned by the whole API, so a client never has to guess how a
 * failure is rendered. {@code fieldErrors} is only present on validation failures.
 */
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
