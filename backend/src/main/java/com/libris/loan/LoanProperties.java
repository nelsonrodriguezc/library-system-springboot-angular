package com.libris.loan;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Every number the loan rules depend on, in one place and configurable.
 *
 * @param loanDays          length of a loan; the due date is the loan date plus this
 * @param maxActive         how many copies one account may hold at once
 * @param dueSoonDays       window used by the interface to badge a loan as "por vencer"
 * @param reminderDaysBefore how early the scheduled job sends the reminder e-mail
 * @param overdueLimit      late returns needed to block an account
 * @param overdueWindowDays rolling window those late returns are counted over
 * @param blockDays         how long a block lasts before it lapses on its own
 */
@ConfigurationProperties(prefix = "libris.loans")
public record LoanProperties(
        int loanDays,
        int maxActive,
        int dueSoonDays,
        int reminderDaysBefore,
        int overdueLimit,
        int overdueWindowDays,
        int blockDays) {

    public LoanProperties {
        loanDays = positiveOr(loanDays, 14);
        maxActive = positiveOr(maxActive, 3);
        dueSoonDays = positiveOr(dueSoonDays, 3);
        reminderDaysBefore = positiveOr(reminderDaysBefore, 2);
        overdueLimit = positiveOr(overdueLimit, 3);
        overdueWindowDays = positiveOr(overdueWindowDays, 90);
        blockDays = positiveOr(blockDays, 7);
    }

    private static int positiveOr(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
