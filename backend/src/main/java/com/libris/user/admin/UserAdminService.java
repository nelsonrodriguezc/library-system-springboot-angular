package com.libris.user.admin;

import com.libris.loan.LoanProperties;
import com.libris.loan.LoanRepository;
import com.libris.loan.projection.EmailCount;
import com.libris.shared.exception.NotFoundException;
import com.libris.shared.web.PageResponse;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import com.libris.user.admin.dto.UserSummaryResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private final AppUserRepository users;
    private final LoanRepository loans;
    private final LoanProperties loanProperties;
    private final Clock clock;

    public UserAdminService(AppUserRepository users,
                            LoanRepository loans,
                            LoanProperties loanProperties,
                            Clock clock) {
        this.users = users;
        this.loans = loans;
        this.loanProperties = loanProperties;
        this.clock = clock;
    }

    /**
     * Loan counts for the whole page are fetched in two grouped queries rather than two
     * per row, which is what keeps this listing from degrading as the library grows.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> search(String search, Boolean blocked, Pageable pageable) {
        Instant now = clock.instant();
        Page<AppUser> page = users.findAll(UserSpecifications.matching(search, blocked, now), pageable);

        List<String> emails = page.getContent().stream()
                .map(user -> user.getEmail().toLowerCase(Locale.ROOT))
                .toList();
        if (emails.isEmpty()) {
            return PageResponse.from(page, user -> toSummary(user, 0, 0, now));
        }

        Map<String, Long> active = toMap(loans.countActiveLoansByEmails(emails));
        Map<String, Long> late = toMap(loans.countLateReturnsByEmails(
                emails, LocalDate.now(clock).minusDays(loanProperties.overdueWindowDays())));

        return PageResponse.from(page, user -> {
            String key = user.getEmail().toLowerCase(Locale.ROOT);
            return toSummary(user, active.getOrDefault(key, 0L), late.getOrDefault(key, 0L), now);
        });
    }

    /** Lets an administrator restore borrowing rights before the block lapses on its own. */
    @Transactional
    public UserSummaryResponse unblock(Long userId) {
        AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("Cuenta", userId));
        user.liftBlock();
        log.info("Block lifted for {}", user.getEmail());

        Instant now = clock.instant();
        String key = user.getEmail().toLowerCase(Locale.ROOT);
        long active = toMap(loans.countActiveLoansByEmails(List.of(key))).getOrDefault(key, 0L);
        long late = toMap(loans.countLateReturnsByEmails(
                List.of(key), LocalDate.now(clock).minusDays(loanProperties.overdueWindowDays())))
                .getOrDefault(key, 0L);

        return toSummary(user, active, late, now);
    }

    private UserSummaryResponse toSummary(AppUser user, long activeLoans, long lateReturns, Instant now) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                activeLoans,
                lateReturns,
                UserAccountStatus.of(user.getBlockedUntil(), lateReturns, now, loanProperties),
                user.getBlockedUntil(),
                user.getCreatedAt());
    }

    private Map<String, Long> toMap(List<EmailCount> rows) {
        return rows.stream().collect(Collectors.toMap(EmailCount::getEmail, EmailCount::getTotal));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
