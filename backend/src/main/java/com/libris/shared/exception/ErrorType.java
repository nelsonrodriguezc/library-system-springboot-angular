package com.libris.shared.exception;

/**
 * Domain-level classification of a failure. The HTTP mapping lives in the web layer, so
 * business code never has to import a servlet or Spring MVC type to describe what went
 * wrong. Adding a new exception means picking one of these — no handler edit required.
 */
public enum ErrorType {

    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    UNAUTHORIZED,
    FORBIDDEN,
    EXTERNAL_SERVICE
}
