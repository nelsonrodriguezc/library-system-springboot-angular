package com.libris.loan;

import com.libris.auth.AuthenticatedUser;
import com.libris.book.Book;
import com.libris.book.BookRepository;
import com.libris.loan.dto.CreateLoanRequest;
import com.libris.loan.dto.LoanResponse;
import com.libris.loan.event.LoanCreatedEvent;
import com.libris.loan.policy.DueDateCalculator;
import com.libris.loan.policy.LoanStatusResolver;
import com.libris.loan.rule.LoanRequest;
import com.libris.loan.rule.LoanRule;
import com.libris.shared.exception.NotFoundException;
import com.libris.shared.web.PageResponse;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import com.libris.user.OverdueBlockPolicy;
import com.libris.user.event.AccountBlockedEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates lending and returning. It owns no rule of its own: eligibility lives in
 * the {@link LoanRule} beans, the due date in {@link DueDateCalculator} and the block in
 * {@link OverdueBlockPolicy}. What is left here is the sequence and the transaction.
 */
@Service
public class LoanService {

    private final LoanRepository loans;
    private final BookRepository books;
    private final AppUserRepository users;
    private final List<LoanRule> rules;
    private final List<LoanLifecycleListener> lifecycleListeners;
    private final DueDateCalculator dueDateCalculator;
    private final LoanStatusResolver statusResolver;
    private final OverdueBlockPolicy overdueBlockPolicy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoanService(LoanRepository loans,
                       BookRepository books,
                       AppUserRepository users,
                       List<LoanRule> rules,
                       List<LoanLifecycleListener> lifecycleListeners,
                       DueDateCalculator dueDateCalculator,
                       LoanStatusResolver statusResolver,
                       OverdueBlockPolicy overdueBlockPolicy,
                       ApplicationEventPublisher events,
                       Clock clock) {
        this.loans = loans;
        this.books = books;
        this.users = users;
        this.rules = rules;
        this.lifecycleListeners = lifecycleListeners;
        this.dueDateCalculator = dueDateCalculator;
        this.statusResolver = statusResolver;
        this.overdueBlockPolicy = overdueBlockPolicy;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public LoanResponse create(CreateLoanRequest request, AuthenticatedUser caller) {
        AppUser borrower = resolveBorrower(request.borrowerEmail(), caller);
        Book book = books.findById(request.bookId())
                .orElseThrow(() -> new NotFoundException("Libro", request.bookId()));

        LocalDate today = LocalDate.now(clock);
        LoanRequest context = new LoanRequest(book, borrower, today, clock.instant());
        rules.forEach(rule -> rule.check(context));

        Loan loan = loans.save(new Loan(
                book, borrower.getName(), borrower.getEmail(), today, dueDateCalculator.dueDateFor(today)));
        book.markLoaned();
        lifecycleListeners.forEach(listener -> listener.onLoanCreated(loan));

        // Delivered after the transaction commits, so no e-mail can ever describe a loan
        // that was rolled back. See NotificationEventListener.
        events.publishEvent(new LoanCreatedEvent(loan.getId()));

        return toResponse(loan, today);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> findMine(AuthenticatedUser caller, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        return PageResponse.from(
                loans.findByBorrowerEmailIgnoreCaseOrderByLoanDateDescIdDesc(caller.email(), pageable),
                loan -> toResponse(loan, today));
    }

    /**
     * Brings a copy back. Late returns count towards the block, and the account is only
     * notified on the return that actually triggers it.
     */
    @Transactional
    public LoanResponse returnLoan(Long loanId, AuthenticatedUser caller) {
        Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Préstamo", loanId));
        if (!caller.isAdmin() && !loan.getBorrowerEmail().equalsIgnoreCase(caller.email())) {
            throw new LoanAccessDeniedException();
        }
        if (loan.isReturned()) {
            throw new LoanAlreadyReturnedException(loanId);
        }

        LocalDate today = LocalDate.now(clock);
        loan.markReturned(today);
        // Default outcome is back on the shelf; a listener may hold it for the waiting list.
        loan.getBook().markAvailable();
        lifecycleListeners.forEach(listener -> listener.onLoanReturned(loan));

        users.findByEmailIgnoreCase(loan.getBorrowerEmail()).ifPresent(borrower -> {
            if (overdueBlockPolicy.applyAfterReturn(borrower, loan)) {
                events.publishEvent(new AccountBlockedEvent(borrower.getId()));
            }
        });

        return toResponse(loan, today);
    }

    private AppUser resolveBorrower(String requestedEmail, AuthenticatedUser caller) {
        String email = requestedEmail == null || requestedEmail.isBlank()
                ? caller.email()
                : requestedEmail.trim().toLowerCase(Locale.ROOT);

        if (!caller.isAdmin() && !email.equalsIgnoreCase(caller.email())) {
            throw new LoanAccessDeniedException();
        }
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Cuenta", email));
    }

    private LoanResponse toResponse(Loan loan, LocalDate today) {
        return LoanResponse.from(loan, statusResolver.resolve(loan, today), today);
    }
}
