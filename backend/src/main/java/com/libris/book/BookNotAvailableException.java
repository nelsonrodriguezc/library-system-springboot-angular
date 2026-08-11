package com.libris.book;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/** The copy cannot be lent right now because it is already out or being held for someone. */
public class BookNotAvailableException extends BusinessException {

    public BookNotAvailableException(String title, BookStatus status) {
        super(ErrorType.CONFLICT, "BOOK_NOT_AVAILABLE",
                "El libro \"%s\" no está disponible (estado actual: %s)".formatted(title, status));
    }
}
