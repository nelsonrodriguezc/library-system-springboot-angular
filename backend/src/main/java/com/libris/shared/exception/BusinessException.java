package com.libris.shared.exception;

/**
 * Base type for every rule the application enforces on purpose, as opposed to a bug.
 * Carries a stable machine-readable {@code code} so the frontend can react to a specific
 * rule without parsing prose.
 */
public abstract class BusinessException extends RuntimeException {

    private final ErrorType type;
    private final String code;

    protected BusinessException(ErrorType type, String code, String message) {
        super(message);
        this.type = type;
        this.code = code;
    }

    public ErrorType type() {
        return type;
    }

    public String code() {
        return code;
    }
}
