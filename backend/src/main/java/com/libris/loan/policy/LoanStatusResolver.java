package com.libris.loan.policy;

import com.libris.loan.Loan;
import com.libris.loan.LoanProperties;
import com.libris.loan.LoanStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Turns a loan and a date into the badge the interface shows. Single owner of that
 * mapping, so the API, the dashboard chart and the statistics can never disagree.
 */
@Component
public class LoanStatusResolver {

    private final LoanProperties properties;

    public LoanStatusResolver(LoanProperties properties) {
        this.properties = properties;
    }

    public LoanStatus resolve(Loan loan, LocalDate today) {
        if (loan.isReturned()) {
            return LoanStatus.DEVUELTO;
        }
        if (loan.isOverdueOn(today)) {
            return LoanStatus.VENCIDO;
        }
        return loan.daysUntilDue(today) <= properties.dueSoonDays() ? LoanStatus.POR_VENCER : LoanStatus.ACTIVO;
    }
}
