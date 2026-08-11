package com.libris.book;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/**
 * Removing a copy that is out on loan or being held would silently break the loan or the
 * waiting list attached to it, so the catalogue refuses.
 */
public class BookNotDeletableException extends BusinessException {

    public BookNotDeletableException(String title, BookStatus status) {
        super(ErrorType.CONFLICT, "BOOK_NOT_DELETABLE",
                "Solo se pueden eliminar libros disponibles. \"%s\" está en estado %s".formatted(title, status));
    }
}
