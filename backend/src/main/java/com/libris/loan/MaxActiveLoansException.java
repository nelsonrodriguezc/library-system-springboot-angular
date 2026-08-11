package com.libris.loan;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/**
 * Cap on simultaneous loans. Not in the original statement: it comes from the interface,
 * which promises "puedes tener hasta 3 préstamos activos", and is configurable through
 * {@code libris.loans.max-active}.
 */
public class MaxActiveLoansException extends BusinessException {

    public MaxActiveLoansException(int maxActive) {
        super(ErrorType.CONFLICT, "MAX_ACTIVE_LOANS",
                "Ya tienes %d préstamos activos, el máximo permitido. Devuelve uno para pedir otro."
                        .formatted(maxActive));
    }
}
