package com.libris.book;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/** Used on path variables, where bean validation on a request body does not apply. */
public class InvalidIsbnException extends BusinessException {

    public InvalidIsbnException(String isbn) {
        super(ErrorType.VALIDATION, "INVALID_ISBN", "El ISBN \"%s\" no es válido".formatted(isbn));
    }
}
