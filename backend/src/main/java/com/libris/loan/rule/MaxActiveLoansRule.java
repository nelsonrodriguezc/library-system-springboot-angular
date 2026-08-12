package com.libris.loan.rule;

import com.libris.loan.LoanProperties;
import com.libris.loan.LoanRepository;
import com.libris.loan.MaxActiveLoansException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Keeps one account from emptying the shelves. */
@Component
@Order(30)
public class MaxActiveLoansRule implements LoanRule {

    private final LoanRepository loans;
    private final LoanProperties properties;

    public MaxActiveLoansRule(LoanRepository loans, LoanProperties properties) {
        this.loans = loans;
        this.properties = properties;
    }

    @Override
    public void check(LoanRequest request) {
        long active = loans.countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(request.borrower().getEmail());
        if (active >= properties.maxActive()) {
            throw new MaxActiveLoansException(properties.maxActive());
        }
    }
}
