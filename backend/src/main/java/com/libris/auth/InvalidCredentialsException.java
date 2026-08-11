package com.libris.auth;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/**
 * Deliberately does not say whether it was the e-mail or the password that was wrong,
 * so the endpoint cannot be used to find out which accounts exist.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorType.UNAUTHORIZED, "INVALID_CREDENTIALS", "Correo o contraseña incorrectos");
    }
}
