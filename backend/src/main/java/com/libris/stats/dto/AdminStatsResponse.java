package com.libris.stats.dto;

import com.libris.user.admin.UserAccountStatus;
import java.time.Instant;
import java.util.List;

/**
 * Everything the administration overview draws.
 *
 * <p>{@code loansByStatus} uses the four mutually exclusive buckets, so the doughnut adds
 * up to the total instead of double counting the loans that are both open and due soon.
 */
public record AdminStatsResponse(
        CatalogueTotals catalogue,
        LoansByStatus loansByStatus,
        long blockedUsers,
        List<MonthlyLoans> loansPerMonth,
        List<OverdueUser> topOverdueUsers) {

    public record CatalogueTotals(long total, long available, long borrowed, long reserved) {
    }

    public record LoansByStatus(long active, long dueSoon, long overdue, long returned) {

        public long total() {
            return active + dueSoon + overdue + returned;
        }
    }

    public record MonthlyLoans(String month, String label, long total) {
    }

    public record OverdueUser(
            Long id,
            String name,
            String email,
            long lateReturns,
            UserAccountStatus status,
            Instant blockedUntil) {
    }
}
