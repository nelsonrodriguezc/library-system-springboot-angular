package com.libris.loan;

import com.libris.loan.projection.EmailCount;
import com.libris.loan.projection.MonthlyLoanCount;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findByBorrowerEmailIgnoreCaseOrderByLoanDateDescIdDesc(String borrowerEmail, Pageable pageable);

    long countByBorrowerEmailIgnoreCaseAndReturnDateIsNull(String borrowerEmail);

    Optional<Loan> findFirstByBookIdAndReturnDateIsNull(Long bookId);

    boolean existsByBookIdAndReturnDateIsNull(Long bookId);

    /**
     * Late returns inside the rolling window. This is the count that drives the block:
     * a strike is only recorded when a book actually comes back after its due date.
     */
    @Query("""
            select count(l) from Loan l
            where lower(l.borrowerEmail) = lower(:email)
              and l.returnDate is not null
              and l.returnDate > l.dueDate
              and l.returnDate >= :since
            """)
    long countLateReturnsSince(@Param("email") String email, @Param("since") LocalDate since);

    List<Loan> findByBorrowerEmailIgnoreCaseAndReturnDateIsNull(String borrowerEmail);

    /**
     * Loans about to fall due that have not been reminded yet. The null check on
     * {@code reminderSentAt} is what makes the daily job idempotent: running it twice in
     * the same day cannot send the same reminder twice.
     */
    List<Loan> findByReturnDateIsNullAndReminderSentAtIsNullAndDueDateBetween(
            LocalDate from, LocalDate to);

    /** Overdue loans that have not been chased yet, for the same reason. */
    List<Loan> findByReturnDateIsNullAndOverdueNoticeSentAtIsNullAndDueDateBefore(LocalDate date);

    // --- Aggregates behind the administration dashboard ---------------------------

    long countByReturnDateIsNull();

    long countByReturnDateIsNotNull();

    long countByReturnDateIsNullAndDueDateBefore(LocalDate date);

    long countByReturnDateIsNullAndDueDateBetween(LocalDate from, LocalDate to);

    /** Open loans per account for a whole page of users, in a single query. */
    @Query("""
            select lower(l.borrowerEmail) as email, count(l) as total
            from Loan l
            where l.returnDate is null and lower(l.borrowerEmail) in :emails
            group by lower(l.borrowerEmail)
            """)
    List<EmailCount> countActiveLoansByEmails(@Param("emails") Collection<String> emails);

    /** Late returns inside the window per account, likewise batched. */
    @Query("""
            select lower(l.borrowerEmail) as email, count(l) as total
            from Loan l
            where l.returnDate is not null
              and l.returnDate > l.dueDate
              and l.returnDate >= :since
              and lower(l.borrowerEmail) in :emails
            group by lower(l.borrowerEmail)
            """)
    List<EmailCount> countLateReturnsByEmails(@Param("emails") Collection<String> emails,
                                              @Param("since") LocalDate since);

    /** Accounts with the most late returns in the window, worst first. */
    @Query("""
            select lower(l.borrowerEmail) as email, count(l) as total
            from Loan l
            where l.returnDate is not null
              and l.returnDate > l.dueDate
              and l.returnDate >= :since
            group by lower(l.borrowerEmail)
            order by count(l) desc
            """)
    List<EmailCount> findTopLateReturners(@Param("since") LocalDate since, Pageable pageable);

    @Query(value = """
            select to_char(loan_date, 'YYYY-MM') as month, count(*) as total
            from loan
            where loan_date >= :since
            group by 1
            order by 1
            """, nativeQuery = true)
    List<MonthlyLoanCount> countLoansByMonthSince(@Param("since") LocalDate since);
}
