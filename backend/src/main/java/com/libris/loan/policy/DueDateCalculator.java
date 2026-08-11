package com.libris.loan.policy;

import com.libris.loan.LoanProperties;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Works out when a loan is due. Trivial today, but isolated so that the day the library
 * wants to skip weekends or honour holidays, exactly one class changes.
 */
@Component
public class DueDateCalculator {

    private final LoanProperties properties;

    public DueDateCalculator(LoanProperties properties) {
        this.properties = properties;
    }

    public LocalDate dueDateFor(LocalDate loanDate) {
        return loanDate.plusDays(properties.loanDays());
    }
}
