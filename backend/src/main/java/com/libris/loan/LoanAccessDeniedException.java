package com.libris.loan;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

/** A reader may only act on their own loans; an ADMIN may act on anyone's. */
public class LoanAccessDeniedException extends BusinessException {

    public LoanAccessDeniedException() {
        super(ErrorType.FORBIDDEN, "LOAN_ACCESS_DENIED", "Solo puedes gestionar tus propios préstamos");
    }
}
