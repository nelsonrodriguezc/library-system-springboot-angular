package com.libris.book;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/**
 * Registering by ISBN alone is allowed, but only when the external catalogue can supply
 * the title and the author. When it cannot, the person has to type them.
 */
public class IncompleteBookDataException extends BusinessException {

    public IncompleteBookDataException(String isbn) {
        super(ErrorType.VALIDATION, "INCOMPLETE_BOOK_DATA",
                ("No se pudo completar la ficha del ISBN %s desde el catálogo externo. "
                        + "Ingresa al menos el título y el autor.").formatted(isbn));
    }
}
