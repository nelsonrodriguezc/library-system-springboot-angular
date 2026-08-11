package com.libris.loan;

import java.time.LocalDate;
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
}
