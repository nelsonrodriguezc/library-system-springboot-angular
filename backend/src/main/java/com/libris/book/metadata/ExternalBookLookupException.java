package com.libris.book.metadata;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/**
 * Raised only by the preview endpoint, where the caller explicitly asked for external
 * data and deserves to know it could not be obtained.
 *
 * <p>Registering a book never raises this: there the lookup is an optimisation, and a
 * failure simply means the record is saved with whatever was typed by hand.
 */
public class ExternalBookLookupException extends BusinessException {

    public ExternalBookLookupException(String isbn) {
        super(ErrorType.EXTERNAL_SERVICE, "EXTERNAL_LOOKUP_FAILED",
                ("No se pudo obtener información del ISBN %s desde el catálogo externo. "
                        + "Puedes completar los datos manualmente.").formatted(isbn));
    }
}
