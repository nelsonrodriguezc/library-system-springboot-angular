package com.libris.stats;

import com.libris.book.BookRepository;
import com.libris.book.BookStatus;
import com.libris.loan.LoanProperties;
import com.libris.loan.LoanRepository;
import com.libris.loan.projection.EmailCount;
import com.libris.stats.dto.AdminStatsResponse;
import com.libris.stats.dto.AdminStatsResponse.CatalogueTotals;
import com.libris.stats.dto.AdminStatsResponse.LoansByStatus;
import com.libris.stats.dto.AdminStatsResponse.MonthlyLoans;
import com.libris.stats.dto.AdminStatsResponse.OverdueUser;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import com.libris.user.admin.UserAccountStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private static final int MONTHS_IN_CHART = 6;
    private static final int TOP_OVERDUE_USERS = 5;
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es"));

    private final BookRepository books;
    private final LoanRepository loans;
    private final AppUserRepository users;
    private final LoanProperties loanProperties;
    private final Clock clock;

    public StatsService(BookRepository books,
                        LoanRepository loans,
                        AppUserRepository users,
                        LoanProperties loanProperties,
                        Clock clock) {
        this.books = books;
        this.loans = loans;
        this.users = users;
        this.loanProperties = loanProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse overview() {
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();

        return new AdminStatsResponse(
                catalogueTotals(),
                loansByStatus(today),
                users.countByBlockedUntilAfter(now),
                loansPerMonth(today),
                topOverdueUsers(today, now));
    }

    private CatalogueTotals catalogueTotals() {
        long available = books.countByStatus(BookStatus.DISPONIBLE);
        long borrowed = books.countByStatus(BookStatus.PRESTADO);
        long reserved = books.countByStatus(BookStatus.RESERVADO);
        return new CatalogueTotals(available + borrowed + reserved, available, borrowed, reserved);
    }

    /**
     * The buckets are carved out of each other so nothing is counted twice: a loan that is
     * open and due within the window is "por vencer" and not also "activo".
     */
    private LoansByStatus loansByStatus(LocalDate today) {
        long open = loans.countByReturnDateIsNull();
        long overdue = loans.countByReturnDateIsNullAndDueDateBefore(today);
        long dueSoon = loans.countByReturnDateIsNullAndDueDateBetween(
                today, today.plusDays(loanProperties.dueSoonDays()));
        long returned = loans.countByReturnDateIsNotNull();
        return new LoansByStatus(open - overdue - dueSoon, dueSoon, overdue, returned);
    }

    private List<MonthlyLoans> loansPerMonth(LocalDate today) {
        YearMonth firstMonth = YearMonth.from(today).minusMonths(MONTHS_IN_CHART - 1L);
        Map<String, Long> totals = loans.countLoansByMonthSince(firstMonth.atDay(1)).stream()
                .collect(Collectors.toMap(row -> row.getMonth(), row -> row.getTotal()));

        // Months with no activity still need a point, otherwise the chart lies about the gap.
        return java.util.stream.IntStream.range(0, MONTHS_IN_CHART)
                .mapToObj(offset -> firstMonth.plusMonths(offset))
                .map(month -> new MonthlyLoans(
                        month.toString(),
                        capitalise(MONTH_LABEL.format(month)),
                        totals.getOrDefault(month.toString(), 0L)))
                .toList();
    }

    private List<OverdueUser> topOverdueUsers(LocalDate today, Instant now) {
        LocalDate windowStart = today.minusDays(loanProperties.overdueWindowDays());
        List<EmailCount> worst = loans.findTopLateReturners(windowStart, PageRequest.of(0, TOP_OVERDUE_USERS));
        if (worst.isEmpty()) {
            return List.of();
        }

        Map<String, AppUser> accounts = users
                .findByEmailIgnoreCaseIn(worst.stream().map(EmailCount::getEmail).toList()).stream()
                .collect(Collectors.toMap(user -> user.getEmail().toLowerCase(Locale.ROOT), Function.identity()));

        return worst.stream()
                .map(row -> {
                    AppUser account = accounts.get(row.getEmail());
                    if (account == null) {
                        return null;
                    }
                    return new OverdueUser(
                            account.getId(),
                            account.getName(),
                            account.getEmail(),
                            row.getTotal(),
                            UserAccountStatus.of(account.getBlockedUntil(), row.getTotal(), now, loanProperties),
                            account.getBlockedUntil());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String capitalise(String label) {
        String trimmed = label.replace(".", "");
        return trimmed.substring(0, 1).toUpperCase(Locale.forLanguageTag("es")) + trimmed.substring(1);
    }
}
