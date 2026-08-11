package com.libris.book;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

public class DuplicateIsbnException extends BusinessException {

    public DuplicateIsbnException(String isbn) {
        super(ErrorType.CONFLICT, "DUPLICATE_ISBN", "Ya existe un libro registrado con el ISBN " + isbn);
    }
}
