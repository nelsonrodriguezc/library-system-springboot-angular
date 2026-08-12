package com.libris.auth;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException(String email) {
        super(ErrorType.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Ya existe una cuenta con el correo " + email);
    }
}
