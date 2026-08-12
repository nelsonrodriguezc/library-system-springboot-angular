package com.libris.loan;

import com.libris.shared.exception.BusinessException;
import com.libris.shared.exception.ErrorType;

public class LoanAlreadyReturnedException extends BusinessException {

    public LoanAlreadyReturnedException(Long loanId) {
        super(ErrorType.CONFLICT, "LOAN_ALREADY_RETURNED", "El préstamo %d ya fue devuelto".formatted(loanId));
    }
}
