package com.libris.user;

import com.libris.loan.Loan;
import com.libris.loan.LoanProperties;
import com.libris.loan.LoanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Decides whether a late return costs the account its borrowing rights.
 *
 * <p>A strike is only recorded when a copy actually comes back after its due date, and
 * only strikes inside the rolling window count, so an old lapse eventually stops
 * weighing. This class decides; publishing the notification is the caller's job.
 */
@Component
public class OverdueBlockPolicy {

    private static final Logger log = LoggerFactory.getLogger(OverdueBlockPolicy.class);

    private final LoanRepository loans;
    private final LoanProperties properties;
    private final Clock clock;

    public OverdueBlockPolicy(LoanRepository loans, LoanProperties properties, Clock clock) {
        this.loans = loans;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @return true when this particular return is what tipped the account into a block,
     *         so the caller knows a notification is owed.
     */
    public boolean applyAfterReturn(AppUser borrower, Loan returnedLoan) {
        if (!returnedLoan.wasReturnedLate()) {
            return false;
        }
        // Already serving a block: the strike still counts, but nothing new happens and
        // the reader is not told twice.
        if (borrower.isBlockedAt(clock.instant())) {
            return false;
        }

        LocalDate windowStart = LocalDate.now(clock).minusDays(properties.overdueWindowDays());
        long strikes = loans.countLateReturnsSince(borrower.getEmail(), windowStart);
        if (strikes < properties.overdueLimit()) {
            log.debug("{} has {} late returns in the last {} days", borrower.getEmail(), strikes,
                    properties.overdueWindowDays());
            return false;
        }

        borrower.blockUntil(clock.instant().plus(properties.blockDays(), ChronoUnit.DAYS));
        log.info("Blocked {} until {} after {} late returns", borrower.getEmail(), borrower.getBlockedUntil(), strikes);
        return true;
    }

    public long lateReturnsInWindow(String email) {
        return loans.countLateReturnsSince(email, LocalDate.now(clock).minusDays(properties.overdueWindowDays()));
    }
}
