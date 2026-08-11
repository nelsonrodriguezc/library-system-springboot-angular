package com.libris.shared.exception;

/** A resource addressed by the request does not exist. */
public class NotFoundException extends BusinessException {

    public NotFoundException(String resource, Object identifier) {
        super(ErrorType.NOT_FOUND, "RESOURCE_NOT_FOUND", "%s no encontrado: %s".formatted(resource, identifier));
    }
}
